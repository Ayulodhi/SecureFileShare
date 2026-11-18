package com.securefileshare.file;

import java.util.UUID;

public class TestErrorUtil {
    public static String normalizeErrorJson(String json, UUID fileId) {
        if (json == null) return "";
        String normalized = json
                .replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})?", "TIMESTAMP");
        if (fileId != null) {
            normalized = normalized.replace(fileId.toString(), "FILE_ID");
        }
        return normalized;
    }
}
