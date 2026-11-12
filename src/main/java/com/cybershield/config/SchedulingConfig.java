package com.cybershield.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables @Scheduled jobs unless cybershield.scheduler.enabled=false (test profile). */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "cybershield.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
