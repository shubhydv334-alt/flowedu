package com.flowedu;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** All JPA entities in one file to keep the showcase compact. */
public final class Domain {
    private Domain() {}

    public enum Role { ADMIN, TEACHER, STUDENT }

    @Entity(name = "AppUser") @Table(name = "app_users")
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AppUser {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @Column(unique = true, nullable = false) private String email;
        @Column(nullable = false) private String passwordHash;
        private String name;
        @Enumerated(EnumType.STRING) private Role role;
    }

    @Entity(name = "Department") @Table(name = "departments")
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Department {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @Column(unique = true, nullable = false) private String name;
    }

    @Entity(name = "Course") @Table(name = "courses")
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Course {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @Column(unique = true, nullable = false) private String code;
        private String name;
        @ManyToOne private Department department;
        private Integer semester;
        private String teacherName;
        private String teacherEmail;
        private String teacherTelegramChatId;
    }

    @Entity(name = "Student") @Table(name = "students")
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Student {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @Column(unique = true, nullable = false) private String rollNo;
        private String name;
        private String email;
        private String telegramChatId;
        @ManyToOne private Department department;
        private Integer semester;
        private String section;
        private String batch;
    }

    @Entity(name = "AttendanceRecord") @Table(name = "attendance_records")
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AttendanceRecord {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @ManyToOne private Student student;
        @ManyToOne private Course course;
        private Double percent;
        private LocalDate recordDate;
    }

    @Entity(name = "MarkRecord") @Table(name = "mark_records")
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MarkRecord {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @ManyToOne private Student student;
        @ManyToOne private Course course;
        private String examName;
        private Double marks;
        private Double maxMarks;
    }

    @Entity(name = "Assignment") @Table(name = "assignments")
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Assignment {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @ManyToOne private Course course;
        private String title;
        private LocalDateTime deadline;
    }

    @Entity(name = "Submission") @Table(name = "submissions")
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Submission {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @ManyToOne private Assignment assignment;
        @ManyToOne private Student student;
        private LocalDateTime submittedAt;
        private Boolean late;
    }

    @Entity(name = "Exam") @Table(name = "exams")
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Exam {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        @ManyToOne private Course course;
        private String name;
        private LocalDate examDate;
    }

    @Entity(name = "Workflow") @Table(name = "workflows")
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Workflow {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        private String name;
        @Column(length = 2000) private String description;
        private Boolean enabled;
        @Column(length = 20000) private String triggerJson;
        @Column(length = 20000) private String conditionsJson;
        @Column(length = 20000) private String audienceJson;
        @Column(length = 20000) private String stepsJson;
        private String createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime lastRunAt;
    }

    @Entity(name = "WorkflowRun") @Table(name = "workflow_runs")
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class WorkflowRun {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        private Long workflowId;
        private String workflowName;
        private String status;        // RUNNING, SUCCESS, PARTIAL, FAILED
        private String triggerType;   // EVENT:..., SCHEDULE, DATE, MANUAL, TEST
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
        private Integer affected;
        private Integer failures;
        private Boolean testRun;
        @Column(length = 2000) private String error;
    }

    @Entity(name = "StepLog") @Table(name = "step_logs")
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StepLog {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
        private Long runId;
        private Integer stepIndex;
        private String stepType;
        private String studentName;
        private String status;        // SUCCESS, SIMULATED, PREVIEW, FALLBACK, FAILED
        private Integer attempts;
        @Column(length = 20000) private String inputJson;
        @Column(length = 20000) private String outputJson;
        @Column(length = 2000) private String error;
        private LocalDateTime loggedAt;
    }
}
