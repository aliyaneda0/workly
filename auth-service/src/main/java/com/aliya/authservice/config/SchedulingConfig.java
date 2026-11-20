package com.aliya.authservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Turns on @Scheduled method processing. Kept as its own class, same reason as the monolith's
// copy: a @WebMvcTest / @DataJpaTest slice shouldn't drag a scheduler thread pool into every
// test context.
//
// Only user today: RefreshTokenCleanupJob.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
