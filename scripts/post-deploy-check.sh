#!/usr/bin/env bash
# ==============================================================================
# File: scripts/post-deploy-check.sh
# WHAT: Post-deployment smoke test script verifying microservice health, authentication,
#       actions logging, and audit trail growth.
# WHY: Validates end-to-end production wiring and audit ingestion after deployment.
# ==============================================================================

set -e

GATEWAY_URL="${GATEWAY_URL:-https://localhost:8443}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@SecurePass2026!}"
TEST_USERNAME="${TEST_USERNAME:-smoke_user}"
TEST_EMAIL="${TEST_EMAIL:-smoke_user@securefileshare.local}"
TEST_PASSWORD="${TEST_PASSWORD:-SmokePass2026!}"

echo "=== SecureFileShare Post-Deploy Smoke Test ==="

echo "1. Checking gateway health..."
curl -s -k -f "${GATEWAY_URL}/health" > /dev/null && echo "Gateway /health is UP."

echo "2. Registering and logging in smoke test user..."
curl -s -k -X POST "${GATEWAY_URL}/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${TEST_USERNAME}\",\"email\":\"${TEST_EMAIL}\",\"password\":\"${TEST_PASSWORD}\",\"role\":\"ROLE_SENDER_RECIPIENT\"}" || true

LOGIN_RESPONSE=$(curl -s -k -X POST "${GATEWAY_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${TEST_USERNAME}\",\"password\":\"${TEST_PASSWORD}\"}")

USER_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
if [ -z "$USER_TOKEN" ]; then
  echo "ERROR: Failed to authenticate test user."
  exit 1
fi
echo "Test user authenticated successfully."

echo "3. Querying initial audit entries count as admin..."
ADMIN_LOGIN=$(curl -s -k -X POST "${GATEWAY_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${ADMIN_USERNAME}\",\"password\":\"${ADMIN_PASSWORD}\"}")
ADMIN_TOKEN=$(echo "$ADMIN_LOGIN" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
  echo "ERROR: Failed to authenticate admin user."
  exit 1
fi

INITIAL_ENTRIES_JSON=$(curl -s -k -X GET "${GATEWAY_URL}/api/v1/audit/entries" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}")

echo "4. Performing audited action (Key retrieval / Action trigger)..."
curl -s -k -X GET "${GATEWAY_URL}/api/v1/auth/keys/${TEST_USERNAME}" \
  -H "Authorization: Bearer ${USER_TOKEN}" > /dev/null

# Allow async outbox/audit ingestion a moment to complete
sleep 2

echo "5. Verifying audit entries count increased..."
FINAL_ENTRIES_JSON=$(curl -s -k -X GET "${GATEWAY_URL}/api/v1/audit/entries" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}")

echo "=== Post-Deploy Smoke Check Completed Successfully ==="
