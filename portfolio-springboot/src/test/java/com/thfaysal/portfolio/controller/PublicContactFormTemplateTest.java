package com.thfaysal.portfolio.controller;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicContactFormTemplateTest {
    @Test
    void publicContactFormUsesPlainHtmlActionWithoutDeferredCsrfTokenLookup() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/templates/index.html")) {
            assertNotNull(input, "Homepage template must be on the classpath");
            String html = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = Pattern.compile("<form\\s+[^>]*id=\"contact-form\"[^>]*>").matcher(html);
            assertTrue(matcher.find(), "Public contact form must exist");
            String form = matcher.group();
            assertTrue(form.contains("action=\"/contact\""));
            assertFalse(form.contains("th:action="),
                    "Public contact form is CSRF-exempt; th:action triggers late session creation");
        }
    }
}
