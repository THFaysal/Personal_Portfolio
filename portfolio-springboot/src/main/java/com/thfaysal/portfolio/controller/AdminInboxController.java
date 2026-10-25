package com.thfaysal.portfolio.controller;

import com.thfaysal.portfolio.dto.AdminReplyForm;
import com.thfaysal.portfolio.model.PrivateMessage;
import com.thfaysal.portfolio.service.PrivateInboxService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AdminInboxController {
    private final PrivateInboxService inbox;

    public AdminInboxController(PrivateInboxService inbox) {
        this.inbox = inbox;
    }

    private void preventCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Robots-Tag", "noindex, nofollow, noarchive");
        response.setHeader("Referrer-Policy", "no-referrer");
    }

    @GetMapping("/admin/login")
    public String login(HttpServletResponse response) {
        preventCaching(response);
        return "admin/login";
    }

    @GetMapping("/admin")
    public String home() { return "redirect:/admin/inbox"; }

    @GetMapping("/admin/inbox")
    public String list(@RequestParam(defaultValue = "0") int page, Model model, HttpServletResponse response) {
        preventCaching(response);
        Page<PrivateMessage> messages = inbox.page(page);
        model.addAttribute("messages", messages);
        model.addAttribute("unread", inbox.unreadCount());
        return "admin/inbox";
    }

    @GetMapping("/admin/inbox/{id}")
    public String detail(@PathVariable String id, Model model, HttpServletResponse response) {
        preventCaching(response);
        Optional<PrivateMessage> message = inbox.get(id);
        if (message.isEmpty()) return "redirect:/admin/inbox?missing";
        model.addAttribute("message", message.get());
        model.addAttribute("replyForm", new AdminReplyForm());
        return "admin/message";
    }

    @PostMapping("/admin/inbox/{id}/read")
    public String read(@PathVariable String id, RedirectAttributes redirect) {
        if (inbox.markRead(id)) redirect.addFlashAttribute("notice", "Message marked as read.");
        return "redirect:/admin/inbox/" + id;
    }

    @PostMapping("/admin/inbox/{id}/reply")
    public String reply(@PathVariable String id, @Valid @ModelAttribute("replyForm") AdminReplyForm form,
                        BindingResult validation, Model model, RedirectAttributes redirect,
                        HttpServletResponse response) {
        preventCaching(response);
        Optional<PrivateMessage> message = inbox.get(id);
        if (message.isEmpty()) return "redirect:/admin/inbox?missing";
        if (validation.hasErrors()) {
            model.addAttribute("message", message.get());
            return "admin/message";
        }
        PrivateInboxService.ReplyResult result = inbox.reply(id, form.getMessage());
        if (result == PrivateInboxService.ReplyResult.SENT) {
            redirect.addFlashAttribute("notice", "Resend accepted your reply. Delivery may take a moment.");
        } else if (result == PrivateInboxService.ReplyResult.EMAIL_UNAVAILABLE) {
            redirect.addFlashAttribute("error", "Could not send email. Check your Resend settings and verified sender domain.");
        } else {
            return "redirect:/admin/inbox?missing";
        }
        return "redirect:/admin/inbox/" + id;
    }
}
