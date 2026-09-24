#!/usr/bin/env bash
# Replaces a channel's release and its tag with this commit, so each channel keeps a single release.
# Usage: replace-channel-release.sh <tag> <title> <notes file> [extra gh release create flags...]
set -euo pipefail
tag="$1"
title="$2"
notes="$3"
shift 3
gh release delete "${tag}" --repo "${GITHUB_REPOSITORY}" --cleanup-tag --yes 2>/dev/null || true
gh api -X DELETE "repos/${GITHUB_REPOSITORY}/git/refs/tags/${tag}" 2>/dev/null || true
gh release create "${tag}" \
  --repo "${GITHUB_REPOSITORY}" \
  --target "${GITHUB_SHA}" \
  --title "${title}" \
  --notes-file "${notes}" \
  "$@" \
  dist/*.apk
