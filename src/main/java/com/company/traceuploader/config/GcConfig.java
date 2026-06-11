package com.company.traceuploader.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GcConfig {
    private boolean enabled = true;
    private int delayHoursAfterManifestCommit = 24;
    private boolean deleteDoneMarker = true;
    private boolean moveToCommittedBeforeDelete = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDelayHoursAfterManifestCommit() {
        return delayHoursAfterManifestCommit;
    }

    public void setDelayHoursAfterManifestCommit(int delayHoursAfterManifestCommit) {
        this.delayHoursAfterManifestCommit = delayHoursAfterManifestCommit;
    }

    public boolean isDeleteDoneMarker() {
        return deleteDoneMarker;
    }

    public void setDeleteDoneMarker(boolean deleteDoneMarker) {
        this.deleteDoneMarker = deleteDoneMarker;
    }

    public boolean isMoveToCommittedBeforeDelete() {
        return moveToCommittedBeforeDelete;
    }

    public void setMoveToCommittedBeforeDelete(boolean moveToCommittedBeforeDelete) {
        this.moveToCommittedBeforeDelete = moveToCommittedBeforeDelete;
    }
}
