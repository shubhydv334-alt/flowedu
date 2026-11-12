package com.flowedu;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AcademicApiIntegrationTest {
    @Autowired MockMvc mvc;

    private String json(String body) { return body; }

    @Test
    void anySignedInUserCanReadStudents() throws Exception {
        mvc.perform(get("/api/students").header("Authorization", ApiTestSupport.student(mvc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].rollNo", hasItem("CSE21001")));
    }

    @Test
    void studentsCanBeFilteredBySection() throws Exception {
        mvc.perform(get("/api/students").param("section", "B").header("Authorization", ApiTestSupport.teacher(mvc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].section", everyItem(is("B"))));
    }

    @Test
    void studentsCannotCreateDepartments() throws Exception {
        mvc.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"name\":\"Mech\"}")).header("Authorization", ApiTestSupport.student(mvc)))
                .andExpect(status().isForbidden());
    }

    @Test
    void departmentNameIsRequired() throws Exception {
        mvc.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"name\":\"  \"}")).header("Authorization", ApiTestSupport.teacher(mvc)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Department name is required"));
    }

    @Test
    void duplicateDepartmentsAreRejectedWithConflict() throws Exception {
        String name = "Dept-" + UUID.randomUUID().toString().substring(0, 8);
        String auth = ApiTestSupport.teacher(mvc);
        mvc.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"name\":\"" + name + "\"}")).header("Authorization", auth))
                .andExpect(status().isOk());
        mvc.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"name\":\"" + name + "\"}")).header("Authorization", auth))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void studentCreationRequiresRollNumberAndName() throws Exception {
        mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"rollNo\":\"X1\"}")).header("Authorization", ApiTestSupport.teacher(mvc)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Roll number and name are required"));
    }

    @Test
    void assignmentSubmissionLifecycle() throws Exception {
        String auth = ApiTestSupport.teacher(mvc);

        JsonNode course = null;
        for (JsonNode c : ApiTestSupport.read(mvc.perform(get("/api/courses").header("Authorization", auth))
                .andReturn().getResponse().getContentAsString())) {
            if ("CS501".equals(c.get("code").asText())) course = c;
        }
        long courseId = course.get("id").asLong();

        String created = mvc.perform(post("/api/assignments").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"courseId\":" + courseId + ",\"title\":\"Lifecycle test\",\"deadline\":\"2030-01-01T17:00:00\"}"))
                        .header("Authorization", auth))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long assignmentId = ApiTestSupport.read(created).get("id").asLong();

        JsonNode pending = ApiTestSupport.read(mvc.perform(get("/api/assignments/" + assignmentId + "/not-submitted").header("Authorization", auth))
                .andReturn().getResponse().getContentAsString());
        int before = pending.size();
        long studentId = pending.get(0).get("id").asLong();

        mvc.perform(post("/api/assignments/" + assignmentId + "/submit").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"studentId\":" + studentId + "}")).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.late").value(false));

        JsonNode after = ApiTestSupport.read(mvc.perform(get("/api/assignments/" + assignmentId + "/not-submitted").header("Authorization", auth))
                .andReturn().getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(before - 1, after.size());

        mvc.perform(post("/api/assignments/" + assignmentId + "/submit").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"studentId\":" + studentId + "}")).header("Authorization", auth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Already submitted"));
    }

    @Test
    void assignmentRequiresCourseTitleAndDeadline() throws Exception {
        mvc.perform(post("/api/assignments").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"title\":\"No course\"}")).header("Authorization", ApiTestSupport.teacher(mvc)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Course, title and deadline are required"));
    }
}
