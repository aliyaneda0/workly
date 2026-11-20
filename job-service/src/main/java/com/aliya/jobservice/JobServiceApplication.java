package com.aliya.jobservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Same exclusion as company-service, same reason: no UserDetailsService-based auth of any kind
// here (stateless JWT-only), so the default "generate an in-memory login user" autoconfiguration
// has nothing to do and would otherwise print a confusing generated-password warning on boot.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class JobServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobServiceApplication.class, args);
    }
}
