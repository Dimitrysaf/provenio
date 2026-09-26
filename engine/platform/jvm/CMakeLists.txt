# The engine as a JNI library for desktop JVMs (the Linux app). It is the Android library's
# native half built for the host: the same core, the same JNI bridge and the same exports.
cmake_minimum_required(VERSION 3.24)

project(engine_jvm LANGUAGES C CXX)

get_filename_component(ENGINE_ROOT "${CMAKE_CURRENT_LIST_DIR}/../.." ABSOLUTE)
set(ENGINE_JNI_BRIDGE "${ENGINE_ROOT}/platform/android/engine/src/main/cpp")

set(ENGINE_BUILD_SHARED ON CACHE BOOL "" FORCE)
set(ENGINE_ENABLE_LIBTORRENT ON CACHE BOOL "" FORCE)
set(ENGINE_BUILD_TESTS OFF CACHE BOOL "" FORCE)

# jni.h and jni_md.h: a JDK's include directory, or any self-contained jni.h.
set(ENGINE_JNI_INCLUDE_DIRS "" CACHE STRING "Directories holding jni.h")
if(NOT ENGINE_JNI_INCLUDE_DIRS)
    message(FATAL_ERROR "Set ENGINE_JNI_INCLUDE_DIRS to the directories holding jni.h")
endif()

add_subdirectory("${ENGINE_ROOT}" "${CMAKE_CURRENT_BINARY_DIR}/engine-core")

target_sources(engine PRIVATE "${ENGINE_JNI_BRIDGE}/jni_bridge.cpp")
target_include_directories(engine PRIVATE "${ENGINE_JNI_BRIDGE}" ${ENGINE_JNI_INCLUDE_DIRS})
if(WIN32)
    # MinGW: the C++ runtime links in statically, and JNIEXPORT already marks the JNI functions for export.
    target_link_options(engine PRIVATE "-static")
else()
    # The C++ runtime goes inside the library so it runs on any distribution's libstdc++.
    target_link_options(
        engine
        PRIVATE
            "-static-libstdc++"
            "-static-libgcc"
            "-Wl,--version-script=${ENGINE_JNI_BRIDGE}/exports.map"
            "-Wl,--exclude-libs,ALL"
    )
endif()
