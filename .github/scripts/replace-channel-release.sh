#!/usr/bin/env bash
# Replaces a channel's release and its tag with this commit, so each channel keeps a single release.
# Usage: replace-channel-release.sh <tag> <title> <notes file> [extra gh release create flags...]
set -euo pipefail
tag="$1"
title="$2"
notes="$3"
shift 3
repo="${GITHUB_REPOSITORY}"
if gh release view "${tag}" --repo "${repo}" >/dev/null 2>&1 </dev/null; then
  gh release delete "${tag}" --repo "${repo}" --yes </dev/null
fi
if gh api "repos/${repo}/git/ref/tags/${tag}" >/dev/null 2>&1 </dev/null; then
  gh api -X DELETE "repos/${repo}/git/refs/tags/${tag}" </dev/null
fi
gh release create "${tag}" \
  --repo "${repo}" \
  --target "${GITHUB_SHA}" \
  --title "${title}" \
  --notes-file "${notes}" \
  "$@" \
  dist/*.apk </dev/null
