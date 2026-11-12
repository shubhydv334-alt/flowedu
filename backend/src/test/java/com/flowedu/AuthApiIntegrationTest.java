package com.flowedu;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private String tokenFor(String email, String password) throws Exception {
        String body = login(email, password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void validCredentialsReturnATokenAndRole() throws Exception {
        login("admin@flowedu.local", "admin123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        login("teacher@flowedu.local", "not-the-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void protectedEndpointsRequireAToken() throws Exception {
        mvc.perform(get("/api/workflows")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenGrantsAccessToTeacherEndpoints() throws Exception {
        String token = tokenFor("teacher@flowedu.local", "teacher123");
        mvc.perform(get("/api/workflows").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void studentsCannotUseTeacherEndpoints() throws Exception {
        String token = tokenFor("student@flowedu.local", "student123");
        mvc.perform(get("/api/workflows").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void teachersCannotManageUsers() throws Exception {
        String token = tokenFor("teacher@flowedu.local", "teacher123");
        mvc.perform(get("/api/auth/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void forgedTokenIsRejected() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedFailuresAreThrottled() throws Exception {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) {
            login("nobody@flowedu.local", "wrong-" + i).andExpect(status().isUnauthorized());
        }
        login("nobody@flowedu.local", "wrong-again").andExpect(status().isTooManyRequests());
    }
}
