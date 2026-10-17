#!/usr/bin/env bash
set -euo pipefail

# bump-version.sh: Updates version.properties based on bump type (patch, minor, major, none)
BUMP_TYPE="${1:-patch}"
VERSION_FILE="version.properties"

if [ ! -f "$VERSION_FILE" ]; then
    echo "Error: $VERSION_FILE not found!"
    exit 1
fi

# Load current version properties
MAJOR=$(grep "^versionMajor=" "$VERSION_FILE" | cut -d'=' -f2)
MINOR=$(grep "^versionMinor=" "$VERSION_FILE" | cut -d'=' -f2)
PATCH=$(grep "^versionPatch=" "$VERSION_FILE" | cut -d'=' -f2)
CODE=$(grep "^versionCode=" "$VERSION_FILE" | cut -d'=' -f2)

PREV_VERSION="${MAJOR}.${MINOR}.${PATCH}"
echo "Current version: v${PREV_VERSION} (code: ${CODE})"

case "$BUMP_TYPE" in
    major)
        MAJOR=$((MAJOR + 1))
        MINOR=0
        PATCH=0
        CODE=$((CODE + 1))
        ;;
    minor)
        MINOR=$((MINOR + 1))
        PATCH=0
        CODE=$((CODE + 1))
        ;;
    patch)
        PATCH=$((PATCH + 1))
        CODE=$((CODE + 1))
        ;;
    none)
        echo "No version bump requested. Keeping v${PREV_VERSION}."
        ;;
    *)
        echo "Unknown bump type: $BUMP_TYPE. Expected 'major', 'minor', 'patch', or 'none'."
        exit 1
        ;;
esac

NEW_VERSION="${MAJOR}.${MINOR}.${PATCH}"

# Save updated properties if bumped
if [ "$BUMP_TYPE" != "none" ]; then
    cat <<EOF > "$VERSION_FILE"
# Attendly Version Properties
# Managed automatically by CI/CD workflow and local development
versionMajor=${MAJOR}
versionMinor=${MINOR}
versionPatch=${PATCH}
versionCode=${CODE}
EOF
    echo "Updated version to v${NEW_VERSION} (code: ${CODE})"
fi

# Export outputs for GitHub Actions if running in GHA
if [ -n "${GITHUB_OUTPUT:-}" ]; then
    echo "version_name=${NEW_VERSION}" >> "$GITHUB_OUTPUT"
    echo "version_code=${CODE}" >> "$GITHUB_OUTPUT"
    echo "prev_version=${PREV_VERSION}" >> "$GITHUB_OUTPUT"
    echo "tag_name=v${NEW_VERSION}" >> "$GITHUB_OUTPUT"
fi
