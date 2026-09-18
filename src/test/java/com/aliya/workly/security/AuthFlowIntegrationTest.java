package com.aliya.workly.security;

import com.aliya.workly.company.Company;
import com.aliya.workly.company.CompanyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Nothing injected here: every request carries a real token from /auth/register or /auth/login,
// so JwtAuthenticationFilter, the token claims and the real Postgres are all in the path.
//
// Deliberately not @Transactional. A test-wide transaction would hide the one thing the replay
// test exists to prove — that revoking a token family survives the BadCredentialsException that
// follows it (RefreshTokenService.rotate's noRollbackFor). Rows are deleted explicitly instead.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    private static final String TEST_EMAIL_DOMAIN = "@authflow.test";
    private static final String TEST_COMPANY_NAME = "Auth Flow Test Co";
    private static final String PASSWORD = "throwaway-pass-123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private CompanyRepository companyRepository;

    private Company company;

    @BeforeEach
    void seedCompany() {
        company = new Company();
        company.setCompanyName(TEST_COMPANY_NAME);
        company.setLocation("Remote");
        company = companyRepository.save(company);
    }

    @AfterEach
    void cleanUp() {
        String users = "select id from app_user where email like '%" + TEST_EMAIL_DOMAIN + "'";
        jdbc.update("delete from job where posted_by in (" + users + ")");
        jdbc.update("delete from refresh_token where user_id in (" + users + ")");
        jdbc.update("delete from app_user where email like '%" + TEST_EMAIL_DOMAIN + "'");
        jdbc.update("delete from company where company_name = ?", TEST_COMPANY_NAME);
    }

    @Test
    void registerThenUseAccessToken_reachesAProtectedEndpoint() throws Exception {
        String email = newEmail();
        JsonNode tokens = register(email, "COMPANY");

        mockMvc.perform(get("/auth/me").header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("COMPANY"));
    }

    @Test
    void companyToken_canPostJob_andPostedByIsTheRealUserId() throws Exception {
        JsonNode tokens = register(newEmail(), "COMPANY");
        long userId = userIdOf(tokens);

        mockMvc.perform(post("/jobs")
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Backend Engineer","companyId":%d,"postedBy":42}
                                """.formatted(company.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postedBy").value(userId));
    }

    @Test
    void applicantToken_cannotPostJob() throws Exception {
        JsonNode tokens = register(newEmail(), "APPLICANT");

        mockMvc.perform(post("/jobs")
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Backend Engineer","companyId":%d}
                                """.formatted(company.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_asAdmin_isRejected() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(newEmail(), "ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_sameEmailTwice_isConflict() throws Exception {
        String email = newEmail();
        register(email, "APPLICANT");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "APPLICANT")))
                .andExpect(status().isConflict());
    }

    @Test
    void login_withCorrectPassword_returnsATokenThatWorks() throws Exception {
        String email = newEmail();
        register(email, "APPLICANT");

        JsonNode tokens = bodyOf(login(email, PASSWORD).andExpect(status().isOk()));

        mockMvc.perform(get("/auth/me").header("Authorization", bearer(tokens)))
                .andExpect(status().isOk());
    }

    @Test
    void login_withWrongPassword_isUnauthorized() throws Exception {
        String email = newEmail();
        register(email, "APPLICANT");

        login(email, "not-the-password").andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withGarbageToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer not.a.real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_cannotBeUsedAsAnAccessToken() throws Exception {
        JsonNode tokens = register(newEmail(), "APPLICANT");

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + tokens.get("refreshToken").asText()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_returnsANewWorkingPair() throws Exception {
        JsonNode first = register(newEmail(), "APPLICANT");

        JsonNode second = bodyOf(refresh(first).andExpect(status().isOk()));

        assertThat(second.get("refreshToken").asText()).isNotEqualTo(first.get("refreshToken").asText());
        mockMvc.perform(get("/auth/me").header("Authorization", bearer(second)))
                .andExpect(status().isOk());
    }

    @Test
    void refresh_replayingAnAlreadyUsedToken_revokesTheWholeFamily() throws Exception {
        JsonNode first = register(newEmail(), "APPLICANT");
        JsonNode second = bodyOf(refresh(first).andExpect(status().isOk()));

        // the first token was already spent, so presenting it again is treated as theft
        refresh(first).andExpect(status().isUnauthorized());

        // ...which must also have killed the replacement handed out to the legitimate client
        refresh(second).andExpect(status().isUnauthorized());
    }

    @Test
    void logout_revokesTheRefreshToken() throws Exception {
        JsonNode tokens = register(newEmail(), "APPLICANT");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.get("refreshToken").asText() + "\"}"))
                .andExpect(status().isNoContent());

        refresh(tokens).andExpect(status().isUnauthorized());
    }

    // ---- helpers ----

    private String newEmail() {
        return UUID.randomUUID() + TEST_EMAIL_DOMAIN;
    }

    private String registerBody(String email, String role) {
        return """
                {"email":"%s","password":"%s","fullName":"Auth Flow","role":"%s"}
                """.formatted(email, PASSWORD, role);
    }

    private JsonNode register(String email, String role) throws Exception {
        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, role)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return readJson(body);
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private ResultActions refresh(JsonNode tokens) throws Exception {
        return mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + tokens.get("refreshToken").asText() + "\"}"));
    }

    private long userIdOf(JsonNode tokens) throws Exception {
        String body = mockMvc.perform(get("/auth/me").header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return readJson(body).get("id").asLong();
    }

    private String bearer(JsonNode tokens) {
        return "Bearer " + tokens.get("accessToken").asText();
    }

    private JsonNode bodyOf(ResultActions result) throws Exception {
        return readJson(result.andReturn().getResponse().getContentAsString());
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("Response was not JSON: " + json, e);
        }
    }
}
