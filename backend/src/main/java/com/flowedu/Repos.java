package com.flowedu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** All Spring Data repositories (nested; enabled via considerNestedRepositories). */
public final class Repos {
    private Repos() {}

    public interface UserRepo extends JpaRepository<Domain.AppUser, Long> {
        Optional<Domain.AppUser> findByEmail(String email);
    }

    public interface DepartmentRepo extends JpaRepository<Domain.Department, Long> {
        Optional<Domain.Department> findByNameIgnoreCase(String name);
    }

    public interface CourseRepo extends JpaRepository<Domain.Course, Long> {
        Optional<Domain.Course> findByCodeIgnoreCase(String code);
    }

    public interface StudentRepo extends JpaRepository<Domain.Student, Long> {
        Optional<Domain.Student> findByRollNoIgnoreCase(String rollNo);
    }

    public interface AttendanceRepo extends JpaRepository<Domain.AttendanceRecord, Long> {}

    public interface MarkRepo extends JpaRepository<Domain.MarkRecord, Long> {}

    public interface AssignmentRepo extends JpaRepository<Domain.Assignment, Long> {}

    public interface SubmissionRepo extends JpaRepository<Domain.Submission, Long> {
        List<Domain.Submission> findByAssignment(Domain.Assignment assignment);
        boolean existsByAssignmentAndStudent(Domain.Assignment assignment, Domain.Student student);
    }

    public interface ExamRepo extends JpaRepository<Domain.Exam, Long> {}

    public interface WorkflowRepo extends JpaRepository<Domain.Workflow, Long> {
        List<Domain.Workflow> findByEnabledTrue();
    }

    public interface RunRepo extends JpaRepository<Domain.WorkflowRun, Long> {
        List<Domain.WorkflowRun> findTop100ByOrderByStartedAtDesc();
        List<Domain.WorkflowRun> findTop10ByOrderByStartedAtDesc();
    }

    public interface StepLogRepo extends JpaRepository<Domain.StepLog, Long> {
        List<Domain.StepLog> findByRunIdOrderByIdAsc(Long runId);
        long countByStatusInAndStepTypeIn(Collection<String> statuses, Collection<String> types);
        long countByStatus(String status);
    }
}
