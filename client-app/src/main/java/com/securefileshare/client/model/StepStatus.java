package com.securefileshare.client.model;

/**
 * Execution state for an upload workflow step.
 * WHAT: Enumerates WAITING, IN_PROGRESS, DONE, and FAILED.
 * WHY: Governs status chip rendering: Done = green chip, In progress = amber chip, Waiting = muted text with no chip.
 */
public enum StepStatus {
    WAITING("Waiting"),
    IN_PROGRESS("In progress"),
    DONE("Done"),
    FAILED("Threat Detected");

    private final String label;

    StepStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
