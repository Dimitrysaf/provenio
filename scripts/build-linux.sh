#!/usr/bin/env bash
# Builds the Linux desktop app and packages it as dist/Provenio.flatpak.
#
#   scripts/build-linux.sh              everything, ending with the Flatpak bundle
#   scripts/build-linux.sh --no-flatpak the engine library and app JAR only (for `gradlew run`)
#
# Needs: a JDK 17+ (JAVA_HOME), a C/C++ toolchain, perl and make. CMake and Ninja come from PATH
# or the Android SDK. The Flatpak step needs flatpak-builder, native or as org.flatpak.Builder.
set -euo pipefail

# Compiling libtorrent, Boost and FFmpeg takes gigabytes per job. Outside CI the whole build runs
# in a memory-capped scope, so running out of memory ends the build instead of the desktop
# session. PROVENIO_BUILD_MEMORY (e.g. 6G) overrides the cap.
if [[ -z "${PROVENIO_BUILD_SCOPED:-}" && -z "${CI:-}" ]] && command -v systemd-run >/dev/null; then
    available_kb=$(awk '/^MemAvailable:/ {print $2}' /proc/meminfo)
    cap_mb=$(( available_kb * 7 / 10 / 1024 ))
    (( cap_mb > 8192 )) && cap_mb=8192
    (( cap_mb < 2048 )) && { echo "less than 3 GB of memory is free; close some apps first" >&2; exit 2; }
    memory_cap=${PROVENIO_BUILD_MEMORY:-${cap_mb}M}
    echo "Building inside a ${memory_cap} memory limit"
    exec systemd-run --user --scope --quiet --collect \
        -p MemoryMax="$memory_cap" -p MemorySwapMax=0 \
        --setenv=PROVENIO_BUILD_SCOPED=1 \
        "$0" "$@"
fi

repository=$(cd "$(dirname "$0")/.." && pwd -P)
build_root="$repository/build/linux"
architecture=$(uname -m)
make_flatpak=true
[[ "${1:-}" == "--no-flatpak" ]] && make_flatpak=false

case "$architecture" in
    x86_64) resource_platform=linux-x64 ;;
    aarch64) resource_platform=linux-arm64 ;;
    *) echo "unsupported architecture: $architecture" >&2; exit 2 ;;
esac

android_sdk=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}
if ! command -v cmake >/dev/null || ! command -v ninja >/dev/null; then
    sdk_cmake=$(ls -d "$android_sdk"/cmake/*/bin 2>/dev/null | sort -V | tail -n 1 || true)
    [[ -n "$sdk_cmake" ]] && export PATH="$sdk_cmake:$PATH"
fi
# About 2 GB per compile job, from the memory this build may use.
if [[ -z "${BUILD_JOBS:-}" ]]; then
    memory_mb=$(awk '/^MemAvailable:/ {print int($2 / 1024)}' /proc/meminfo)
    if [[ -r /sys/fs/cgroup/$(awk -F: '/^0::/ {print $3}' /proc/self/cgroup)/memory.max ]]; then
        limit=$(cat "/sys/fs/cgroup/$(awk -F: '/^0::/ {print $3}' /proc/self/cgroup)/memory.max")
        [[ "$limit" =~ ^[0-9]+$ ]] && memory_mb=$(( limit / 1024 / 1024 ))
    fi
    BUILD_JOBS=$(( memory_mb / 2048 ))
    (( BUILD_JOBS < 1 )) && BUILD_JOBS=1
    (( BUILD_JOBS > $(nproc) )) && BUILD_JOBS=$(nproc)
fi
export BUILD_JOBS
echo "Using $BUILD_JOBS parallel jobs"

for tool in cmake ninja perl make cc c++; do
    command -v "$tool" >/dev/null || { echo "missing build tool: $tool" >&2; exit 2; }
done

jni_includes=""
if [[ -n "${JAVA_HOME:-}" && -f "$JAVA_HOME/include/jni.h" ]]; then
    jni_includes="$JAVA_HOME/include;$JAVA_HOME/include/linux"
else
    # The NDK's jni.h is self-contained and uses the standard JNI ABI.
    ndk_jni=$(ls "$android_sdk"/ndk/*/toolchains/llvm/prebuilt/*/sysroot/usr/include/jni.h 2>/dev/null | sort -V | tail -n 1 || true)
    [[ -n "$ndk_jni" ]] || { echo "no jni.h: set JAVA_HOME to a full JDK" >&2; exit 2; }
    # Only the header: the sysroot around it holds Android's C library headers.
    install -Dm644 "$ndk_jni" "$build_root/jni/jni.h"
    jni_includes="$build_root/jni"
fi

echo "==> OpenSSL"
"$repository/engine/scripts/build-linux-openssl.sh" "$build_root/deps" "$architecture"
openssl_root="$build_root/deps/openssl/install/$architecture"

echo "==> Engine JNI library"
cmake -S "$repository/engine/platform/jvm" -B "$build_root/engine" -G Ninja \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_POSITION_INDEPENDENT_CODE=ON \
    -DENGINE_JNI_INCLUDE_DIRS="$jni_includes" \
    -DOPENSSL_ROOT_DIR="$openssl_root" \
    -DOPENSSL_INCLUDE_DIR="$openssl_root/include" \
    -DOPENSSL_SSL_LIBRARY="$openssl_root/lib/libssl.a" \
    -DOPENSSL_CRYPTO_LIBRARY="$openssl_root/lib/libcrypto.a" \
    -DOPENSSL_USE_STATIC_LIBS=TRUE \
    -DCMAKE_JOB_POOLS="compile=$BUILD_JOBS;link=1" \
    -DCMAKE_JOB_POOL_COMPILE=compile \
    -DCMAKE_JOB_POOL_LINK=link
cmake --build "$build_root/engine" --target engine --parallel "$BUILD_JOBS"
engine_library=$(find "$build_root/engine" -name 'libengine.so*' -type f | head -n 1)
[[ -n "$engine_library" ]] || { echo "libengine.so was not built" >&2; exit 1; }
# Compose Desktop bundles this directory as the app's resources, so `gradlew run` finds it too.
install -Dm755 "$engine_library" "$build_root/app-resources/$resource_platform/libengine.so"

echo "==> App JAR"
# A single-use Gradle with smaller heaps than the Android build's, stopped when done.
# CI runners have the memory for the Kotlin compiler's full heap.
kotlin_heap=1536m
[[ -n "${CI:-}" ]] && kotlin_heap=3g
(cd "$repository" && ./gradlew --no-daemon --no-configuration-cache \
    -Dorg.gradle.jvmargs="-Xmx2g -XX:MaxMetaspaceSize=768m" \
    -Pkotlin.daemon.jvmargs=-Xmx$kotlin_heap \
    -Dorg.gradle.workers.max="$BUILD_JOBS" \
    :composeApp:packageUberJarForCurrentOS \
    -Pprovenio.engine.fromSource=false)
app_jar=$(ls -t "$repository"/composeApp/build/compose/jars/*.jar | head -n 1)

input="$build_root/flatpak-input"
rm -rf "$input"
mkdir -p "$input"
cp "$app_jar" "$input/provenio.jar"
cp "$engine_library" "$input/libengine.so"
cp "$repository"/packaging/linux/{provenio.sh,io.github.dimitrysaf.Provenio.desktop,io.github.dimitrysaf.Provenio.metainfo.xml,io.github.dimitrysaf.Provenio.png} "$input/"

$make_flatpak || { echo "App JAR and engine ready in $input"; exit 0; }

echo "==> Flatpak"
# `flatpak run` moves org.flatpak.Builder into its own systemd scope, outside the memory cap
# above, so there the job count is what keeps FFmpeg and mpv within memory.
if command -v flatpak-builder >/dev/null; then
    flatpak_builder=(flatpak-builder)
else
    flatpak_builder=(flatpak run --filesystem=host org.flatpak.Builder)
fi
flatpak remote-add --user --if-not-exists flathub https://dl.flathub.org/repo/flathub.flatpakrepo
"${flatpak_builder[@]}" \
    --user \
    --install-deps-from=flathub \
    --force-clean \
    --ccache \
    --jobs="$BUILD_JOBS" \
    --state-dir="$build_root/flatpak-state" \
    --repo="$build_root/flatpak-repo" \
    "$build_root/flatpak-build" \
    "$repository/packaging/linux/io.github.dimitrysaf.Provenio.yml"
mkdir -p "$repository/dist"
flatpak build-bundle \
    --runtime-repo=https://dl.flathub.org/repo/flathub.flatpakrepo \
    "$build_root/flatpak-repo" \
    "$repository/dist/Provenio.flatpak" \
    io.github.dimitrysaf.Provenio
echo "Built dist/Provenio.flatpak"
