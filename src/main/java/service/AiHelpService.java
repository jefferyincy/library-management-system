package service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

@Service
public class AiHelpService {
    private static final Logger log = LoggerFactory.getLogger(AiHelpService.class);

    private static final String SYSTEM_PROMPT = """
            You are a friendly, concise help assistant for a small web-based library management system.
            Rules you must follow:
            - Students browse a catalog, copy a Book ID (UUID), use "Borrow" to check out and "Return" to check in.
            - Loan period is 14 days from borrow date; late fee is INR 5 per day after the due date (not legal advice).
            - Email confirmations on borrow only work if the server has SMTP configured.
            - Roles: STUDENT (borrow/return), EMPLOYEE (desk read-only views), ADMIN (manage books, users, bans).
            - Suspended accounts cannot log in or borrow.
            - Keep answers short (under 120 words unless the user asks for detail). No markdown headings.
            - If asked something unrelated to this app, politely redirect to library app topics.
            """;

    private final String openAiApiKey;
    private final String openAiModel;
    private final String openAiOrganizationId;
    private final String openAiProjectId;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public AiHelpService(
            @Value("${library.ai.openai.api-key:}") String openAiApiKey,
            @Value("${library.ai.openai.model:gpt-4o-mini}") String openAiModel,
            @Value("${library.ai.openai.organization-id:}") String openAiOrganizationId,
            @Value("${library.ai.openai.project-id:}") String openAiProjectId) {
        this.openAiApiKey = normalizeApiKey(openAiApiKey);
        this.openAiModel = openAiModel != null && !openAiModel.isBlank() ? openAiModel.trim() : "gpt-4o-mini";
        this.openAiOrganizationId = openAiOrganizationId != null ? openAiOrganizationId.trim() : "";
        this.openAiProjectId = openAiProjectId != null ? openAiProjectId.trim() : "";
    }

    @PostConstruct
    void logAiMode() {
        if (isAiEnabled()) {
            log.info("Library help chat: OpenAI enabled (model={}).", openAiModel);
        } else {
            log.info("Library help chat: built-in FAQ only (set OPENAI_API_KEY or library.ai.openai.api-key for OpenAI).");
        }
    }

    private static String normalizeApiKey(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    public boolean isAiEnabled() {
        return !openAiApiKey.isEmpty();
    }

    public String reply(String userRole, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "Ask me anything about using the library portal (borrowing, returns, fines, roles…).";
        }
        if (isAiEnabled()) {
            try {
                return callOpenAi(userMessage);
            } catch (Exception e) {
                log.warn("OpenAI request failed: {}", e.getMessage(), e);
                String explained = explainOpenAiFailure(e.getMessage());
                if (explained != null) {
                    return explained + "\n\n" + fallbackReply(userMessage);
                }
                return fallbackReply(userMessage)
                        + "\n\n(Full AI is temporarily unavailable; showing built-in help instead.)";
            }
        }
        return fallbackReply(userMessage);
    }

    /**
     * OpenAI error JSON is embedded in IllegalStateException message after "HTTP status: ".
     */
    private static String explainOpenAiFailure(String message) {
        if (message == null) {
            return null;
        }
        if (message.contains("insufficient_quota")) {
            return "AI answers are off because this OpenAI account has no API quota left (billing or free-tier limits). "
                    + "Add credits or a payment method at https://platform.openai.com/account/billing — the key is loading correctly.";
        }
        if (message.contains("invalid_api_key") || message.contains("Incorrect API key")) {
            return "AI answers are off because OpenAI rejected the API key (revoked, typo, or wrong project). Check the key in application-local.properties or OPENAI_API_KEY.";
        }
        if (message.contains("rate_limit") || message.contains("Rate limit")) {
            return "AI answers are off temporarily due to OpenAI rate limits. Try again in a minute.";
        }
        return null;
    }

    private String callOpenAi(String userMessage) throws Exception {
        Map<String, Object> body = Map.of(
                "model", openAiModel,
                "messages", java.util.List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", 400,
                "temperature", 0.6
        );
        String json = objectMapper.writeValueAsString(body);
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openAiApiKey);
        if (!openAiOrganizationId.isEmpty()) {
            req.header("OpenAI-Organization", openAiOrganizationId);
        }
        if (!openAiProjectId.isEmpty()) {
            req.header("OpenAI-Project", openAiProjectId);
        }
        HttpRequest request = req.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String bodySnippet = response.body();
            if (bodySnippet != null && bodySnippet.length() > 800) {
                bodySnippet = bodySnippet.substring(0, 800) + "…";
            }
            log.warn("OpenAI HTTP {} — {}", response.statusCode(), bodySnippet);
            throw new IllegalStateException("HTTP " + response.statusCode() + ": " + response.body());
        }
        JsonNode root = objectMapper.readTree(response.body());
        String text = root.path("choices").path(0).path("message").path("content").asText("");
        return text.isBlank() ? fallbackReply(userMessage) : text.trim();
    }

    private String fallbackReply(String raw) {
        String m = raw.toLowerCase(Locale.ROOT);
        if (m.contains("borrow") || m.contains("issue") || m.contains("checkout") || m.contains("lend")) {
            return "To borrow: find the book in the catalog, copy its Book ID (the long ID in the first column), paste it into “Borrow — book ID”, and click Issue. You must be logged in as a student and the book must have available copies.";
        }
        if (m.contains("return")) {
            return "To return: use the same Book ID you borrowed with, paste it under “Return — book ID”, and click Return. Fines apply if you are past the due date (INR 5 per day after due).";
        }
        if (m.contains("fine") || m.contains("late") || m.contains("overdue")) {
            return "Late returns: the app charges INR 5 per day after your due date. The due date is 14 days after you borrow. You’ll see the fine amount when you return.";
        }
        if (m.contains("due") || m.contains("how long") || m.contains("14")) {
            return "Books are due 14 days after the borrow date. Check “Your issue history” for Issue date and Due date.";
        }
        if (m.contains("email") || m.contains("mail")) {
            return "If the library configured email on the server, you may get a confirmation when you borrow. If not, borrowing still works—there just won’t be an email.";
        }
        if (m.contains("employee") || m.contains("staff") || m.contains("admin")) {
            return "Students borrow and return. Employees can view catalog, users, and loans (read-only). Admins add books, manage user roles, suspend accounts, and remove users.";
        }
        if (m.contains("ban") || m.contains("susp") || m.contains("lock")) {
            return "Suspended accounts cannot log in or borrow. Contact an administrator if you think this is a mistake.";
        }
        if (m.contains("id") && (m.contains("book") || m.contains("where"))) {
            return "Each book row shows a Book ID (UUID). Click or select that text and copy it—you need that exact ID for Borrow and Return.";
        }
        return "I can help with this library website: borrowing, returning, due dates, fines, roles, and emails. "
                + "Try: “How do I borrow?” or “What are fines?” "
                + "For smarter answers, your administrator can set library.ai.openai.api-key in application.properties (OpenAI API key).";
    }
}
