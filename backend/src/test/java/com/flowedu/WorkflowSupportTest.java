package com.flowedu;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowSupportTest {

    // ---- templates
    @Test
    void rendersVariablesAndFallbacks() {
        Map<String, Object> ctx = Map.of("student.name", "Aarav", "attendance.percent", 62.5);
        assertThat(WorkflowSupport.render("Hi {{student.name}}, you are at {{attendance.percent}}%", ctx))
                .isEqualTo("Hi Aarav, you are at 62.5%");
        assertThat(WorkflowSupport.render("Hello {{student.nickname|there}}", ctx)).isEqualTo("Hello there");
    }

    @Test
    void wholeNumbersRenderWithoutDecimals() {
        assertThat(WorkflowSupport.str(82.0)).isEqualTo("82");
    }

    @Test
    void unknownVariablesRenderAsEmptyText() {
        assertThat(WorkflowSupport.render("[{{missing}}]", Map.of())).isEqualTo("[]");
    }

    @Test
    void deepRenderWalksNestedMapsAndLists() {
        Map<String, Object> cfg = Map.of("subject", "Hi {{n}}", "extra", List.of("{{n}}", 3));
        @SuppressWarnings("unchecked")
        Map<String, Object> out = (Map<String, Object>) WorkflowSupport.deepRender(cfg, Map.of("n", "Diya"));
        assertThat(out.get("subject")).isEqualTo("Hi Diya");
        assertThat(out.get("extra")).isEqualTo(List.of("Diya", 3));
    }

    // ---- conditions
    private static Map<String, Object> rule(String field, String op, String value) {
        return Map.of("field", field, "op", op, "value", value);
    }

    @Test
    void comparesNumbersNumerically() {
        Map<String, Object> cond = Map.of("logic", "AND", "rules", List.of(rule("attendance.percent", "<", "75")));
        assertThat(WorkflowSupport.matches(cond, Map.of("attendance.percent", 70.0))).isTrue();
        assertThat(WorkflowSupport.matches(cond, Map.of("attendance.percent", 80.0))).isFalse();
        // "9" < "75" as text would be false, as numbers it is true
        assertThat(WorkflowSupport.matches(cond, Map.of("attendance.percent", "9"))).isTrue();
    }

    @Test
    void comparesTextIgnoringCase() {
        Map<String, Object> eq = Map.of("rules", List.of(rule("student.department", "==", "cse")));
        assertThat(WorkflowSupport.matches(eq, Map.of("student.department", "CSE"))).isTrue();
        Map<String, Object> has = Map.of("rules", List.of(rule("course.name", "contains", "data")));
        assertThat(WorkflowSupport.matches(has, Map.of("course.name", "Database Systems"))).isTrue();
    }

    @Test
    void supportsAndOrLogic() {
        var rules = List.of(rule("a", ">", "1"), rule("b", ">", "1"));
        Map<String, Object> ctx = Map.of("a", 5, "b", 0);
        assertThat(WorkflowSupport.matches(Map.of("logic", "AND", "rules", rules), ctx)).isFalse();
        assertThat(WorkflowSupport.matches(Map.of("logic", "OR", "rules", rules), ctx)).isTrue();
    }

    @Test
    void missingFieldNeverMatchesAndEmptyConditionsAlwaysDo() {
        Map<String, Object> cond = Map.of("rules", List.of(rule("nope", "==", "x")));
        assertThat(WorkflowSupport.matches(cond, Map.of())).isFalse();
        assertThat(WorkflowSupport.matches(null, Map.of())).isTrue();
        assertThat(WorkflowSupport.matches(Map.of("rules", List.of()), Map.of())).isTrue();
    }

    // ---- audience and course membership
    private static Domain.Student student(long deptId, int semester, String section, String batch) {
        return Domain.Student.builder()
                .department(Domain.Department.builder().id(deptId).name("D" + deptId).build())
                .semester(semester).section(section).batch(batch).build();
    }

    @Test
    void audienceFiltersByDepartmentSectionAndBatch() {
        var s = student(1, 5, "A", "2021");
        assertThat(WorkflowSupport.audienceMatches(Map.of("departmentId", 1, "section", "a"), s)).isTrue();
        assertThat(WorkflowSupport.audienceMatches(Map.of("departmentId", 2), s)).isFalse();
        assertThat(WorkflowSupport.audienceMatches(Map.of("batch", "2022"), s)).isFalse();
        assertThat(WorkflowSupport.audienceMatches(null, s)).isTrue();
    }

    @Test
    void studentBelongsToCourseWhenDepartmentAndSemesterMatch() {
        var s = student(1, 5, "A", "2021");
        var same = Domain.Course.builder().department(s.getDepartment()).semester(5).build();
        var otherSemester = Domain.Course.builder().department(s.getDepartment()).semester(6).build();
        assertThat(WorkflowSupport.inCourse(s, same)).isTrue();
        assertThat(WorkflowSupport.inCourse(s, otherSemester)).isFalse();
        assertThat(WorkflowSupport.inCourse(s, null)).isFalse();
    }

    // ---- JSON helpers
    @Test
    void readMapAndListTolerateBadInput() {
        assertThat(WorkflowSupport.readMap("not json")).isEmpty();
        assertThat(WorkflowSupport.readMap(null)).isEmpty();
        assertThat(WorkflowSupport.readList("{}")).isEmpty();
        assertThat(WorkflowSupport.readMap(WorkflowSupport.json(Map.of("k", "v")))).containsEntry("k", "v");
    }
}
