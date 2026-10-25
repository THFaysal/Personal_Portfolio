package com.thfaysal.portfolio.controller;

import com.thfaysal.portfolio.service.PrivateInboxService;
import com.thfaysal.portfolio.service.SubmissionLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PortfolioController.class)
@AutoConfigureMockMvc(addFilters = false)
class PortfolioControllerTest {
    @Autowired MockMvc mvc;
    @MockBean PrivateInboxService inbox;
    @MockBean SubmissionLimiter limiter;

    @Test
    void rendersPortfolio() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void acceptedContactIsSavedToPrivateInbox() throws Exception {
        given(limiter.allow(eq("contact"), anyString())).willReturn(true);
        mvc.perform(post("/contact")
                        .param("name", "Visitor")
                        .param("email", "visitor@example.com")
                        .param("subject", "Question")
                        .param("message", "Can I see your network topology?"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value(org.hamcrest.Matchers.containsString("saved")));
        org.mockito.Mockito.verify(inbox).receive(any());
    }
}
