package com.thfaysal.portfolio.cms;

import com.thfaysal.portfolio.security.AdminSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AdminContentController.class, PublicMediaController.class})
@Import(AdminSecurityConfig.class)
@TestPropertySource(properties = {"app.admin.username=owner", "app.admin.password=LongPasswordForTesting123!"})
class PortfolioContentSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean PortfolioContentService content;
    @MockBean MediaStore media;

    @Test void anonymousUserCannotOpenManager() throws Exception {
        mvc.perform(get("/admin/content"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login"));
    }
    @Test void regularUserCannotDeleteContent() throws Exception {
        mvc.perform(post("/admin/content/seed-sdn/delete").with(user("reader").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
    }
    @Test void adminMustUseCsrfForDestructiveAction() throws Exception {
        mvc.perform(post("/admin/content/seed-sdn/delete").with(user("owner").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
    @Test void mediaCannotBeDownloadedUnlessPublished() throws Exception {
        mvc.perform(get("/media/507f191e810c19729de860ea")).andExpect(status().isNotFound());
    }
}
