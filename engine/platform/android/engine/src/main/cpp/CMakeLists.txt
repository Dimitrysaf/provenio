cmake_minimum_required(VERSION 3.24)

project(engine_android LANGUAGES C CXX)

get_filename_component(ENGINE_ROOT "${CMAKE_CURRENT_LIST_DIR}/../../../../../.." ABSOLUTE)

set(ENGINE_BUILD_SHARED ON CACHE BOOL "" FORCE)
set(ENGINE_ENABLE_LIBTORRENT ON CACHE BOOL "" FORCE)
set(ENGINE_BUILD_TESTS OFF CACHE BOOL "" FORCE)

set(OPENSSL_ROOT
    "${ENGINE_ROOT}/platform/android/.deps/openssl-3.5.7/install/${ANDROID_ABI}"
)
set(OPENSSL_ROOT_DIR "${OPENSSL_ROOT}" CACHE PATH "" FORCE)
set(OPENSSL_INCLUDE_DIR "${OPENSSL_ROOT}/include" CACHE PATH "" FORCE)
set(OPENSSL_SSL_LIBRARY "${OPENSSL_ROOT}/lib/libssl.a" CACHE FILEPATH "" FORCE)
set(OPENSSL_CRYPTO_LIBRARY "${OPENSSL_ROOT}/lib/libcrypto.a" CACHE FILEPATH "" FORCE)
set(OPENSSL_USE_STATIC_LIBS TRUE CACHE BOOL "" FORCE)

add_subdirectory("${ENGINE_ROOT}" "${CMAKE_CURRENT_BINARY_DIR}/engine-core")

target_sources(engine PRIVATE jni_bridge.cpp)
target_include_directories(engine PRIVATE "${CMAKE_CURRENT_LIST_DIR}")
target_link_options(
    engine
    PRIVATE
        "-Wl,-z,max-page-size=16384"
        "-Wl,--version-script=${CMAKE_CURRENT_LIST_DIR}/exports.map"
        "-Wl,--exclude-libs,ALL"
)
