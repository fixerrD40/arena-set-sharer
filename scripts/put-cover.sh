#!/usr/bin/env bash
# Upload every set-covers/{code}.jpg to PUT /api/assets/covers/{code}.jpg.
# Skips codes that already return 200 (idempotent). Caller must be ROLE_ADMIN.
#
# Usage: ./scripts/put-cover.sh
# Optional: PUT_COVER_FORCE=1 to re-upload even when present.
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
COVERS_DIR="$ROOT_DIR/set-covers"

cleanup() { unset ADMIN_PASSWORD 2>/dev/null || true; }
trap cleanup EXIT

read -r -p "Admin email: " ADMIN_EMAIL
read -r -s -p "Admin password: " ADMIN_PASSWORD
echo >&2
read -r -p "Base URL [http://localhost:8080]: " BASE_URL

BASE_URL=${BASE_URL:-http://localhost:8080}
BASE_URL=${BASE_URL%/}

if [[ -z "$ADMIN_EMAIL" || -z "$ADMIN_PASSWORD" ]]; then
  echo "email and password are required." >&2
  exit 1
fi

if [[ ! -d "$COVERS_DIR" ]]; then
  echo "Missing $COVERS_DIR — drop {setCode}.jpg files there." >&2
  exit 1
fi

shopt -s nullglob
files=("$COVERS_DIR"/*.jpg "$COVERS_DIR"/*.JPG "$COVERS_DIR"/*.jpeg "$COVERS_DIR"/*.JPEG)
if [[ ${#files[@]} -eq 0 ]]; then
  echo "No JPEG files in $COVERS_DIR" >&2
  exit 1
fi

token=$(curl -sS -X POST "$BASE_URL/auth/login" \
  -H 'Content-Type: application/json' \
  -d "$(jq -n \
    --arg email "$ADMIN_EMAIL" \
    --arg password "$ADMIN_PASSWORD" \
    '{email:$email, password:$password}')" \
  | jq -er '.token')

uploaded=0
skipped=0
failed=0

for cover_path in "${files[@]}"; do
  base=$(basename "$cover_path")
  set_code=$(echo "${base%.*}" | tr '[:upper:]' '[:lower:]')
  if [[ ! "$set_code" =~ ^[a-z0-9]{2,16}$ ]]; then
    echo "skip $base (invalid set code)" >&2
    failed=$((failed + 1))
    continue
  fi

  url="$BASE_URL/api/assets/covers/${set_code}.jpg"

  # Sharer requires JPEG magic (FF D8 FF); mislabeled webp/png/avif become HTTP 400.
  magic=$(od -An -tx1 -N3 "$cover_path" | tr -d ' \n')
  if [[ "$magic" != "ffd8ff" ]]; then
    echo "failed $set_code (not a JPEG; got magic $magic — convert before upload)" >&2
    failed=$((failed + 1))
    continue
  fi

  if [[ "${PUT_COVER_FORCE:-}" != "1" ]]; then
    existing=$(curl -sS -o /dev/null -w '%{http_code}' "$url" || true)
    if [[ "$existing" == "200" ]]; then
      echo "skip $set_code (already present)"
      skipped=$((skipped + 1))
      continue
    fi
  fi

  code=$(curl -sS -o /tmp/put-cover-body -w '%{http_code}' -X PUT "$url" \
    -H "Authorization: Bearer $token" \
    -H 'Content-Type: image/jpeg' \
    --data-binary @"$cover_path")

  if [[ "$code" == "204" ]]; then
    echo "uploaded $set_code → $url"
    uploaded=$((uploaded + 1))
  else
    echo "failed $set_code HTTP $code" >&2
    cat /tmp/put-cover-body >&2 || true
    echo >&2
    failed=$((failed + 1))
  fi
done

echo "done: uploaded=$uploaded skipped=$skipped failed=$failed"
[[ "$failed" -eq 0 ]]
