package com.cybershield.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Loads the demonstration accounts and sample records after start-up.
 * Active ONLY in the "dev" profile, and only when cybershield.demo-data.enabled=true,
 * so it can never run in production.
 */
@Component
@Profile("dev")
public class DemoDataInitializer implements ApplicationRunner {

    private final DemoDataService demoDataService;
    private final boolean enabled;

    public DemoDataInitializer(DemoDataService demoDataService,
                               @Value("${cybershield.demo-data.enabled:false}") boolean enabled) {
        this.demoDataService = demoDataService;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (enabled) {
            demoDataService.seed();
        }
    }
}
