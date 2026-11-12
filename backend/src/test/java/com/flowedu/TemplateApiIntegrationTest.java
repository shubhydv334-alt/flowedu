package com.flowedu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TemplateApiIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void listsTheBuiltInTemplates() throws Exception {
        mvc.perform(get("/api/templates").header("Authorization", ApiTestSupport.teacher(mvc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].key", hasItems("low-attendance", "assignment-reminder", "exam-countdown", "weekly-teacher-summary")));
    }

    @Test
    void usingATemplateCreatesADisabledWorkflow() throws Exception {
        mvc.perform(post("/api/templates/exam-countdown/use").header("Authorization", ApiTestSupport.teacher(mvc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Exam Countdown"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.trigger.type").value("DATE"));
    }

    @Test
    void unknownTemplatesAreNotFound() throws Exception {
        mvc.perform(post("/api/templates/nope/use").header("Authorization", ApiTestSupport.teacher(mvc)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not found"));
    }

    @Test
    void studentsCannotInstantiateTemplates() throws Exception {
        mvc.perform(post("/api/templates/exam-countdown/use").header("Authorization", ApiTestSupport.student(mvc)))
                .andExpect(status().isForbidden());
    }
}
