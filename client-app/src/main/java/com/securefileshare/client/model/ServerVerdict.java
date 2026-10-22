package com.securefileshare.client.model;

/**
 * Server verification verdict for an uploaded encrypted package.
 * WHAT: Enumerates CLEAN (accepted into vault), SUSPICIOUS (quarantined), and INFECTED (rejected).
 * WHY: Governs Phase C3 upload outcome handling and status chip styling.
 */
public enum ServerVerdict {
    CLEAN("Clean", "File accepted and verified successfully."),
    SUSPICIOUS("Quarantined for review", "Heuristics flagged unusual structure; stored quarantined."),
    INFECTED("Infected", "Server integrity inspection detected malware; upload rejected.");

    private final String title;
    private final String message;

    ServerVerdict(String title, String message) {
        this.title = title;
        this.message = message;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }
}
