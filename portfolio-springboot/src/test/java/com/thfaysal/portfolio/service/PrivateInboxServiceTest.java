package com.thfaysal.portfolio.service;

import com.thfaysal.portfolio.dto.ContactForm;
import com.thfaysal.portfolio.model.PrivateMessage;
import com.thfaysal.portfolio.repository.PrivateMessageRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PrivateInboxServiceTest {
    PrivateMessageRepository repository = mock(PrivateMessageRepository.class);
    NotificationService notifier = mock(NotificationService.class);
    PrivateInboxService inbox = new PrivateInboxService(repository, notifier);

    @Test
    void storesContactBeforeAttemptingEmailAlert() {
        ContactForm form = new ContactForm();
        form.setName(" Visitor ");
        form.setEmail(" visitor@example.com ");
        form.setSubject(" Job inquiry ");
        form.setMessage(" Hello, may I contact you? ");
        when(repository.save(any(PrivateMessage.class))).thenAnswer(call -> call.getArgument(0));
        PrivateMessage saved = inbox.receive(form);
        assertEquals("Visitor", saved.getName());
        assertEquals("visitor@example.com", saved.getEmail());
        assertEquals("Hello, may I contact you?", saved.getBody());
        var sequence = inOrder(repository, notifier);
        sequence.verify(repository).save(any());
        sequence.verify(notifier).notifyNewPrivateMessage(saved);
    }

    @Test
    void successfulReplyIsRecorded() {
        PrivateMessage saved = new PrivateMessage("Visitor", "visitor@example.com", "Question", "Hello");
        when(repository.findById("id1")).thenReturn(Optional.of(saved));
        when(notifier.sendPrivateReply(saved, "Thanks for writing.")).thenReturn(true);
        assertEquals(PrivateInboxService.ReplyResult.SENT, inbox.reply("id1", " Thanks for writing. "));
        assertTrue(saved.isReplied());
        assertFalse(saved.isUnread());
        verify(repository).save(saved);
    }

    @Test
    void rejectedEmailDoesNotCreateFalseReplyHistory() {
        PrivateMessage saved = new PrivateMessage("Visitor", "visitor@example.com", "Question", "Hello");
        when(repository.findById("id1")).thenReturn(Optional.of(saved));
        assertEquals(PrivateInboxService.ReplyResult.EMAIL_UNAVAILABLE, inbox.reply("id1", "Thanks for writing."));
        assertFalse(saved.isReplied());
        verify(repository, never()).save(saved);
    }
}
