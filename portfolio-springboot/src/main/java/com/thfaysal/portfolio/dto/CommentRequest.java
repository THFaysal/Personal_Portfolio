package com.thfaysal.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "Your name is required")
        @Size(max = 80, message = "Name must be 80 characters or fewer") String name,
        @NotBlank(message = "Please write a comment")
        @Size(min = 5, max = 800, message = "Comment must be 5-800 characters") String message,
        String website
) {
}
