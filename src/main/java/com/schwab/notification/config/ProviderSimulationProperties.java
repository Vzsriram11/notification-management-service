package com.schwab.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds to notification.simulation.* in application.yml — lets you dial a
 * channel's failure/timeout rate up or down to deterministically exercise
 * each branch of the retry logic instead of hoping a real failure occurs.
 */
@ConfigurationProperties(prefix = "notification.simulation")
public class ProviderSimulationProperties {
    private double failureRate = 0.0;
    private double timeoutRate = 0.0;

    public double getFailureRate() { return failureRate; }
    public void setFailureRate(double failureRate) { this.failureRate = failureRate; }
    public double getTimeoutRate() { return timeoutRate; }
    public void setTimeoutRate(double timeoutRate) { this.timeoutRate = timeoutRate; }
}
