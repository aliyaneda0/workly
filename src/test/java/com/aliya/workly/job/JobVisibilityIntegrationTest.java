package com.aliya.workly.job;

import com.aliya.workly.company.Company;
import com.aliya.workly.company.CompanyRepository;
import com.aliya.workly.security.AuthPrincipal;
import com.aliya.workly.user.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Real Postgres required (same as WorklyApplicationTests.contextLoads) — needs DATABASE_URL
// pointed at a running instance, e.g. the docker-compose Postgres via .env's localhost URL.
// Mockito can't verify this: the DRAFT-hiding logic lives inside a JPA Specification predicate,
// which only runs against a real query engine, not a mocked repository.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class JobVisibilityIntegrationTest {

    private static final String DRAFT_TITLE = "Draft Job Should Stay Hidden";
    private static final String OPEN_TITLE = "Open Job Should Be Visible";
    private static final Long OWNER_ID = 999_001L;
    private static final Long OTHER_USER_ID = 999_002L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private Company company;
    private Job openJob;
    private Job draftJob;

    @BeforeEach
    void seedJobs() {
        company = new Company();
        company.setCompanyName("Visibility Test Co");
        company.setLocation("Remote");
        company = companyRepository.save(company);

        openJob = new Job();
        openJob.setTitle(OPEN_TITLE);
        openJob.setStatus(JobStatus.OPEN);
        openJob.setCompany(company);
        openJob.setPostedBy(OWNER_ID);
        openJob = jobRepository.save(openJob);

        draftJob = new Job();
        draftJob.setTitle(DRAFT_TITLE);
        draftJob.setStatus(JobStatus.DRAFT);
        draftJob.setCompany(company);
        draftJob.setPostedBy(OWNER_ID);
        draftJob = jobRepository.save(draftJob);
    }

    @AfterEach
    void cleanUp() {
        jobRepository.delete(openJob);
        jobRepository.delete(draftJob);
        companyRepository.delete(company);
    }

    @Test
    void anonymousCaller_doesNotSeeDraftJob() throws Exception {
        mockMvc.perform(get("/jobs").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", not(hasItem(DRAFT_TITLE))))
                .andExpect(jsonPath("$.content[*].title", hasItem(OPEN_TITLE)));
    }

    @Test
    void nonOwnerCaller_doesNotSeeSomeoneElsesDraftJob() throws Exception {
        mockMvc.perform(get("/jobs").param("size", "100").with(authentication(companyUser(OTHER_USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", not(hasItem(DRAFT_TITLE))));
    }

    @Test
    void ownerCaller_seesOwnDraftJob() throws Exception {
        mockMvc.perform(get("/jobs").param("size", "100").with(authentication(companyUser(OWNER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", hasItem(DRAFT_TITLE)));
    }

    @Test
    void adminCaller_seesEveryonesDraftJobs() throws Exception {
        AuthPrincipal admin = new AuthPrincipal(999_003L, "admin@test", Role.ADMIN);
        var auth = new UsernamePasswordAuthenticationToken(admin, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        mockMvc.perform(get("/jobs").param("size", "100").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", hasItem(DRAFT_TITLE)));
    }

    private UsernamePasswordAuthenticationToken companyUser(Long userId) {
        AuthPrincipal principal = new AuthPrincipal(userId, "user" + userId + "@test", Role.COMPANY);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_COMPANY")));
    }
}
