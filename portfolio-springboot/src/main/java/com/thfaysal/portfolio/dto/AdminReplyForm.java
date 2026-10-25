package com.thfaysal.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminReplyForm {
    @NotBlank(message = "Enter a reply before sending.")
    @Size(min = 5, max = 3500, message = "Reply must contain 5 to 3500 characters.")
    private String message;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
