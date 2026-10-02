#!/usr/bin/env bash
set -euo pipefail

repository="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
base="$(sed -n 's/^MARKETING_VERSION=//p' "$repository/iosApp/Configuration/Version.xcconfig" | tr -d '[:space:]')"
build="${PROVENIO_BUILD_NUMBER:-${GITHUB_RUN_NUMBER:-0}}"
echo "${base}.${build}"
