package com.flowedu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImportApiIntegrationTest {
    @Autowired MockMvc mvc;

    private MockHttpServletRequestBuilder upload(String type, String csv, boolean preview, String auth) {
        var file = new MockMultipartFile("file", "data.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        return multipart("/api/import/" + type).file(file).param("preview", String.valueOf(preview)).header("Authorization", auth);
    }

    private static final String STUDENTS = "rollNo,name,email,department,semester,section,batch\n"
            + "IMPORT001,Test Student,t1@example.edu,CSE,5,A,2021\n"
            + ",Missing Roll,t2@example.edu,CSE,5,A,2021\n"
            + "IMPORT003,Bad Semester,t3@example.edu,CSE,five,A,2021\n";

    @Test
    void previewValidatesRowsWithoutSavingAnything() throws Exception {
        String auth = ApiTestSupport.teacher(mvc);
        mvc.perform(upload("students", STUDENTS, true, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.valid").value(1))
                .andExpect(jsonPath("$.invalid").value(2))
                .andExpect(jsonPath("$.errors[0].line").value(3))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("rollno")))
                .andExpect(jsonPath("$.errors[1].message").value("semester must be a whole number"));
        mvc.perform(get("/api/students").header("Authorization", auth))
                .andExpect(jsonPath("$[*].rollNo", not(hasItem("IMPORT001"))));
    }

    @Test
    void commitSavesOnlyTheValidRows() throws Exception {
        // Different roll numbers from the preview test, so the two tests can run in any order.
        String csv = STUDENTS.replace("IMPORT001", "IMPORT101").replace("IMPORT003", "IMPORT103");
        String auth = ApiTestSupport.teacher(mvc);
        mvc.perform(upload("students", csv, false, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1));
        mvc.perform(get("/api/students").header("Authorization", auth))
                .andExpect(jsonPath("$[*].rollNo", hasItem("IMPORT101")))
                .andExpect(jsonPath("$[*].rollNo", not(hasItem("IMPORT103"))));
    }

    @Test
    void attendanceRowsMustReferenceKnownStudentsAndCourses() throws Exception {
        String csv = "rollNo,courseCode,percent\nNOPE1,CS501,70\nCSE21001,ZZ999,70\nCSE21001,CS501,140\n";
        mvc.perform(upload("attendance", csv, true, ApiTestSupport.teacher(mvc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(0))
                .andExpect(jsonPath("$.errors[0].message").value("unknown student NOPE1"))
                .andExpect(jsonPath("$.errors[1].message").value("unknown course ZZ999"))
                .andExpect(jsonPath("$.errors[2].message").value("percent must be between 0 and 100"));
    }

    @Test
    void emptyFilesAreRejected() throws Exception {
        mvc.perform(upload("students", "", true, ApiTestSupport.teacher(mvc)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("The file is empty"));
    }

    @Test
    void unknownImportTypesAreRejected() throws Exception {
        mvc.perform(upload("grades", "a,b\n1,2\n", true, ApiTestSupport.teacher(mvc)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown import type: grades"));
    }

    @Test
    void studentsCannotImport() throws Exception {
        mvc.perform(upload("students", STUDENTS, true, ApiTestSupport.student(mvc)))
                .andExpect(status().isForbidden());
    }
}
