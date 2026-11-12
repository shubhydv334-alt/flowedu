package com.flowedu;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Phase 2: departments, courses, semesters, sections, students, assignments, exams. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AcademicController {
    private static final String WRITE = "hasAnyRole('ADMIN','TEACHER')";

    private final Repos.DepartmentRepo departments;
    private final Repos.CourseRepo courses;
    private final Repos.StudentRepo students;
    private final Repos.AttendanceRepo attendance;
    private final Repos.AssignmentRepo assignments;
    private final Repos.SubmissionRepo submissions;
    private final Repos.ExamRepo exams;
    private final ApplicationEventPublisher publisher;

    public record CourseReq(String code, String name, Long departmentId, Integer semester,
                            String teacherName, String teacherEmail, String teacherTelegramChatId) {}
    public record StudentReq(String rollNo, String name, String email, String telegramChatId,
                             Long departmentId, Integer semester, String section, String batch) {}
    public record AssignmentReq(Long courseId, String title, LocalDateTime deadline) {}
    public record ExamReq(Long courseId, String name, LocalDate examDate) {}

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private ResponseEntity<Void> delete(JpaRepository<?, Long> repo, Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Departments
    @GetMapping("/departments")
    public List<Domain.Department> listDepartments() { return departments.findAll(); }

    @PostMapping("/departments") @PreAuthorize(WRITE)
    public Domain.Department createDepartment(@RequestBody Map<String, String> body) {
        String name = blankToNull(body.get("name"));
        if (name == null) throw new IllegalArgumentException("Department name is required");
        return departments.save(Domain.Department.builder().name(name).build());
    }

    @DeleteMapping("/departments/{id}") @PreAuthorize(WRITE)
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) { return delete(departments, id); }

    // ---- Courses
    @GetMapping("/courses")
    public List<Domain.Course> listCourses() { return courses.findAll(); }

    @PostMapping("/courses") @PreAuthorize(WRITE)
    public Domain.Course createCourse(@RequestBody CourseReq r) {
        if (blankToNull(r.code()) == null || blankToNull(r.name()) == null || r.departmentId() == null) {
            throw new IllegalArgumentException("Code, name and department are required");
        }
        return courses.save(Domain.Course.builder()
                .code(r.code().trim()).name(r.name().trim())
                .department(departments.findById(r.departmentId()).orElseThrow())
                .semester(r.semester())
                .teacherName(blankToNull(r.teacherName()))
                .teacherEmail(blankToNull(r.teacherEmail()))
                .teacherTelegramChatId(blankToNull(r.teacherTelegramChatId()))
                .build());
    }

    @DeleteMapping("/courses/{id}") @PreAuthorize(WRITE)
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) { return delete(courses, id); }

    // ---- Students
    @GetMapping("/students")
    public List<Domain.Student> listStudents(@RequestParam(required = false) Long departmentId,
                                             @RequestParam(required = false) Integer semester,
                                             @RequestParam(required = false) String section,
                                             @RequestParam(required = false) String batch) {
        return students.findAll().stream()
                .filter(s -> departmentId == null || (s.getDepartment() != null && departmentId.equals(s.getDepartment().getId())))
                .filter(s -> semester == null || semester.equals(s.getSemester()))
                .filter(s -> section == null || section.equalsIgnoreCase(s.getSection()))
                .filter(s -> batch == null || batch.equalsIgnoreCase(s.getBatch()))
                .toList();
    }

    @PostMapping("/students") @PreAuthorize(WRITE)
    public Domain.Student createStudent(@RequestBody StudentReq r) {
        if (blankToNull(r.rollNo()) == null || blankToNull(r.name()) == null) {
            throw new IllegalArgumentException("Roll number and name are required");
        }
        return students.save(Domain.Student.builder()
                .rollNo(r.rollNo().trim()).name(r.name().trim())
                .email(blankToNull(r.email()))
                .telegramChatId(blankToNull(r.telegramChatId()))
                .department(r.departmentId() == null ? null : departments.findById(r.departmentId()).orElseThrow())
                .semester(r.semester())
                .section(blankToNull(r.section()))
                .batch(blankToNull(r.batch()))
                .build());
    }

    @DeleteMapping("/students/{id}") @PreAuthorize(WRITE)
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) { return delete(students, id); }

    // ---- Attendance (read-only here, written by CSV import)
    @GetMapping("/attendance")
    public List<Domain.AttendanceRecord> listAttendance() {
        return attendance.findAll().stream()
                .sorted(Comparator.comparing(Domain.AttendanceRecord::getId).reversed())
                .limit(500).toList();
    }

    // ---- Assignments (Phase 6)
    @GetMapping("/assignments")
    public List<Domain.Assignment> listAssignments() { return assignments.findAll(); }

    @PostMapping("/assignments") @PreAuthorize(WRITE)
    public Domain.Assignment createAssignment(@RequestBody AssignmentReq r) {
        if (r.courseId() == null || blankToNull(r.title()) == null || r.deadline() == null) {
            throw new IllegalArgumentException("Course, title and deadline are required");
        }
        return assignments.save(Domain.Assignment.builder()
                .course(courses.findById(r.courseId()).orElseThrow())
                .title(r.title().trim()).deadline(r.deadline()).build());
    }

    @DeleteMapping("/assignments/{id}") @PreAuthorize(WRITE)
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        assignments.findById(id).ifPresent(a -> submissions.deleteAll(submissions.findByAssignment(a)));
        return delete(assignments, id);
    }

    /** The "not submitted" list. */
    @GetMapping("/assignments/{id}/not-submitted")
    public List<Domain.Student> notSubmitted(@PathVariable Long id) {
        var a = assignments.findById(id).orElseThrow();
        Set<Long> done = submissions.findByAssignment(a).stream()
                .map(s -> s.getStudent().getId()).collect(Collectors.toSet());
        return students.findAll().stream()
                .filter(s -> WorkflowSupport.inCourse(s, a.getCourse()))
                .filter(s -> !done.contains(s.getId()))
                .toList();
    }

    /** Submissions with the late flag. */
    @GetMapping("/assignments/{id}/submissions")
    public List<Domain.Submission> submissionsOf(@PathVariable Long id) {
        return submissions.findByAssignment(assignments.findById(id).orElseThrow());
    }

    @PostMapping("/assignments/{id}/submit") @PreAuthorize(WRITE)
    public Domain.Submission submit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var a = assignments.findById(id).orElseThrow();
        Object sid = body.get("studentId");
        if (sid == null) throw new IllegalArgumentException("studentId is required");
        var s = students.findById(Long.valueOf(String.valueOf(sid))).orElseThrow();
        if (submissions.existsByAssignmentAndStudent(a, s)) throw new IllegalArgumentException("Already submitted");
        LocalDateTime now = LocalDateTime.now();
        var saved = submissions.save(Domain.Submission.builder()
                .assignment(a).student(s).submittedAt(now).late(now.isAfter(a.getDeadline())).build());
        publisher.publishEvent(new Triggers.AssignmentSubmitted(a.getId(), s.getId()));
        return saved;
    }

    // ---- Exams (Phase 6)
    @GetMapping("/exams")
    public List<Domain.Exam> listExams() { return exams.findAll(); }

    @PostMapping("/exams") @PreAuthorize(WRITE)
    public Domain.Exam createExam(@RequestBody ExamReq r) {
        if (r.courseId() == null || blankToNull(r.name()) == null || r.examDate() == null) {
            throw new IllegalArgumentException("Course, name and exam date are required");
        }
        return exams.save(Domain.Exam.builder()
                .course(courses.findById(r.courseId()).orElseThrow())
                .name(r.name().trim()).examDate(r.examDate()).build());
    }

    @DeleteMapping("/exams/{id}") @PreAuthorize(WRITE)
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) { return delete(exams, id); }
}
