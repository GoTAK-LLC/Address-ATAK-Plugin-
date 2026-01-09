#!/usr/bin/env bash
set -euo pipefail

echo "=== STARSYNC Plugin Release Builder ==="
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PROJECT_NAME="$(basename "$PWD")"
BUILD_FILE="app/build.gradle"

if [[ ! -f "$BUILD_FILE" ]]; then
  echo "Error: '$BUILD_FILE' not found." >&2
  exit 1
fi

VERSION_LINE=$(grep -E '^\s*ext\.PLUGIN_VERSION\s*=\s*"[^"]+"' "$BUILD_FILE" | head -n 1 || true)
if [[ -z "$VERSION_LINE" ]]; then
  echo "Error: Could not find PLUGIN_VERSION in $BUILD_FILE" >&2
  exit 1
fi

VERSION=${VERSION_LINE#*\"}
VERSION=${VERSION%%\"*}

echo "Version: $VERSION"
echo ""

ZIP_NAME="${PROJECT_NAME}-${VERSION}.zip"
PARENT_DIR="$(dirname "$PWD")"
ZIP_PATH="${PARENT_DIR}/${ZIP_NAME}"

echo "Creating: $ZIP_PATH"
echo ""

rm -f "$ZIP_PATH"

pushd "$PARENT_DIR" >/dev/null

zip -r "$ZIP_PATH" "$PROJECT_NAME" \
    -x "$PROJECT_NAME/.git/*" \
    -x "$PROJECT_NAME/.gradle/*" \
    -x "$PROJECT_NAME/build/*" \
    -x "$PROJECT_NAME/app/build/*" \
    -x "$PROJECT_NAME/tools/*" \
    > /dev/null

popd >/dev/null

echo "✓ Done: $(du -h "$ZIP_PATH" | cut -f1)"
