package com.thfaysal.portfolio.service;

import com.thfaysal.portfolio.dto.ContactForm;
import com.thfaysal.portfolio.model.PrivateMessage;
import com.thfaysal.portfolio.repository.PrivateMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class PrivateInboxService {
    private final PrivateMessageRepository repository;
    private final NotificationService notificationService;

    public PrivateInboxService(PrivateMessageRepository repository, NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public PrivateMessage receive(ContactForm form) {
        String subject = StringUtils.hasText(form.getSubject())
                ? form.getSubject().trim() : "Portfolio inquiry";
        PrivateMessage saved = repository.save(new PrivateMessage(
                form.getName().trim(), form.getEmail().trim(), subject, form.getMessage().trim()));
        // Email is best-effort. A working MongoDB inbox must not depend on the mail API.
        notificationService.notifyNewPrivateMessage(saved);
        return saved;
    }

    public Page<PrivateMessage> page(int page) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(0, Math.min(page, 10000)), 10));
    }

    public long unreadCount() {
        return repository.countByReadAtIsNull();
    }

    public Optional<PrivateMessage> get(String id) {
        return repository.findById(id);
    }

    public boolean markRead(String id) {
        Optional<PrivateMessage> record = repository.findById(id);
        if (record.isEmpty()) return false;
        PrivateMessage message = record.get();
        message.markRead();
        repository.save(message);
        return true;
    }

    /** Records a reply only if Resend accepts the outbound email request. */
    public ReplyResult reply(String id, String content) {
        Optional<PrivateMessage> record = repository.findById(id);
        if (record.isEmpty()) return ReplyResult.NOT_FOUND;
        PrivateMessage message = record.get();
        String text = content.trim();
        if (!notificationService.sendPrivateReply(message, text)) return ReplyResult.EMAIL_UNAVAILABLE;
        message.recordReply(text);
        repository.save(message);
        return ReplyResult.SENT;
    }

    public enum ReplyResult { SENT, NOT_FOUND, EMAIL_UNAVAILABLE }
}
