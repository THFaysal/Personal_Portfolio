package com.thfaysal.portfolio.security;

import com.thfaysal.portfolio.controller.AdminInboxController;
import com.thfaysal.portfolio.service.PrivateInboxService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;

@WebMvcTest(AdminInboxController.class)
@Import(AdminSecurityConfig.class)
@TestPropertySource(properties = {
        "app.admin.username=owner",
        "app.admin.password=LongUniqueTestPassword123!",
        "app.admin.password-hash="
})
class AdminInboxSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean PrivateInboxService inbox;

    @Test
    void anonymousCannotReadInbox() throws Exception {
        mvc.perform(get("/admin/inbox"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login"));
    }

    @Test
    void correctCredentialsCanSignIn() throws Exception {
        mvc.perform(formLogin("/admin/login").user("owner").password("LongUniqueTestPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withRoles("ADMIN"));
    }

    @Test
    void wrongCredentialsCannotSignIn() throws Exception {
        mvc.perform(formLogin("/admin/login").user("owner").password("wrongpassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login?error"));
    }

    @Test
    void viewerCannotUseAdminActions() throws Exception {
        mvc.perform(post("/admin/inbox/abc/read")
                        .with(user("visitor").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminActionRequiresCsrfToken() throws Exception {
        mvc.perform(post("/admin/inbox/abc/read").with(user("owner").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void authorizedAndCsrfProtectedReadActionWorks() throws Exception {
        mvc.perform(post("/admin/inbox/abc/read")
                        .with(user("owner").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
