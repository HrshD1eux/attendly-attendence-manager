#!/usr/bin/env bash
set -euo pipefail

# generate-release-notes.sh: Generates formatted dynamic release notes from git commits
TAG_NAME="${1:-}"
OUTPUT_FILE="${2:-release_notes.md}"
REPO_NAME="${GITHUB_REPOSITORY:-HrshD1eux/attendly-attendence-manager}"

# Determine previous tag
PREV_TAG=""
if [ -n "$TAG_NAME" ] && git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
    PREV_TAG=$(git describe --tags --abbrev=0 "${TAG_NAME}^" 2>/dev/null || echo "")
else
    PREV_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
fi

echo "Generating release notes for $TAG_NAME (previous tag: ${PREV_TAG:-none})"

# Gather commits
if [ -n "$PREV_TAG" ]; then
    COMMIT_LOG=$(git log "${PREV_TAG}..HEAD" --pretty=format:"%h|%s|%an" 2>/dev/null || git log -n 25 --pretty=format:"%h|%s|%an")
else
    COMMIT_LOG=$(git log -n 50 --pretty=format:"%h|%s|%an")
fi

# Categorization arrays / temp files
FEATS=""
FIXES=""
UI=""
PERF=""
BUILD=""
DOCS=""
OTHER=""

while IFS='|' read -r hash subject author; do
    [ -z "$hash" ] && continue
    # Skip skip-ci / release commits to avoid noise
    if echo "$subject" | grep -qiE '\[skip ci\]|chore\(release\)'; then
        continue
    fi

    item="- ${subject} ([${hash}](https://github.com/${REPO_NAME}/commit/${hash}))"

    if echo "$subject" | grep -qiE '^(feat|feature|add)'; then
        FEATS="${FEATS}${item}\n"
    elif echo "$subject" | grep -qiE '^(fix|bug|patch)'; then
        FIXES="${FIXES}${item}\n"
    elif echo "$subject" | grep -qiE '^(ui|style|design|theme)'; then
        UI="${UI}${item}\n"
    elif echo "$subject" | grep -qiE '^(perf|refactor|clean)'; then
        PERF="${PERF}${item}\n"
    elif echo "$subject" | grep -qiE '^(build|ci|deps|workflow)'; then
        BUILD="${BUILD}${item}\n"
    elif echo "$subject" | grep -qiE '^(doc|docs)'; then
        DOCS="${DOCS}${item}\n"
    else
        OTHER="${OTHER}${item}\n"
    fi
done <<< "$COMMIT_LOG"

# Build markdown file
{
    echo "## Attendly Release ${TAG_NAME}"
    echo ""
    echo "**Released on:** $(date -u '+%B %d, %Y (%H:%M UTC)')"
    echo ""

    if [ -n "$FEATS" ]; then
        echo "### 🚀 Features & New Capabilities"
        echo -e "$FEATS"
    fi

    if [ -n "$UI" ]; then
        echo "### 🎨 UI & UX Improvements"
        echo -e "$UI"
    fi

    if [ -n "$FIXES" ]; then
        echo "### 🐛 Bug Fixes & Stability"
        echo -e "$FIXES"
    fi

    if [ -n "$PERF" ]; then
        echo "### ⚡ Performance & Architecture"
        echo -e "$PERF"
    fi

    if [ -n "$BUILD" ]; then
        echo "### 📦 Build, Dependencies & CI/CD"
        echo -e "$BUILD"
    fi

    if [ -n "$DOCS" ]; then
        echo "### 📝 Documentation"
        echo -e "$DOCS"
    fi

    if [ -n "$OTHER" ]; then
        echo "### 🔍 Additional Changes"
        echo -e "$OTHER"
    fi

    # Fallback if no categorized commits found
    if [ -z "$FEATS" ] && [ -z "$UI" ] && [ -z "$FIXES" ] && [ -z "$PERF" ] && [ -z "$BUILD" ] && [ -z "$OTHER" ]; then
        echo "### 📋 Changes"
        echo "- General performance, stability, and UI improvements."
        echo ""
    fi

    if [ -n "$PREV_TAG" ]; then
        echo "---"
        echo "🔍 **Full Changelog**: https://github.com/${REPO_NAME}/compare/${PREV_TAG}...${TAG_NAME}"
        echo ""
    fi

    echo "---"
    echo "### 📲 Installation & Verification"
    echo "1. Download \`Attendly-${TAG_NAME}.apk\` on your Android device."
    echo "2. Open the file and allow installation from your browser/files app if prompted."
    echo "3. Verify package SHA-256 with \`checksums.txt\` if required."
} > "$OUTPUT_FILE"

echo "Release notes written to $OUTPUT_FILE"
