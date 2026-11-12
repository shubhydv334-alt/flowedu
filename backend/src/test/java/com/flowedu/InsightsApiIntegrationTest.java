package com.flowedu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InsightsApiIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void dashboardReportsCountsAndSevenDaysOfRuns() throws Exception {
        mvc.perform(get("/api/dashboard").header("Authorization", ApiTestSupport.teacher(mvc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflows").isNumber())
                .andExpect(jsonPath("$.activeStudents", greaterThanOrEqualTo(10)))
                .andExpect(jsonPath("$.runsPerDay", hasSize(7)))
                .andExpect(jsonPath("$.recentRuns").isArray());
    }

    @Test
    void dashboardIsAvailableToStudents() throws Exception {
        mvc.perform(get("/api/dashboard").header("Authorization", ApiTestSupport.student(mvc)))
                .andExpect(status().isOk());
    }

    @Test
    void dashboardRequiresSignIn() throws Exception {
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
    }
}
