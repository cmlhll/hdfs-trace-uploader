package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class DiskWatermarkConfig {
    private int warnPercent = 70;
    private int errorPercent = 85;
    private int criticalPercent = 92;

    public int getWarnPercent() {
        return warnPercent;
    }

    public void setWarnPercent(int warnPercent) {
        this.warnPercent = warnPercent;
    }

    public int warnPercent() {
        return warnPercent;
    }

    public int getErrorPercent() {
        return errorPercent;
    }

    public void setErrorPercent(int errorPercent) {
        this.errorPercent = errorPercent;
    }

    public int errorPercent() {
        return errorPercent;
    }

    public int getCriticalPercent() {
        return criticalPercent;
    }

    public void setCriticalPercent(int criticalPercent) {
        this.criticalPercent = criticalPercent;
    }

    public int criticalPercent() {
        return criticalPercent;
    }

    void validate() {
        validatePercent("diskWatermark.warnPercent", warnPercent);
        validatePercent("diskWatermark.errorPercent", errorPercent);
        validatePercent("diskWatermark.criticalPercent", criticalPercent);
        if (!(warnPercent <= errorPercent && errorPercent <= criticalPercent)) {
            throw new IllegalArgumentException("diskWatermark values must be ordered warn <= error <= critical");
        }
    }

    private static void validatePercent(String name, int value) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(name + " must be between 0 and 100");
        }
    }

    @Override
    public String toString() {
        return "DiskWatermarkConfig{" +
                "warnPercent=" + warnPercent +
                ", errorPercent=" + errorPercent +
                ", criticalPercent=" + criticalPercent +
                '}';
    }
}
