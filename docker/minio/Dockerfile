# ==============================================================================
# File: docker/minio/Dockerfile
# WHAT: Minimal, zero-registry-dependency MinIO object storage container.
# WHY: Downloads official standalone MinIO binary directly from dl.min.io CDN,
#      completely eliminating Docker Hub and Quay.io pull rate limits and 401 errors.
#      Supports both linux/amd64 (GitHub Actions runners) and linux/arm64 (Oracle Cloud ARM VM).
# ==============================================================================

FROM alpine:3.20

# Install MinIO and curl directly from official Alpine package repositories
# Why: Eliminates external CDN blocking (curl exit 22), registry rate limits, and architecture-specific download scripts
RUN apk add --no-cache minio curl && mkdir -p /data

# Expose S3 API port (9000) and Web Console port (9001)
EXPOSE 9000 9001

VOLUME ["/data"]

ENTRYPOINT ["minio"]
CMD ["server", "/data", "--console-address", ":9001"]
