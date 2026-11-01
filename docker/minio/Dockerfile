# ==============================================================================
# File: docker/minio/Dockerfile
# WHAT: Minimal, zero-registry-dependency MinIO object storage container.
# WHY: Downloads official standalone MinIO binary directly from dl.min.io CDN,
#      completely eliminating Docker Hub and Quay.io pull rate limits and 401 errors.
#      Supports both linux/amd64 (GitHub Actions runners) and linux/arm64 (Oracle Cloud ARM VM).
# ==============================================================================

FROM alpine:3.20

# Install runtime dependencies: libc6-compat provides glibc compatibility for Go binaries on musl Alpine
RUN apk add --no-cache curl libc6-compat ca-certificates

# Detect host architecture and download official MinIO release binary
RUN ARCH=$(uname -m) && \
    if [ "$ARCH" = "x86_64" ]; then MINIO_ARCH="linux-amd64"; \
    elif [ "$ARCH" = "aarch64" ]; then MINIO_ARCH="linux-arm64"; \
    else MINIO_ARCH="linux-amd64"; fi && \
    curl -s -f -L "https://dl.min.io/server/minio/release/${MINIO_ARCH}/minio" -o /usr/bin/minio && \
    chmod +x /usr/bin/minio

# Create default storage directory
RUN mkdir -p /data

# Expose S3 API port (9000) and Web Console port (9001)
EXPOSE 9000 9001

VOLUME ["/data"]

ENTRYPOINT ["minio"]
CMD ["server", "/data", "--console-address", ":9001"]
