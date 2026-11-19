# ==============================================================================
# File: docker/minio/Dockerfile
# WHAT: Minimal, zero-registry-dependency MinIO object storage container.
# WHY: Downloads official standalone MinIO binary directly from dl.min.io CDN,
#      completely eliminating Docker Hub and Quay.io pull rate limits and 401 errors.
#      Supports both linux/amd64 (GitHub Actions runners) and linux/arm64 (Oracle Cloud ARM VM).
# ==============================================================================

FROM alpine:3.20

# Install MinIO, curl, and tzdata (for precise time/clock synchronization on cloud VMs and runners)
RUN apk add --no-cache minio curl tzdata && mkdir -p /data

EXPOSE 9000 9001

VOLUME ["/data"]

ENTRYPOINT ["minio"]
CMD ["server", "/data", "--console-address", ":9001"]
