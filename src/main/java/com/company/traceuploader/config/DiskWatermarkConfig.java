package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DiskWatermarkConfig {
    private int warnPercent = 70;
    private int errorPercent = 85;
    private int criticalPercent = 92;

    public int getWarnPercent() {
        return warnPercent;
    }

    public void setWarnPercent(int warnPercent) {
        this.warnPercent = warnPercent;
    }

    public int getErrorPercent() {
        return errorPercent;
    }

    public void setErrorPercent(int errorPercent) {
        this.errorPercent = errorPercent;
    }

    public int getCriticalPercent() {
        return criticalPercent;
    }

    public void setCriticalPercent(int criticalPercent) {
        this.criticalPercent = criticalPercent;
    }
}
