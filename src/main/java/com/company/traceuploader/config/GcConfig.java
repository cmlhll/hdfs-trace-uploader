package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class GcConfig {
    private boolean enabled = true;
    private long delayHoursAfterManifestCommit = 24;
    private boolean deleteDoneMarker = true;
    private boolean moveToCommittedBeforeDelete = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    public long getDelayHoursAfterManifestCommit() {
        return delayHoursAfterManifestCommit;
    }

    public void setDelayHoursAfterManifestCommit(long delayHoursAfterManifestCommit) {
        this.delayHoursAfterManifestCommit = delayHoursAfterManifestCommit;
    }

    public long delayHoursAfterManifestCommit() {
        return delayHoursAfterManifestCommit;
    }

    public boolean isDeleteDoneMarker() {
        return deleteDoneMarker;
    }

    public void setDeleteDoneMarker(boolean deleteDoneMarker) {
        this.deleteDoneMarker = deleteDoneMarker;
    }

    public boolean deleteDoneMarker() {
        return deleteDoneMarker;
    }

    public boolean isMoveToCommittedBeforeDelete() {
        return moveToCommittedBeforeDelete;
    }

    public void setMoveToCommittedBeforeDelete(boolean moveToCommittedBeforeDelete) {
        this.moveToCommittedBeforeDelete = moveToCommittedBeforeDelete;
    }

    public boolean moveToCommittedBeforeDelete() {
        return moveToCommittedBeforeDelete;
    }

    void validate() {
        if (delayHoursAfterManifestCommit < 0) {
            throw new IllegalArgumentException("gc.delayHoursAfterManifestCommit must be >= 0");
        }
    }

    @Override
    public String toString() {
        return "GcConfig{" +
                "enabled=" + enabled +
                ", delayHoursAfterManifestCommit=" + delayHoursAfterManifestCommit +
                ", deleteDoneMarker=" + deleteDoneMarker +
                ", moveToCommittedBeforeDelete=" + moveToCommittedBeforeDelete +
                '}';
    }
}
