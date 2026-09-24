#!/usr/bin/env bash
# Keeps only the two channel releases: deletes every other release and every other tag.
set -euo pipefail
keep() { [[ "$1" == "beta" || "$1" == "stable" ]]; }
gh release list --repo "${GITHUB_REPOSITORY}" --limit 1000 --json tagName --jq '.[].tagName' |
  while read -r tag; do
    keep "${tag}" || gh release delete "${tag}" --repo "${GITHUB_REPOSITORY}" --cleanup-tag --yes || true
  done
gh api --paginate "repos/${GITHUB_REPOSITORY}/git/refs/tags" --jq '.[].ref' 2>/dev/null |
  while read -r ref; do
    keep "${ref#refs/tags/}" || gh api -X DELETE "repos/${GITHUB_REPOSITORY}/git/${ref}" || true
  done
