package com.flowedu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Every failure should reach the client as {"error": "..."} with the right status, never as a 500 stack trace. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiErrorsIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void malformedJsonIsABadRequest() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void wrongHttpMethodIsMethodNotAllowed() throws Exception {
        mvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void missingUploadPartIsABadRequest() throws Exception {
        mvc.perform(post("/api/import/students").contentType(MediaType.MULTIPART_FORM_DATA)
                        .header("Authorization", ApiTestSupport.teacher(mvc)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void unknownIdsAreNotFoundWithoutDetails() throws Exception {
        mvc.perform(get("/api/workflows/987654").header("Authorization", ApiTestSupport.teacher(mvc)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not found"));
    }

    @Test
    void invalidPathVariablesAreABadRequest() throws Exception {
        mvc.perform(get("/api/workflows/not-a-number").header("Authorization", ApiTestSupport.teacher(mvc)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
