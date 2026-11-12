package com.flowedu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Small helpers shared by the MockMvc integration tests. */
final class ApiTestSupport {
    static final ObjectMapper JSON = new ObjectMapper();

    private ApiTestSupport() {}

    /** Signs in with one of the seeded demo accounts and returns the Authorization header value. */
    static String bearer(MockMvc mvc, String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("email", email, "password", password))))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JSON.readTree(body).get("token").asText();
    }

    static String admin(MockMvc mvc) throws Exception { return bearer(mvc, "admin@flowedu.local", "admin123"); }
    static String teacher(MockMvc mvc) throws Exception { return bearer(mvc, "teacher@flowedu.local", "teacher123"); }
    static String student(MockMvc mvc) throws Exception { return bearer(mvc, "student@flowedu.local", "student123"); }

    static JsonNode read(String json) throws Exception { return JSON.readTree(json); }
}
