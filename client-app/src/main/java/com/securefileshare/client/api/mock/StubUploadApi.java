package com.securefileshare.client.api.mock;

import com.securefileshare.client.api.UploadApi;
import com.securefileshare.client.model.EncryptedPackage;
import com.securefileshare.client.model.ServerVerdict;

import java.util.function.BiConsumer;

/**
 * Stub implementation of UploadApi for Phase C3.
 * WHAT: Simulates encrypted package upload progress and returns configurable server verdicts
 *       (CLEAN, SUSPICIOUS, or INFECTED).
 * WHY: Enables testing the full upload progression and testing all server verdict outcome paths.
 */
public class StubUploadApi implements UploadApi {

    private ServerVerdict verdict = ServerVerdict.CLEAN;
    private int delayMillis = 300;

    public StubUploadApi() {}

    public StubUploadApi(ServerVerdict verdict) {
        this.verdict = verdict;
    }

    public void setVerdict(ServerVerdict verdict) {
        this.verdict = verdict;
    }

    public void setDelayMillis(int delayMillis) {
        this.delayMillis = delayMillis;
    }

    @Override
    public ServerVerdict uploadPackage(EncryptedPackage encryptedPackage, BiConsumer<Long, Long> progressConsumer) throws Exception {
        long totalBytes = encryptedPackage != null ? encryptedPackage.getOriginalSizeBytes() : 1024 * 1024;
        int steps = 5;
        long chunkSize = totalBytes / steps;

        for (int i = 1; i <= steps; i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Upload cancelled by user");
            }

            long currentBytes = Math.min(totalBytes, i * chunkSize);
            if (progressConsumer != null) {
                progressConsumer.accept(currentBytes, totalBytes);
            }

            Thread.sleep(Math.max(10, delayMillis / steps));
        }

        return verdict;
    }
}
