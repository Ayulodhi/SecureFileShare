package com.securefileshare.client.model;

/**
 * State and outcome of a security verification check.
 * WHAT: Captures state (WAITING, IN_PROGRESS, PASSED, FAILED) and failure detail strings.
 * WHY: Governs status chip rendering: Passed = green chip ("Valid", "Done", etc.),
 *      In progress = amber chip, Failed = red chip with failure reason.
 */
public record CheckState(
        Status status,
        String chipLabel,
        String failureReason
) {
    public enum Status {
        WAITING,
        IN_PROGRESS,
        PASSED,
        FAILED
    }

    public static CheckState waiting() {
        return new CheckState(Status.WAITING, "Waiting", null);
    }

    public static CheckState inProgress() {
        return new CheckState(Status.IN_PROGRESS, "In progress", null);
    }

    public static CheckState passed(String label) {
        return new CheckState(Status.PASSED, label, null);
    }

    public static CheckState failed(String failureReason) {
        return new CheckState(Status.FAILED, "Failed", failureReason);
    }
}
