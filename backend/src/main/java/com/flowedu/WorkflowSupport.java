package com.flowedu;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure helpers: JSON, template variables, conditions, audience. */
public final class WorkflowSupport {
    private WorkflowSupport() {}

    public static final ObjectMapper OM = new ObjectMapper();

    /** {{student.name}} or {{student.name|there}} (text after | is the fallback when the value is empty). */
    private static final Pattern VAR = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*(?:\\|\\s*([^}]*?))?\\s*\\}\\}");

    // ---- JSON
    public static String json(Object o) {
        try { return OM.writeValueAsString(o); } catch (Exception e) { return "{}"; }
    }

    public static Map<String, Object> readMap(String s) {
        if (s == null || s.isBlank()) return new LinkedHashMap<>();
        try { return OM.readValue(s, new TypeReference<LinkedHashMap<String, Object>>() {}); }
        catch (Exception e) { return new LinkedHashMap<>(); }
    }

    public static List<Map<String, Object>> readList(String s) {
        if (s == null || s.isBlank()) return new ArrayList<>();
        try { return OM.readValue(s, new TypeReference<List<Map<String, Object>>>() {}); }
        catch (Exception e) { return new ArrayList<>(); }
    }

    // ---- Values
    public static String str(Object o) {
        if (o == null) return "";
        if (o instanceof Double || o instanceof Float) {
            return BigDecimal.valueOf(((Number) o).doubleValue())
                    .setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(o);
    }

    private static Double num(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof String s) {
            try { return Double.valueOf(s.trim()); } catch (Exception e) { return null; }
        }
        return null;
    }

    private static boolean blank(Object o) { return o == null || String.valueOf(o).isBlank(); }

    // ---- Templates
    public static String render(String template, Map<String, Object> ctx) {
        if (template == null) return null;
        Matcher m = VAR.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String v = str(ctx.get(m.group(1)));
            if (v.isEmpty() && m.group(2) != null) v = m.group(2);
            m.appendReplacement(sb, Matcher.quoteReplacement(v));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static Object deepRender(Object o, Map<String, Object> ctx) {
        if (o instanceof String s) return render(s, ctx);
        if (o instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (var e : map.entrySet()) out.put(String.valueOf(e.getKey()), deepRender(e.getValue(), ctx));
            return out;
        }
        if (o instanceof List<?> list) {
            List<Object> out = new ArrayList<>();
            for (Object x : list) out.add(deepRender(x, ctx));
            return out;
        }
        return o;
    }

    // ---- Conditions: {"logic":"AND","rules":[{"field":"attendance.percent","op":"<","value":"75"}]}
    @SuppressWarnings("unchecked")
    public static boolean matches(Map<String, Object> cond, Map<String, Object> ctx) {
        if (cond == null) return true;
        Object rulesObj = cond.get("rules");
        if (!(rulesObj instanceof List<?> rules) || rules.isEmpty()) return true;
        boolean or = "OR".equalsIgnoreCase(String.valueOf(cond.get("logic")));
        for (Object r : rules) {
            boolean ok = test((Map<String, Object>) r, ctx);
            if (or && ok) return true;
            if (!or && !ok) return false;
        }
        return !or;
    }

    private static boolean test(Map<String, Object> rule, Map<String, Object> ctx) {
        Object left = ctx.get(String.valueOf(rule.get("field")));
        if (left == null) return false;
        String op = String.valueOf(rule.get("op"));
        String right = rule.get("value") == null ? "" : String.valueOf(rule.get("value"));
        Double a = num(left), b = num(right);
        if (a != null && b != null && !"contains".equals(op)) {
            int c = Double.compare(a, b);
            return switch (op) {
                case "<" -> c < 0;
                case "<=" -> c <= 0;
                case ">" -> c > 0;
                case ">=" -> c >= 0;
                case "==" -> c == 0;
                case "!=" -> c != 0;
                default -> false;
            };
        }
        String x = str(left);
        return switch (op) {
            case "==" -> x.equalsIgnoreCase(right);
            case "!=" -> !x.equalsIgnoreCase(right);
            case "contains" -> x.toLowerCase().contains(right.toLowerCase());
            default -> false;
        };
    }

    // ---- Audience: {"departmentId":1,"batch":"2021","section":"A"}
    public static boolean audienceMatches(Map<String, Object> a, Domain.Student s) {
        if (a == null) return true;
        if (!blank(a.get("departmentId"))) {
            if (s.getDepartment() == null
                    || !String.valueOf(s.getDepartment().getId()).equals(String.valueOf(a.get("departmentId")))) return false;
        }
        if (!blank(a.get("batch")) && !String.valueOf(a.get("batch")).equalsIgnoreCase(String.valueOf(s.getBatch()))) return false;
        if (!blank(a.get("section")) && !String.valueOf(a.get("section")).equalsIgnoreCase(String.valueOf(s.getSection()))) return false;
        return true;
    }

    /** A student belongs to a course when department and semester match. */
    public static boolean inCourse(Domain.Student s, Domain.Course c) {
        return c != null && s.getDepartment() != null && c.getDepartment() != null
                && Objects.equals(s.getDepartment().getId(), c.getDepartment().getId())
                && Objects.equals(s.getSemester(), c.getSemester());
    }
}
