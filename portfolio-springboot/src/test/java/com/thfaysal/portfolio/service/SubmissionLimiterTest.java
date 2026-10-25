package com.thfaysal.portfolio.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SubmissionLimiterTest {
    @Test
    void cooldownIsPerActionAndAddress() {
        SubmissionLimiter limiter = new SubmissionLimiter();
        assertTrue(limiter.allow("comment", "192.0.2.1"));
        assertFalse(limiter.allow("comment", "192.0.2.1"));
        assertTrue(limiter.allow("contact", "192.0.2.1"));
        assertTrue(limiter.allow("comment", "192.0.2.2"));
    }
}
