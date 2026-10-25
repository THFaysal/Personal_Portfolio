package com.thfaysal.portfolio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ContactForm {
    @NotBlank(message = "Name is required")
    @Size(max = 80, message = "Name is too long")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    @Size(max = 200, message = "Email is too long")
    @Pattern(regexp = "^[^\\r\\n]+$", message = "Enter a valid email")
    private String email;

    @Size(max = 200, message = "Subject is too long")
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(min = 10, max = 1500, message = "Message must be between 10 and 1500 characters")
    private String message;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
