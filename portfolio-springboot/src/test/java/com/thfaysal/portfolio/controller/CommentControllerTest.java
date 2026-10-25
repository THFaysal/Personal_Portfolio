package com.thfaysal.portfolio.controller;

import com.thfaysal.portfolio.dto.CommentResponse;
import com.thfaysal.portfolio.model.PortfolioComment;
import com.thfaysal.portfolio.service.CommentService;
import com.thfaysal.portfolio.service.NotificationService;
import com.thfaysal.portfolio.service.SubmissionLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {
    @Autowired MockMvc mvc;
    @MockBean CommentService comments;
    @MockBean NotificationService notifications;
    @MockBean SubmissionLimiter limiter;

    @Test
    void getListsRecentComments() throws Exception {
        given(comments.recent()).willReturn(List.of(new CommentResponse("Visitor", "Nice work", Instant.parse("2026-10-18T08:00:00Z"))));
        mvc.perform(get("/api/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Visitor"));
    }

    @Test
    void validPostPersistsThenAttemptsNotification() throws Exception {
        PortfolioComment saved = new PortfolioComment("Visitor", "A helpful comment");
        given(limiter.allow(eq("comment"), anyString())).willReturn(true);
        given(comments.create(any())).willReturn(saved);
        mvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Visitor\",\"message\":\"A helpful comment\",\"website\":\"\"}"))
                .andExpect(status().isCreated());
        verify(notifications).notifyNewComment(saved);
    }

    @Test
    void rejectsInvalidPost() throws Exception {
        mvc.perform(post("/api/comments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"message\":\"no\"}"))
                .andExpect(status().isBadRequest());
    }
}
