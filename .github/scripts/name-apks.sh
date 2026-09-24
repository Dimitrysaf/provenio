#!/usr/bin/env bash
# Copies Gradle's APKs into the output folder under the names the in-app updater looks for.
set -euo pipefail
apk_dir="$1"
out_dir="$2"
mkdir -p "${out_dir}"
for apk in "${apk_dir}"/*.apk; do
  case "$(basename "${apk}")" in
    *-universal-*) name="Provenio.apk" ;;
    *-arm64-v8a-*) name="Provenio-arm64v8.apk" ;;
    *-armeabi-v7a-*) name="Provenio-armv7.apk" ;;
    *-x86_64-*) name="Provenio-x86_64.apk" ;;
    *-x86-*) name="Provenio-x86.apk" ;;
    *) continue ;;
  esac
  cp "${apk}" "${out_dir}/${name}"
done
if [[ ! -f "${out_dir}/Provenio.apk" ]]; then
  echo "No universal APK was built." >&2
  exit 1
fi
ls -l "${out_dir}"
