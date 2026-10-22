package com.securefileshare.client.api;

import com.securefileshare.client.api.mock.MockFileApi;
import com.securefileshare.client.model.FileDto;
import com.securefileshare.client.model.ScanStatus;
import com.securefileshare.client.model.SharedFileDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MockFileApi.
 * WHAT: Asserts retrieval of seeded files matching Phase C2 specifications and verifies canShare rules.
 * WHY: Validates file service mock integrity before UI binding.
 */
class MockFileApiTest {

    private MockFileApi mockFileApi;

    @BeforeEach
    void setUp() {
        mockFileApi = new MockFileApi();
    }

    @Test
    @DisplayName("getMyFiles should return specified sample rows with exact names and scan statuses")
    void testGetMyFilesSampleRows() throws Exception {
        List<FileDto> files = mockFileApi.getMyFiles();

        assertThat(files).hasSize(3);

        FileDto file1 = files.get(0);
        assertThat(file1.getName()).isEqualTo("contract-final.pdf");
        assertThat(file1.getFormattedSize()).isEqualTo("2.4 MB");
        assertThat(file1.getScanStatus()).isEqualTo(ScanStatus.CLEAN);
        assertThat(file1.getSharedWith()).contains("rahul");
        assertThat(file1.isCanShare()).isTrue();

        FileDto file2 = files.get(1);
        assertThat(file2.getName()).isEqualTo("source-backup.zip");
        assertThat(file2.getFormattedSize()).isEqualTo("48 MB");
        assertThat(file2.getScanStatus()).isEqualTo(ScanStatus.QUARANTINED);
        assertThat(file2.getSharedWith()).isEqualTo("Not shareable");
        assertThat(file2.isCanShare()).isFalse();

        FileDto file3 = files.get(2);
        assertThat(file3.getName()).isEqualTo("marksheet.pdf");
        assertThat(file3.getFormattedSize()).isEqualTo("310 KB");
        assertThat(file3.getScanStatus()).isEqualTo(ScanStatus.CLEAN);
        assertThat(file3.getSharedWith()).isEqualTo("not shared");
        assertThat(file3.isCanShare()).isTrue();
    }

    @Test
    @DisplayName("getSharedWithMe should return incoming shared encrypted files")
    void testGetSharedWithMe() throws Exception {
        List<SharedFileDto> shared = mockFileApi.getSharedWithMe();

        assertThat(shared).isNotEmpty();
        assertThat(shared.get(0).getFileName()).isEqualTo("project-architecture-v2.pdf");
        assertThat(shared.get(0).getScanStatus()).isEqualTo(ScanStatus.CLEAN);
    }
}
