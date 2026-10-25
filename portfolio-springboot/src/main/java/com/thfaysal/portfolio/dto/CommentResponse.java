package com.thfaysal.portfolio.dto;

import com.thfaysal.portfolio.model.PortfolioComment;

import java.time.Instant;

public record CommentResponse(String name, String message, Instant createdAt) {
    public static CommentResponse from(PortfolioComment comment) {
        return new CommentResponse(comment.getName(), comment.getMessage(), comment.getCreatedAt());
    }
}
