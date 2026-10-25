package com.thfaysal.portfolio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thfaysal.portfolio.model.PortfolioComment;
import com.thfaysal.portfolio.model.PrivateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final URI RESEND_URL = URI.create("https://api.resend.com/emails");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String recipient;
    private final String sender;

    public NotificationService(ObjectMapper objectMapper,
            @Value("${app.notifications.api-key:}") String apiKey,
            @Value("${app.notifications.to:thfaysal711@gmail.com}") String recipient,
            @Value("${app.notifications.from:onboarding@resend.dev}") String sender) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.recipient = recipient;
        this.sender = sender;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey)
                && StringUtils.hasText(recipient)
                && StringUtils.hasText(sender);
    }

    public void notifyNewComment(PortfolioComment comment) {
        if (!isConfigured()) {
            log.info("Comment saved; email alerts are not configured (RESEND_API_KEY missing).");
            return;
        }
        String message = "A visitor left a comment on your portfolio.\n\n"
                + "Name: " + comment.getName() + "\n"
                + "Comment:\n" + comment.getMessage() + "\n\n"
                + "Received: " + comment.getCreatedAt() + "\n";
        if (!send("New portfolio comment", message, recipient, null)) {
            log.warn("Comment saved, but email notification could not be delivered.");
        }
    }

    /** Alert for a saved private message; failure never removes the saved message. */
    public void notifyNewPrivateMessage(PrivateMessage message) {
        if (!isConfigured()) {
            log.info("Private message saved; Resend alerts are not configured.");
            return;
        }
        String subject = "Private portfolio inquiry: " + safeSubject(message.getSubject());
        String text = "You have a new PRIVATE portfolio message.\n\n"
                + "From: " + message.getName() + "\n"
                + "Email: " + message.getEmail() + "\n"
                + "Subject: " + message.getSubject() + "\n\n"
                + message.getBody() + "\n\nOpen your admin inbox to respond.";
        if (!send(subject, text, recipient, message.getEmail())) {
            log.warn("Private message saved, but the owner alert was not accepted by Resend.");
        }
    }

    /** Acceptances from the Resend HTTPS API, not guarantees of inbox delivery. */
    public boolean sendPrivateReply(PrivateMessage message, String reply) {
        if (!isConfigured()) return false;
        String subject = "Re: " + safeSubject(message.getSubject());
        String text = "Hello " + message.getName() + ",\n\n"
                + reply + "\n\n"
                + "---\n"
                + "Reply to your message sent through Tanvirul Hoque Faysal's portfolio.\n"
                + "If you wish to continue the conversation, reply to this email.";
        return send(subject, text, message.getEmail(), recipient);
    }

    private String safeSubject(String subject) {
        if (subject == null || subject.isBlank()) return "Portfolio inquiry";
        return subject.replaceAll("[\r\n\t]", " ").trim();
    }

    private boolean send(String subject, String body, String to, String replyTo) {
        try {
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("from", sender);
            payload.put("to", List.of(to));
            payload.put("subject", subject);
            payload.put("text", body);
            if (replyTo != null) {
                payload.put("reply_to", replyTo);
            }
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(RESEND_URL)
                    .timeout(Duration.ofSeconds(8))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            }
            log.warn("Email API returned HTTP {}", response.statusCode());
        } catch (JsonProcessingException e) {
            log.warn("Could not prepare email notification.");
        } catch (IOException e) {
            log.warn("Email API network request failed.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Email API request interrupted.");
        }
        return false;
    }
}
