package com.aliya.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// No UserDetailsServiceAutoConfiguration exclusion here, unlike the other extracted services:
// this one has real authentication (local login + OAuth2), so it needs Spring Security's
// normal machinery. Same as the monolith's WorklyApplication — the oauth2-client dependency
// already supplies a ClientRegistrationRepository bean that suppresses the generated-password
// warning (see DECISIONS.md ADR-009's note on this).
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
