package com.aliya.workly.security;

import com.aliya.workly.company.Company;
import com.aliya.workly.company.CompanyRepository;
import com.aliya.workly.review.Review;
import com.aliya.workly.review.ReviewRepository;
import com.aliya.workly.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// One protected endpoint per module, hit as each role, through the real filter chain and a real
// Postgres (same DATABASE_URL requirement as JobVisibilityIntegrationTest). Callers are injected
// with @WithAuthPrincipal, so this checks the authorization rules, not JWT parsing — that is
// AuthFlowIntegrationTest's job.
//
// @Transactional rolls every row back after each test. MockMvc runs on the test thread, so the
// controller's own transactions join it.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class RoleAccessIntegrationTest {

    private static final long OWNER_ID = 900_001L;
    private static final long OTHER_ID = 900_002L;
    private static final long SPOOFED_ID = 42L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private ReviewRepository reviewRepository;

    private Company company;
    private Review ownersReview;

    @BeforeEach
    void seed() {
        company = new Company();
        company.setCompanyName("Role Access Test Co");
        company.setLocation("Remote");
        company = companyRepository.save(company);

        ownersReview = new Review();
        ownersReview.setTitle("Original title");
        ownersReview.setRating(3.0);
        ownersReview.setCompany(company);
        ownersReview.setReviewedBy(OWNER_ID);
        ownersReview = reviewRepository.save(ownersReview);
    }

    // ---- POST /jobs: COMPANY and ADMIN only ----

    @Test
    void createJob_anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(postJob()).andExpect(status().isUnauthorized());
    }

    @Test
    @WithAuthPrincipal(role = Role.APPLICANT)
    void createJob_applicant_isForbidden() throws Exception {
        mockMvc.perform(postJob()).andExpect(status().isForbidden());
    }

    @Test
    @WithAuthPrincipal(id = OWNER_ID, role = Role.COMPANY)
    void createJob_company_isCreated_andPostedByIsTheCallerNotTheBody() throws Exception {
        mockMvc.perform(postJob())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postedBy").value(OWNER_ID));
    }

    @Test
    @WithAuthPrincipal(role = Role.ADMIN)
    void createJob_admin_isCreated() throws Exception {
        mockMvc.perform(postJob()).andExpect(status().isCreated());
    }

    // ---- POST /companies: ADMIN only ----

    @Test
    void createCompany_anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(postCompany()).andExpect(status().isUnauthorized());
    }

    @Test
    @WithAuthPrincipal(role = Role.APPLICANT)
    void createCompany_applicant_isForbidden() throws Exception {
        mockMvc.perform(postCompany()).andExpect(status().isForbidden());
    }

    @Test
    @WithAuthPrincipal(role = Role.COMPANY)
    void createCompany_company_isForbidden() throws Exception {
        mockMvc.perform(postCompany()).andExpect(status().isForbidden());
    }

    @Test
    @WithAuthPrincipal(role = Role.ADMIN)
    void createCompany_admin_isCreated() throws Exception {
        mockMvc.perform(postCompany()).andExpect(status().isCreated());
    }

    // ---- reviews: any signed-in user writes, own-review-or-ADMIN edits ----

    @Test
    void createReview_anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(postReview()).andExpect(status().isUnauthorized());
    }

    @Test
    @WithAuthPrincipal(id = OTHER_ID, role = Role.APPLICANT)
    void createReview_applicant_isCreated_andReviewedByIsTheCallerNotTheBody() throws Exception {
        mockMvc.perform(postReview())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewedBy").value(OTHER_ID));
    }

    @Test
    @WithAuthPrincipal(id = OWNER_ID, role = Role.APPLICANT)
    void updateReview_owner_isOk() throws Exception {
        mockMvc.perform(putReview("Edited by owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Edited by owner"));
    }

    @Test
    @WithAuthPrincipal(id = OTHER_ID, role = Role.APPLICANT)
    void updateReview_someoneElse_isForbidden_andTheReviewIsUntouched() throws Exception {
        mockMvc.perform(putReview("Edited by a stranger")).andExpect(status().isForbidden());

        assertThat(reviewRepository.findById(ownersReview.getId()).orElseThrow().getTitle())
                .isEqualTo("Original title");
    }

    @Test
    @WithAuthPrincipal(id = OTHER_ID, role = Role.ADMIN)
    void updateReview_adminWhoIsNotTheOwner_isOk() throws Exception {
        mockMvc.perform(putReview("Edited by admin")).andExpect(status().isOk());
    }

    @Test
    @WithAuthPrincipal(id = OTHER_ID, role = Role.APPLICANT)
    void deleteReview_someoneElse_isForbidden_andTheReviewSurvives() throws Exception {
        mockMvc.perform(delete(reviewUrl(ownersReview.getId()))).andExpect(status().isForbidden());

        assertThat(reviewRepository.existsById(ownersReview.getId())).isTrue();
    }

    @Test
    @WithAuthPrincipal(id = OWNER_ID, role = Role.APPLICANT)
    void deleteReview_owner_isNoContent() throws Exception {
        mockMvc.perform(delete(reviewUrl(ownersReview.getId()))).andExpect(status().isNoContent());

        assertThat(reviewRepository.existsById(ownersReview.getId())).isFalse();
    }

    // ---- request builders ----

    // postedBy in the body is a spoof attempt; the controller must ignore it
    private MockHttpServletRequestBuilder postJob() {
        return post("/jobs").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"Backend Engineer","companyId":%d,"postedBy":%d}
                """.formatted(company.getId(), SPOOFED_ID));
    }

    private MockHttpServletRequestBuilder postCompany() {
        return post("/companies").contentType(MediaType.APPLICATION_JSON).content("""
                {"companyName":"Created In Test","location":"Remote"}
                """);
    }

    private MockHttpServletRequestBuilder postReview() {
        return post("/company/" + company.getId() + "/reviews").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"Great place","rating":4.5,"reviewedBy":%d}
                """.formatted(SPOOFED_ID));
    }

    private MockHttpServletRequestBuilder putReview(String title) {
        return put(reviewUrl(ownersReview.getId())).contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"%s","rating":2.0}
                """.formatted(title));
    }

    private String reviewUrl(Long reviewId) {
        return "/company/" + company.getId() + "/reviews/" + reviewId;
    }
}
