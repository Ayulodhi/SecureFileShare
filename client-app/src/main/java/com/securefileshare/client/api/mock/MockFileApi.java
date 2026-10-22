package com.securefileshare.client.api.mock;

import com.securefileshare.client.api.FileApi;
import com.securefileshare.client.model.FileDto;
import com.securefileshare.client.model.ScanStatus;
import com.securefileshare.client.model.SharedFileDto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mock implementation of FileApi with realistic sample data and artificial latency.
 * WHAT: Simulates local and shared encrypted files with Clean and Quarantined statuses.
 * WHY: Enables complete UI development and verification of loading/error states without live backend services.
 */
public class MockFileApi implements FileApi {

    private final List<FileDto> myFiles = new CopyOnWriteArrayList<>();
    private final List<SharedFileDto> sharedWithMe = new CopyOnWriteArrayList<>();

    public MockFileApi() {
        seedSampleData();
    }

    /**
     * Seeds realistic sample rows matching Phase C2 specifications.
     */
    private void seedSampleData() {
        // Specified Sample Rows:
        // 1. contract-final.pdf 2.4 MB Clean shared with rahul (expires in 2 days)
        // 2. source-backup.zip 48 MB Quarantined Not shareable
        // 3. marksheet.pdf 310 KB Clean not shared
        myFiles.add(new FileDto(
                UUID.randomUUID().toString(),
                "contract-final.pdf",
                "2.4 MB",
                ScanStatus.CLEAN,
                "shared with rahul (expires in 2 days)"
        ));
        myFiles.add(new FileDto(
                UUID.randomUUID().toString(),
                "source-backup.zip",
                "48 MB",
                ScanStatus.QUARANTINED,
                "Not shareable"
        ));
        myFiles.add(new FileDto(
                UUID.randomUUID().toString(),
                "marksheet.pdf",
                "310 KB",
                ScanStatus.CLEAN,
                "not shared"
        ));

        // Sample rows for Shared with me
        sharedWithMe.add(new SharedFileDto(
                UUID.randomUUID().toString(),
                "project-architecture-v2.pdf",
                "3.8 MB",
                "priya.sharma",
                "Expires in 5 days",
                ScanStatus.CLEAN
        ));
        sharedWithMe.add(new SharedFileDto(
                UUID.randomUUID().toString(),
                "audit-compliance-report.docx",
                "820 KB",
                "security-admin",
                "Expires in 24 hours",
                ScanStatus.CLEAN
        ));
    }

    @Override
    public List<FileDto> getMyFiles() throws Exception {
        // Small artificial delay so loading states/spinner are clearly visible
        Thread.sleep(350);
        return new ArrayList<>(myFiles);
    }

    @Override
    public List<SharedFileDto> getSharedWithMe() throws Exception {
        Thread.sleep(300);
        return new ArrayList<>(sharedWithMe);
    }
}
