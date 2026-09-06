package com.aliya.workly.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Turns on @Scheduled method processing app-wide. Kept as its own class (rather than an
// annotation on WorklyApplication) so a @WebMvcTest / @DataJpaTest slice doesn't drag a
// scheduler thread pool into every test context.
//
// Only user today: RefreshTokenCleanupJob.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
