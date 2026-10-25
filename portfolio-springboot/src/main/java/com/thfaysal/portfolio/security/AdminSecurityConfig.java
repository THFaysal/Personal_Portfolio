package com.thfaysal.portfolio.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;

@Configuration
public class AdminSecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(AdminSecurityConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** The admin account is intentionally DISABLED until owner credentials are configured. */
    @Bean
    public UserDetailsService adminUsers(
            @Value("${app.admin.username:}") String username,
            @Value("${app.admin.password:}") String password,
            @Value("${app.admin.password-hash:}") String passwordHash,
            PasswordEncoder encoder) {
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager();
        if (username.isBlank()) {
            log.warn("Private inbox login is disabled: ADMIN_USERNAME is not configured.");
            return users;
        }
        String credential;
        if (passwordHash.matches("\\$2[aby]\\$\\d\\d\\$[./A-Za-z0-9]{53}")) {
            credential = passwordHash;
        } else if (password.length() >= 16) {
            credential = encoder.encode(password);
        } else {
            log.warn("Private inbox login is disabled: configure ADMIN_PASSWORD with 16+ characters or ADMIN_PASSWORD_HASH (BCrypt).");
            return users;
        }
        users.createUser(User.withUsername(username).password(credential).roles("ADMIN").build());
        return users;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/login", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/", "/assets/**", "/cv/**", "/favicon.ico", "/api/comments", "/contact").permitAll()
                .anyRequest().denyAll())
            // Public APIs do not rely on authenticated sessions; admin operations KEEP CSRF protection.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/contact", "/api/comments"))
            .formLogin(login -> login
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .defaultSuccessUrl("/admin/inbox", true)
                .failureUrl("/admin/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            .sessionManagement(session -> session.sessionFixation(fix -> fix.migrateSession()))
            .headers(headers -> headers.frameOptions(frame -> frame.deny()));
        http.addFilterBefore(new AdminLoginThrottle(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Best-effort brute-force throttle; does not trust visitor-provided forwarding headers. */
    public static class AdminLoginThrottle extends OncePerRequestFilter {
        private final Cache<String, AtomicInteger> attempts = Caffeine.newBuilder()
                .maximumSize(20000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            if (request.getMethod().equals("POST") && request.getServletPath().equals("/admin/login")) {
                String key = request.getRemoteAddr();
                int count = attempts.get(key, ignored -> new AtomicInteger()).incrementAndGet();
                if (count > 15) {
                    response.sendError(429, "Too many login attempts. Please wait before trying again.");
                    return;
                }
            }
            chain.doFilter(request, response);
        }
    }
}
