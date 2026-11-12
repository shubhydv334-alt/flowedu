package com.flowedu;

import com.opencsv.CSVReaderHeaderAware;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/**
 * Phase 2: bulk CSV import for students, attendance and marks.
 * Two-step flow: preview (validate, nothing saved) then commit.
 *
 *  students   : rollNo,name,email,telegramChatId,department,semester,section,batch
 *  attendance : rollNo,courseCode,percent,date            (date optional, defaults to today)
 *  marks      : rollNo,courseCode,exam,marks,maxMarks     (maxMarks optional, defaults to 100)
 */
@Service
@RequiredArgsConstructor
public class ImportService {
    private final Repos.DepartmentRepo departments;
    private final Repos.CourseRepo courses;
    private final Repos.StudentRepo students;
    private final Repos.AttendanceRepo attendance;
    private final Repos.MarkRepo marks;
    private final ApplicationEventPublisher publisher;

    public record Row(int line, Map<String, String> data, String error) {}

    @Transactional
    public Map<String, Object> process(String type, MultipartFile file, boolean commit) throws Exception {
        if (!List.of("students", "attendance", "marks").contains(type)) {
            throw new IllegalArgumentException("Unknown import type: " + type);
        }
        List<Row> rows = parse(type, file);
        int valid = (int) rows.stream().filter(r -> r.error() == null).count();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", type);
        out.put("total", rows.size());
        out.put("valid", valid);
        out.put("invalid", rows.size() - valid);
        List<Map<String, Object>> errors = new ArrayList<>();
        for (Row r : rows) {
            if (r.error() != null && errors.size() < 50) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("line", r.line());
                e.put("message", r.error());
                errors.add(e);
            }
        }
        out.put("errors", errors);
        out.put("preview", rows.stream().limit(10).map(Row::data).toList());

        if (commit) {
            int n = 0;
            for (Row r : rows) {
                if (r.error() == null) { save(type, r.data()); n++; }
            }
            out.put("imported", n);
            if (type.equals("attendance") && n > 0) {
                // Fired after commit; starts every enabled "Attendance uploaded" workflow.
                publisher.publishEvent(new Triggers.AttendanceUploaded(n));
            }
        }
        return out;
    }

    private List<Row> parse(String type, MultipartFile file) throws Exception {
        List<Row> rows = new ArrayList<>();
        try (var reader = new CSVReaderHeaderAware(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            Map<String, String> m;
            int line = 1;
            while ((m = reader.readMap()) != null) {
                line++;
                Map<String, String> norm = new LinkedHashMap<>();
                for (var e : m.entrySet()) {
                    String key = e.getKey() == null ? "" : e.getKey().replaceAll("[^A-Za-z0-9]", "").toLowerCase();
                    norm.put(key, e.getValue() == null ? "" : e.getValue().trim());
                }
                rows.add(validate(type, line, norm));
            }
        }
        return rows;
    }

    private Row validate(String type, int line, Map<String, String> m) {
        String err = null;
        switch (type) {
            case "students" -> {
                err = need(m, "rollno", "name");
                if (err == null && !m.getOrDefault("semester", "").isBlank() && !isInt(m.get("semester"))) {
                    err = "semester must be a whole number";
                }
            }
            case "attendance" -> {
                err = need(m, "rollno", "coursecode", "percent");
                if (err == null && !isNum(m.get("percent"))) err = "percent must be a number";
                if (err == null && (Double.parseDouble(m.get("percent")) < 0 || Double.parseDouble(m.get("percent")) > 100)) {
                    err = "percent must be between 0 and 100";
                }
                if (err == null && students.findByRollNoIgnoreCase(m.get("rollno")).isEmpty()) err = "unknown student " + m.get("rollno");
                if (err == null && courses.findByCodeIgnoreCase(m.get("coursecode")).isEmpty()) err = "unknown course " + m.get("coursecode");
                if (err == null && !m.getOrDefault("date", "").isBlank() && !isDate(m.get("date"))) err = "date must be yyyy-mm-dd";
            }
            case "marks" -> {
                err = need(m, "rollno", "coursecode", "exam", "marks");
                if (err == null && !isNum(m.get("marks"))) err = "marks must be a number";
                if (err == null && !m.getOrDefault("maxmarks", "").isBlank() && !isNum(m.get("maxmarks"))) err = "maxMarks must be a number";
                if (err == null && students.findByRollNoIgnoreCase(m.get("rollno")).isEmpty()) err = "unknown student " + m.get("rollno");
                if (err == null && courses.findByCodeIgnoreCase(m.get("coursecode")).isEmpty()) err = "unknown course " + m.get("coursecode");
            }
            default -> err = "unsupported type";
        }
        return new Row(line, m, err);
    }

    private void save(String type, Map<String, String> m) {
        switch (type) {
            case "students" -> {
                var s = students.findByRollNoIgnoreCase(m.get("rollno")).orElseGet(Domain.Student::new);
                s.setRollNo(m.get("rollno"));
                s.setName(m.get("name"));
                s.setEmail(nullIfBlank(m.get("email")));
                s.setTelegramChatId(nullIfBlank(m.get("telegramchatid")));
                String dn = m.getOrDefault("department", "");
                if (!dn.isBlank()) {
                    s.setDepartment(departments.findByNameIgnoreCase(dn)
                            .orElseGet(() -> departments.save(Domain.Department.builder().name(dn).build())));
                }
                s.setSemester(isInt(m.get("semester")) ? Integer.valueOf(m.get("semester")) : null);
                s.setSection(nullIfBlank(m.get("section")));
                s.setBatch(nullIfBlank(m.get("batch")));
                students.save(s);
            }
            case "attendance" -> {
                String d = m.getOrDefault("date", "");
                attendance.save(Domain.AttendanceRecord.builder()
                        .student(students.findByRollNoIgnoreCase(m.get("rollno")).orElseThrow())
                        .course(courses.findByCodeIgnoreCase(m.get("coursecode")).orElseThrow())
                        .percent(Double.valueOf(m.get("percent")))
                        .recordDate(d.isBlank() ? LocalDate.now() : LocalDate.parse(d))
                        .build());
            }
            case "marks" -> {
                String max = m.getOrDefault("maxmarks", "");
                marks.save(Domain.MarkRecord.builder()
                        .student(students.findByRollNoIgnoreCase(m.get("rollno")).orElseThrow())
                        .course(courses.findByCodeIgnoreCase(m.get("coursecode")).orElseThrow())
                        .examName(m.get("exam"))
                        .marks(Double.valueOf(m.get("marks")))
                        .maxMarks(max.isBlank() ? 100.0 : Double.valueOf(max))
                        .build());
            }
            default -> throw new IllegalArgumentException("unsupported type");
        }
    }

    private static String need(Map<String, String> m, String... keys) {
        for (String k : keys) {
            if (m.getOrDefault(k, "").isBlank()) return "missing value for " + k;
        }
        return null;
    }
    private static String nullIfBlank(String s) { return s == null || s.isBlank() ? null : s; }
    private static boolean isInt(String s) { try { Integer.parseInt(s); return true; } catch (Exception e) { return false; } }
    private static boolean isNum(String s) { try { Double.parseDouble(s); return true; } catch (Exception e) { return false; } }
    private static boolean isDate(String s) { try { LocalDate.parse(s); return true; } catch (Exception e) { return false; } }
}
