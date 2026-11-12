package com.flowedu;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Phase 4 and 7: the four built-in actions (Email, Telegram, Webhook, AI). */
public final class Actions {
    private Actions() {}

    static final Duration TIMEOUT = Duration.ofSeconds(20);

    static String s(Object o) { return WorkflowSupport.str(o); }

    /** Readable error text: HTTP status plus the service's own reply, with secrets masked. */
    static String describe(Throwable e, String... secrets) {
        String msg;
        if (e instanceof org.springframework.web.reactive.function.client.WebClientResponseException w) {
            String body = w.getResponseBodyAsString();
            if (body.length() > 300) body = body.substring(0, 300) + "...";
            msg = w.getStatusCode().value() + " " + body;
        } else {
            msg = String.valueOf(e.getMessage());
        }
        for (String sec : secrets) {
            if (sec != null && sec.length() > 4) msg = msg.replace(sec, "***");
        }
        return msg;
    }

    // ------------------------------------------------------------------ EMAIL
    @Component
    public static class EmailAction implements Action {
        private final ObjectProvider<JavaMailSender> sender;
        private final String host;
        private final String from;

        public EmailAction(ObjectProvider<JavaMailSender> sender,
                           @Value("${spring.mail.host:}") String host,
                           @Value("${flowedu.mail-from}") String from) {
            this.sender = sender;
            this.host = host;
            this.from = from;
        }

        @Override public String type() { return "EMAIL"; }

        @Override
        public Map<String, Object> run(Map<String, Object> cfg, Map<String, Object> ctx, Options o) {
            String to;
            if (o.testMode()) {
                to = s(o.testEmail());
            } else {
                String r = s(cfg.getOrDefault("recipient", "STUDENT")).toUpperCase();
                to = switch (r) {
                    case "TEACHER" -> s(ctx.get("course.teacherEmail"));
                    case "CUSTOM" -> s(cfg.get("to"));
                    default -> s(ctx.get("student.email"));
                };
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("to", to);
            out.put("subject", s(cfg.get("subject")));
            out.put("body", s(cfg.get("body")));
            if (o.dryRun()) return out;
            if (to.isBlank()) throw new IllegalStateException("No recipient email address");

            JavaMailSender js = host == null || host.isBlank() ? null : sender.getIfAvailable();
            if (js == null) {            // mail server not configured: log-only mode for demos
                out.put("simulated", true);
                return out;
            }
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(to);
            m.setSubject(s(cfg.get("subject")));
            m.setText(s(cfg.get("body")));
            js.send(m);
            return out;
        }
    }

    // --------------------------------------------------------------- TELEGRAM
    @Component
    public static class TelegramAction implements Action {
        private final String token;

        public TelegramAction(@Value("${flowedu.telegram-token:}") String token) { this.token = token; }

        @Override public String type() { return "TELEGRAM"; }

        @Override
        public Map<String, Object> run(Map<String, Object> cfg, Map<String, Object> ctx, Options o) {
            String chat;
            if (o.testMode()) {
                chat = s(o.testChat());
            } else {
                String r = s(cfg.getOrDefault("recipient", "STUDENT")).toUpperCase();
                chat = switch (r) {
                    case "TEACHER" -> s(ctx.get("course.teacherTelegram"));
                    case "CUSTOM" -> s(cfg.get("chatId"));
                    default -> s(ctx.get("student.telegramChatId"));
                };
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("chatId", chat);
            out.put("text", s(cfg.get("text")));
            if (o.dryRun()) return out;
            if (token == null || token.isBlank()) {   // no bot token: log-only mode for demos
                out.put("simulated", true);
                return out;
            }
            if (chat.isBlank()) throw new IllegalStateException("No Telegram chat id for this recipient");
            try {
                WebClient.create().post()
                        .uri("https://api.telegram.org/bot" + token + "/sendMessage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("chat_id", chat, "text", s(cfg.get("text"))))
                        .retrieve().bodyToMono(String.class).block(TIMEOUT);
            } catch (Exception e) {
                // Telegram's reply says why (e.g. "chat not found"); never include the bot token.
                throw new IllegalStateException("Telegram: " + describe(e, token));
            }
            return out;
        }
    }

    // ---------------------------------------------------------------- WEBHOOK
    @Component
    public static class WebhookAction implements Action {
        private final boolean allowPrivate;

        public WebhookAction(@Value("${flowedu.allow-private-webhooks:false}") boolean allowPrivate) {
            this.allowPrivate = allowPrivate;
        }

        @Override public String type() { return "WEBHOOK"; }

        @Override
        public Map<String, Object> run(Map<String, Object> cfg, Map<String, Object> ctx, Options o) {
            String url = s(cfg.get("url"));
            String method = s(cfg.getOrDefault("method", "POST")).toUpperCase();
            String body = s(cfg.get("body"));
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("url", url);
            out.put("method", method);
            out.put("body", body);
            if (o.dryRun()) return out;
            UrlGuard.checkWebhookUrl(url, allowPrivate);
            if (o.testMode()) {          // never call external systems from "send test to me"
                out.put("simulated", true);
                return out;
            }
            String resp = WebClient.create().method(HttpMethod.valueOf(method))
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body.isBlank() ? "{}" : body)
                    .retrieve().bodyToMono(String.class).defaultIfEmpty("").block(TIMEOUT);
            out.put("response", resp == null ? "" : (resp.length() > 500 ? resp.substring(0, 500) : resp));
            return out;
        }
    }

    // --------------------------------------------------------------------- AI
    /**
     * Personalized reminders, summaries and tone rewrite using FREE AI tiers.
     *
     *  provider=auto (default) tries, in order, every provider that has a key:
     *      gemini (Google AI Studio free key) -> groq (free key) -> openrouter (free ":free" models)
     *  provider=gemini | groq | openrouter uses only that one.
     *  provider=pollinations needs NO key, but the prompt (with student names) goes to a public
     *      third-party service, so it is never used by "auto". Choose it explicitly, for demos only.
     *
     * If every provider fails, the engine falls back to the step's "fallback" text.
     * The result is stored as ai.message for the following steps.
     */
    @Component
    public static class AiAction implements Action {
        private final String provider, geminiKey, geminiModel, groqKey, groqModel, openrouterKey, openrouterModel;

        public AiAction(@Value("${flowedu.ai.provider:auto}") String provider,
                        @Value("${flowedu.ai.gemini-key:}") String geminiKey,
                        @Value("${flowedu.ai.gemini-model:gemini-3.5-flash-lite}") String geminiModel,
                        @Value("${flowedu.ai.groq-key:}") String groqKey,
                        @Value("${flowedu.ai.groq-model:openai/gpt-oss-20b}") String groqModel,
                        @Value("${flowedu.ai.openrouter-key:}") String openrouterKey,
                        @Value("${flowedu.ai.openrouter-model:openrouter/free}") String openrouterModel) {
            this.provider = provider == null ? "auto" : provider.trim().toLowerCase();
            this.geminiKey = geminiKey;
            this.geminiModel = geminiModel;
            this.groqKey = groqKey;
            this.groqModel = groqModel;
            this.openrouterKey = openrouterKey;
            this.openrouterModel = openrouterModel;
        }

        @Override public String type() { return "AI"; }

        @Override
        public Map<String, Object> run(Map<String, Object> cfg, Map<String, Object> ctx, Options o) throws Exception {
            String task = s(cfg.getOrDefault("task", "personalize")).toLowerCase();
            String tone = s(cfg.getOrDefault("tone", "friendly"));
            String base = s(cfg.get("prompt"));
            String instruction = switch (task) {
                case "summarize" -> "Summarize the following information in 2-3 clear sentences.";
                case "rewrite" -> "Rewrite the following message in the requested tone, keeping every fact unchanged.";
                default -> "Write a short personalized message for a student using the facts below.";
            };
            String prompt = "You write messages for a college automation system. " + instruction
                    + " Tone: " + tone + ". Plain text only, no markdown, no placeholders, at most 80 words.\n\n" + base;

            List<String> order = switch (provider) {
                case "gemini", "groq", "openrouter", "pollinations" -> List.of(provider);
                default -> List.of("gemini", "groq", "openrouter");
            };
            List<String> errors = new java.util.ArrayList<>();
            for (String p : order) {
                try {
                    String text = call(p, prompt);
                    if (text != null && !text.isBlank()) {
                        Map<String, Object> out = new LinkedHashMap<>();
                        out.put("message", text.trim());
                        out.put("provider", p);
                        return out;
                    }
                    errors.add(p + ": empty answer");
                } catch (Exception e) {
                    errors.add(p + ": " + describe(e, geminiKey, groqKey, openrouterKey));
                }
            }
            throw new IllegalStateException("No AI provider answered (" + String.join("; ", errors)
                    + "). Set GEMINI_API_KEY, GROQ_API_KEY or OPENROUTER_API_KEY, see README.");
        }

        private String call(String p, String prompt) throws Exception {
            return switch (p) {
                case "gemini" -> gemini(prompt);
                case "groq" -> openAiStyle("https://api.groq.com/openai/v1/chat/completions", groqKey, groqModel, prompt, "Groq");
                case "openrouter" -> openAiStyle("https://openrouter.ai/api/v1/chat/completions", openrouterKey, openrouterModel, prompt, "OpenRouter");
                case "pollinations" -> openAiStyle("https://text.pollinations.ai/openai", null, "openai", prompt, "Pollinations");
                default -> throw new IllegalStateException("Unknown AI provider " + p);
            };
        }

        private String gemini(String prompt) throws Exception {
            if (geminiKey == null || geminiKey.isBlank()) throw new IllegalStateException("Gemini key not set");
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key=" + geminiKey;
            String resp = WebClient.create().post().uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))))
                    .retrieve().bodyToMono(String.class).block(TIMEOUT);
            JsonNode n = WorkflowSupport.OM.readTree(resp);
            return n.at("/candidates/0/content/parts/0/text").asText("");
        }

        /** Groq, OpenRouter and Pollinations all speak the OpenAI chat-completions format. */
        private String openAiStyle(String url, String key, String model, String prompt, String name) throws Exception {
            boolean needsKey = !"Pollinations".equals(name);
            if (needsKey && (key == null || key.isBlank())) throw new IllegalStateException(name + " key not set");
            var req = WebClient.create().post().uri(url).contentType(MediaType.APPLICATION_JSON);
            if (key != null && !key.isBlank()) req = req.header("Authorization", "Bearer " + key);
            String resp = req
                    .bodyValue(Map.of("model", model, "messages", List.of(Map.of("role", "user", "content", prompt))))
                    .retrieve().bodyToMono(String.class).block(TIMEOUT);
            JsonNode n = WorkflowSupport.OM.readTree(resp);
            return n.at("/choices/0/message/content").asText("");
        }
    }
}