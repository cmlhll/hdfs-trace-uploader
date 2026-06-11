package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class MetricsConfig {
    private boolean enabled = true;
    private int prometheusPort = 9409;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    public int getPrometheusPort() {
        return prometheusPort;
    }

    public void setPrometheusPort(int prometheusPort) {
        this.prometheusPort = prometheusPort;
    }

    public int prometheusPort() {
        return prometheusPort;
    }

    void validate() {
        if (prometheusPort <= 0 || prometheusPort > 65535) {
            throw new IllegalArgumentException("metrics.prometheusPort must be between 1 and 65535");
        }
    }

    @Override
    public String toString() {
        return "MetricsConfig{" +
                "enabled=" + enabled +
                ", prometheusPort=" + prometheusPort +
                '}';
    }
}
