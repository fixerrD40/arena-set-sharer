#!/bin/sh
set -eu
# Named volumes mount as root; ensure APP_COVERS_DIR is writable by app.
COVERS_DIR="${APP_COVERS_DIR:-/covers}"
mkdir -p "$COVERS_DIR"
chown -R app:app "$COVERS_DIR"
exec su-exec app java -jar /app/app.jar
