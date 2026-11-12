package com.flowedu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowApiIntegrationTest {
    @Autowired MockMvc mvc;

    private long createFromTemplate(String auth) throws Exception {
        String body = mvc.perform(post("/api/templates/low-attendance/use").header("Authorization", auth))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ApiTestSupport.read(body).get("id").asLong();
    }

    @Test
    void workflowLifecycleToggleCloneAndDelete() throws Exception {
        String auth = ApiTestSupport.teacher(mvc);
        long id = createFromTemplate(auth);

        mvc.perform(get("/api/workflows/" + id).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mvc.perform(post("/api/workflows/" + id + "/toggle").header("Authorization", auth))
                .andExpect(jsonPath("$.enabled").value(true));
        // Switch it off again so later tests never fire a live workflow by accident.
        mvc.perform(post("/api/workflows/" + id + "/toggle").header("Authorization", auth))
                .andExpect(jsonPath("$.enabled").value(false));

        mvc.perform(post("/api/workflows/" + id + "/clone").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Low Attendance Alert (copy)"))
                .andExpect(jsonPath("$.enabled").value(false));

        mvc.perform(delete("/api/workflows/" + id).header("Authorization", auth))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/workflows/" + id).header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidCronExpressionsAreRejected() throws Exception {
        String body = "{\"name\":\"Bad cron\",\"trigger\":{\"type\":\"SCHEDULE\",\"cron\":\"every day\"},"
                + "\"steps\":[{\"type\":\"EMAIL\",\"config\":{}}]}";
        mvc.perform(post("/api/workflows").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", ApiTestSupport.teacher(mvc)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Invalid cron expression")));
    }

    @Test
    void workflowsNeedAtLeastOneAction() throws Exception {
        String body = "{\"name\":\"Empty\",\"trigger\":{\"type\":\"EVENT\",\"event\":\"ATTENDANCE_UPLOADED\"},\"steps\":[]}";
        mvc.perform(post("/api/workflows").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", ApiTestSupport.teacher(mvc)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Add at least one action"));
    }

    @Test
    void unknownActionTypesAreRejected() throws Exception {
        String body = "{\"name\":\"Odd\",\"trigger\":{\"type\":\"EVENT\",\"event\":\"ATTENDANCE_UPLOADED\"},"
                + "\"steps\":[{\"type\":\"SMS\",\"config\":{}}]}";
        mvc.perform(post("/api/workflows").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", ApiTestSupport.teacher(mvc)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown action type: SMS"));
    }

    @Test
    void previewRunsWithoutSendingAnything() throws Exception {
        String auth = ApiTestSupport.teacher(mvc);
        long id = createFromTemplate(auth);
        mvc.perform(post("/api/workflows/" + id + "/test").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.run.testRun").value(true))
                .andExpect(jsonPath("$.steps").isArray());
    }

    @Test
    void studentsCannotListWorkflows() throws Exception {
        mvc.perform(get("/api/workflows").header("Authorization", ApiTestSupport.student(mvc)))
                .andExpect(status().isForbidden());
    }
}
