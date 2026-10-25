package com.thfaysal.portfolio.controller;

import com.thfaysal.portfolio.dto.CommentRequest;
import com.thfaysal.portfolio.dto.CommentResponse;
import com.thfaysal.portfolio.model.PortfolioComment;
import com.thfaysal.portfolio.service.CommentService;
import com.thfaysal.portfolio.service.NotificationService;
import com.thfaysal.portfolio.service.SubmissionLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService comments;
    private final NotificationService notifications;
    private final SubmissionLimiter limiter;

    public CommentController(CommentService comments, NotificationService notifications,
                             SubmissionLimiter limiter) {
        this.comments = comments;
        this.notifications = notifications;
        this.limiter = limiter;
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> latest() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(comments.recent());
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> create(@Valid @RequestBody CommentRequest request,
                                                       HttpServletRequest httpRequest) {
        // Honeypot: genuine users do not see this field. Don't persist automated submissions.
        if (request.website() != null && !request.website().isBlank()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Thanks for your feedback."));
        }
        if (!limiter.allow("comment", httpRequest.getRemoteAddr())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Please wait a minute before commenting again."));
        }
        PortfolioComment saved = comments.create(request);
        notifications.notifyNewComment(saved);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Thanks! Your comment has been posted."));
    }
}
