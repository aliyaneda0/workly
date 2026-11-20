package com.aliya.companyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Excludes the default "generate an in-memory login user" autoconfiguration — this service is
// stateless JWT-only (no form login, no UserDetailsService), so that default has nothing to do
// and would otherwise print a confusing generated-password warning on every boot. The monolith
// doesn't need this exclusion because pulling in spring-boot-starter-oauth2-client there already
// suppresses it.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CompanyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompanyServiceApplication.class, args);
    }
}
