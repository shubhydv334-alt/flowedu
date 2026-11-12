package com.flowedu;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Phase 9: demo data so the app is usable the first time it starts. */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final Repos.UserRepo users;
    private final Repos.DepartmentRepo departments;
    private final Repos.CourseRepo courses;
    private final Repos.StudentRepo students;
    private final Repos.AttendanceRepo attendance;
    private final Repos.AssignmentRepo assignments;
    private final Repos.SubmissionRepo submissions;
    private final Repos.ExamRepo exams;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;

        user("admin@flowedu.local", "admin123", "Admin", Domain.Role.ADMIN);
        user("teacher@flowedu.local", "teacher123", "Dr. Rao", Domain.Role.TEACHER);
        user("student@flowedu.local", "student123", "Aarav Sharma", Domain.Role.STUDENT);

        var cse = departments.save(Domain.Department.builder().name("CSE").build());
        var ece = departments.save(Domain.Department.builder().name("ECE").build());

        var se = course("CS501", "Software Engineering", cse, "Dr. Rao", "rao@example.edu");
        var db = course("CS502", "Database Systems", cse, "Prof. Mehta", "mehta@example.edu");
        var dsp = course("EC501", "Digital Signal Processing", ece, "Dr. Iyer", "iyer@example.edu");

        String[][] rows = {
            {"CSE21001", "Aarav Sharma", "CSE", "A"}, {"CSE21002", "Diya Patel", "CSE", "A"},
            {"CSE21003", "Rohan Verma", "CSE", "A"},  {"CSE21004", "Ananya Iyer", "CSE", "B"},
            {"CSE21005", "Kabir Singh", "CSE", "B"},  {"CSE21006", "Meera Nair", "CSE", "B"},
            {"ECE21001", "Ishaan Gupta", "ECE", "A"}, {"ECE21002", "Saanvi Reddy", "ECE", "A"},
            {"ECE21003", "Vihaan Joshi", "ECE", "A"}, {"ECE21004", "Tara Menon", "ECE", "A"}};

        for (String[] r : rows) {
            var dept = r[2].equals("CSE") ? cse : ece;
            var s = students.save(Domain.Student.builder()
                    .rollNo(r[0]).name(r[1]).email(r[0].toLowerCase() + "@example.edu")
                    .department(dept).semester(5).section(r[3]).batch("2021").build());
            // Baseline attendance (all fine). Upload sample-data/attendance.csv to trigger the demo.
            List<Domain.Course> cs = r[2].equals("CSE") ? List.of(se, db) : List.of(dsp);
            for (Domain.Course c : cs) {
                attendance.save(Domain.AttendanceRecord.builder()
                        .student(s).course(c).percent(82.0).recordDate(LocalDate.now().minusDays(7)).build());
            }
        }

        var lab = assignments.save(Domain.Assignment.builder().course(db).title("DBMS Lab Report")
                .deadline(LocalDate.now().plusDays(2).atTime(17, 0)).build());
        assignments.save(Domain.Assignment.builder().course(se).title("SE Case Study")
                .deadline(LocalDate.now().plusDays(5).atTime(17, 0)).build());
        // Two students already submitted the lab report
        for (String roll : List.of("CSE21001", "CSE21002")) {
            students.findByRollNoIgnoreCase(roll).ifPresent(s -> submissions.save(Domain.Submission.builder()
                    .assignment(lab).student(s).submittedAt(LocalDateTime.now().minusHours(3)).late(false).build()));
        }

        exams.save(Domain.Exam.builder().course(se).name("SE Mid-Sem").examDate(LocalDate.now().plusDays(3)).build());
        exams.save(Domain.Exam.builder().course(dsp).name("DSP Mid-Sem").examDate(LocalDate.now().plusDays(3)).build());
    }

    private void user(String email, String pass, String name, Domain.Role role) {
        users.save(Domain.AppUser.builder().email(email).passwordHash(encoder.encode(pass)).name(name).role(role).build());
    }

    private Domain.Course course(String code, String name, Domain.Department d, String teacher, String teacherEmail) {
        return courses.save(Domain.Course.builder().code(code).name(name).department(d).semester(5)
                .teacherName(teacher).teacherEmail(teacherEmail).build());
    }
}
