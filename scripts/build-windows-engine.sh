#!/usr/bin/env bash
# Cross-builds the engine's JNI library for the Windows desktop app, on Linux, into build/windows.
#
#   scripts/build-windows-engine.sh     build/windows/engine.dll and build/windows/provenio.ico
#
# Needs: llvm-mingw in /opt/llvm-mingw, a JDK (JAVA_HOME), CMake, Ninja, perl, make and ImageMagick.
set -euo pipefail

repository=$(cd "$(dirname "$0")/.." && pwd -P)
engine_root="$repository/engine"
build_root="$repository/build/windows"
dependency_root="$engine_root/platform/windows/.deps"
target_triple=x86_64-w64-mingw32
openssl_root="$dependency_root/openssl/install/x86_64"
jobs=${BUILD_JOBS:-$(nproc)}

for tool in cmake ninja perl make convert "/opt/llvm-mingw/bin/${target_triple}-clang"; do
    command -v "$tool" >/dev/null || { echo "missing build tool: $tool" >&2; exit 2; }
done
[[ -f "${JAVA_HOME:-}/include/jni.h" ]] || { echo "no jni.h: set JAVA_HOME to a full JDK" >&2; exit 2; }

# The engine's Windows OpenSSL build reads its target config from /source, where its container mounts the engine.
if [[ ! -e /source ]]; then
    sudo ln -s "$engine_root" /source
fi

echo "==> Dependencies"
"$engine_root/scripts/prepare-native-dependencies.sh" "$dependency_root"
"$engine_root/scripts/build-windows-openssl.sh" "$dependency_root" x86_64

# jni.h is the same everywhere; jni_md.h is Windows' own, so the JNI functions are exported from the DLL.
jni_includes="$build_root/jni"
mkdir -p "$jni_includes"
cp "$JAVA_HOME/include/jni.h" "$jni_includes/jni.h"
cat > "$jni_includes/jni_md.h" <<'EOF'
#ifndef _JAVASOFT_JNI_MD_H_
#define _JAVASOFT_JNI_MD_H_
#define JNIEXPORT __declspec(dllexport)
#define JNIIMPORT __declspec(dllimport)
#define JNICALL __stdcall
typedef long jint;
typedef long long jlong;
typedef signed char jbyte;
#endif
EOF

echo "==> Engine JNI library"
cmake -S "$engine_root/platform/jvm" -B "$build_root/engine" -G Ninja \
    -DCMAKE_TOOLCHAIN_FILE="$engine_root/cmake/windows-llvm-mingw-toolchain.cmake" \
    -DWINDOWS_TRIPLE="$target_triple" \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_C_FLAGS="-D_WIN32_WINNT=0x0A00 -DWINVER=0x0A00" \
    -DCMAKE_CXX_FLAGS="-D_WIN32_WINNT=0x0A00 -DWINVER=0x0A00" \
    -DCMAKE_POSITION_INDEPENDENT_CODE=ON \
    -DENGINE_JNI_INCLUDE_DIRS="$jni_includes" \
    -DOPENSSL_ROOT_DIR="$openssl_root" \
    -DOPENSSL_INCLUDE_DIR="$openssl_root/include" \
    -DOPENSSL_SSL_LIBRARY="$openssl_root/lib/libssl.a" \
    -DOPENSSL_CRYPTO_LIBRARY="$openssl_root/lib/libcrypto.a" \
    -DSSL_EAY="$openssl_root/lib/libssl.a" \
    -DLIB_EAY="$openssl_root/lib/libcrypto.a" \
    -DOPENSSL_USE_STATIC_LIBS=TRUE \
    -DFETCHCONTENT_SOURCE_DIR_BOOST="$dependency_root/sources/boost_1_86_0" \
    -DFETCHCONTENT_SOURCE_DIR_LIBTORRENT="$dependency_root/sources/libtorrent-2.0.12"
cmake --build "$build_root/engine" --target engine --parallel "$jobs"
engine_library=$(find "$build_root/engine" -name 'engine.dll' -type f | head -n 1)
[[ -n "$engine_library" ]] || { echo "engine.dll was not built" >&2; exit 1; }
cp "$engine_library" "$build_root/engine.dll"
"/opt/llvm-mingw/bin/llvm-strip" --strip-unneeded "$build_root/engine.dll"

echo "==> Icon"
convert "$repository/packaging/linux/io.github.dimitrysaf.Provenio.png" \
    -define icon:auto-resize=256,128,64,48,32,16 "$build_root/provenio.ico"

echo "Built build/windows/engine.dll"
