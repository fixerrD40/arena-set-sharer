#!/usr/bin/env bash
# Upload a set cover JPEG. Caller must be ROLE_ADMIN.
# Prompts for login and file; password is not echoed and not on the command line.
#
# Usage: ./scripts/put-cover.sh
set -euo pipefail

cleanup() { unset ADMIN_PASSWORD 2>/dev/null || true; }
trap cleanup EXIT

read -r -p "Admin email: " ADMIN_EMAIL
read -r -s -p "Admin password: " ADMIN_PASSWORD
echo >&2
read -r -p "Set code (e.g. hob): " SET_CODE_RAW
read -r -p "Cover JPEG path: " COVER_PATH
read -r -p "Base URL [http://localhost:8080]: " BASE_URL

SET_CODE=$(echo "$SET_CODE_RAW" | tr '[:upper:]' '[:lower:]')
BASE_URL=${BASE_URL:-http://localhost:8080}
BASE_URL=${BASE_URL%/}

if [[ -z "$ADMIN_EMAIL" || -z "$ADMIN_PASSWORD" || -z "$SET_CODE" || -z "$COVER_PATH" ]]; then
  echo "email, password, set code, and cover path are required." >&2
  exit 1
fi

if [[ ! -f "$COVER_PATH" ]]; then
  echo "File not found: $COVER_PATH" >&2
  exit 1
fi

token=$(curl -sS -X POST "$BASE_URL/auth/login" \
  -H 'Content-Type: application/json' \
  -d "$(jq -n \
    --arg email "$ADMIN_EMAIL" \
    --arg password "$ADMIN_PASSWORD" \
    '{email:$email, password:$password}')" \
  | jq -er '.token')

code=$(curl -sS -o /tmp/put-cover-body -w '%{http_code}' -X PUT \
  "$BASE_URL/api/assets/covers/${SET_CODE}.jpg" \
  -H "Authorization: Bearer $token" \
  -H 'Content-Type: image/jpeg' \
  --data-binary @"$COVER_PATH")

if [[ "$code" != "204" ]]; then
  echo "put-cover failed HTTP $code" >&2
  cat /tmp/put-cover-body >&2 || true
  echo >&2
  exit 1
fi

echo "Uploaded cover for $SET_CODE → $BASE_URL/api/assets/covers/${SET_CODE}.jpg"
