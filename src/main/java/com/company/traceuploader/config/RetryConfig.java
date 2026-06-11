package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class RetryConfig {
    private int maxAttempts = 20;
    private long initialBackoffSeconds = 5;
    private long maxBackoffSeconds = 600;
    private double backoffMultiplier = 2.0;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public long getInitialBackoffSeconds() {
        return initialBackoffSeconds;
    }

    public void setInitialBackoffSeconds(long initialBackoffSeconds) {
        this.initialBackoffSeconds = initialBackoffSeconds;
    }

    public long initialBackoffSeconds() {
        return initialBackoffSeconds;
    }

    public long getMaxBackoffSeconds() {
        return maxBackoffSeconds;
    }

    public void setMaxBackoffSeconds(long maxBackoffSeconds) {
        this.maxBackoffSeconds = maxBackoffSeconds;
    }

    public long maxBackoffSeconds() {
        return maxBackoffSeconds;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public void setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }

    public double backoffMultiplier() {
        return backoffMultiplier;
    }

    void validate() {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("retry.maxAttempts must be > 0");
        }
        if (initialBackoffSeconds < 0) {
            throw new IllegalArgumentException("retry.initialBackoffSeconds must be >= 0");
        }
        if (maxBackoffSeconds < initialBackoffSeconds) {
            throw new IllegalArgumentException("retry.maxBackoffSeconds must be >= retry.initialBackoffSeconds");
        }
        if (backoffMultiplier < 1.0) {
            throw new IllegalArgumentException("retry.backoffMultiplier must be >= 1.0");
        }
    }

    @Override
    public String toString() {
        return "RetryConfig{" +
                "maxAttempts=" + maxAttempts +
                ", initialBackoffSeconds=" + initialBackoffSeconds +
                ", maxBackoffSeconds=" + maxBackoffSeconds +
                ", backoffMultiplier=" + backoffMultiplier +
                '}';
    }
}
