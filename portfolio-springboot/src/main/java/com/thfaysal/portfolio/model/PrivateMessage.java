package com.thfaysal.portfolio.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Private contact messages are never exposed by a public API. */
@Document(collection = "private_messages")
public class PrivateMessage {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern("dd MMM yyyy, HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    @Id
    private String id;
    private String name;
    private String email;
    private String subject;
    private String body;
    private Instant createdAt = Instant.now();
    private Instant readAt;
    private List<EmailReply> replies = new ArrayList<>();

    protected PrivateMessage() { }

    public PrivateMessage(String name, String email, String subject, String body) {
        this.name = name;
        this.email = email;
        this.subject = subject;
        this.body = body;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedAtLabel() { return TIME_FORMAT.format(createdAt); }
    public Instant getReadAt() { return readAt; }
    public boolean isUnread() { return readAt == null; }
    public List<EmailReply> getReplies() { return replies == null ? List.of() : replies; }
    public boolean isReplied() { return !getReplies().isEmpty(); }

    public void markRead() {
        if (readAt == null) readAt = Instant.now();
    }

    public void recordReply(String content) {
        if (replies == null) replies = new ArrayList<>();
        replies.add(new EmailReply(content, Instant.now()));
        markRead();
    }

    /** Text and timestamp of a reply accepted by the email provider. */
    public static class EmailReply {
        private String body;
        private Instant sentAt;

        protected EmailReply() { }
        public EmailReply(String body, Instant sentAt) {
            this.body = body;
            this.sentAt = sentAt;
        }
        public String getBody() { return body; }
        public Instant getSentAt() { return sentAt; }
        public String getSentAtLabel() { return TIME_FORMAT.format(sentAt); }
    }
}
