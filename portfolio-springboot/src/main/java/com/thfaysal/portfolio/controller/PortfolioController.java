package com.thfaysal.portfolio.controller;

import com.thfaysal.portfolio.dto.ContactForm;
import com.thfaysal.portfolio.cms.PortfolioContentService;
import com.thfaysal.portfolio.service.PrivateInboxService;
import com.thfaysal.portfolio.service.SubmissionLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class PortfolioController {
    private static final Logger log = LoggerFactory.getLogger(PortfolioController.class);
    private final PrivateInboxService inbox;
    private final SubmissionLimiter limiter;
    private final PortfolioContentService content;

    public PortfolioController(PrivateInboxService inbox, SubmissionLimiter limiter, PortfolioContentService content) {
        this.inbox = inbox;
        this.limiter = limiter;
        this.content = content;
    }

    @GetMapping("/")
    public String showPortfolio(Model model) {
        model.addAttribute("portfolio", content.publicView());
        return "index";
    }

    @PostMapping("/contact")
    @ResponseBody
    public ResponseEntity<Map<String, String>> submitContact(
            @Valid @ModelAttribute ContactForm contactForm,
            BindingResult validation,
            @RequestParam(value = "website", required = false) String website,
            HttpServletRequest httpRequest) {
        if (validation.hasErrors()) {
            String error = validation.getFieldError() == null
                    ? "Please check the form and try again."
                    : validation.getFieldError().getDefaultMessage();
            return ResponseEntity.badRequest().body(Map.of("message", error));
        }
        if (website != null && !website.isBlank()) {
            return ResponseEntity.ok(Map.of("message", "Thanks for reaching out."));
        }
        if (!limiter.allow("contact", httpRequest.getRemoteAddr())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Please wait a minute before trying again."));
        }
        try {
            inbox.receive(contactForm);
        } catch (DataAccessException exception) {
            log.warn("Private message could not be saved to MongoDB.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message",
                    "The private inbox is temporarily unavailable. Please use the direct email link."));
        }
        return ResponseEntity.ok(Map.of("message", "Your private message was saved. I'll reply by email."));
    }
}
