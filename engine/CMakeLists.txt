cmake_minimum_required(VERSION 3.24)

project(
    engine
    VERSION 0.1.1
    DESCRIPTION "Cross-platform peer-to-peer streaming engine"
    LANGUAGES C CXX
)

include(GNUInstallDirs)
find_package(Threads REQUIRED)

option(ENGINE_BUILD_SHARED "Build Engine as a shared library" OFF)
option(ENGINE_ENABLE_LIBTORRENT "Build the pinned libtorrent protocol backend" OFF)
set(ENGINE_BUILD_TESTS_DEFAULT OFF)
if(PROJECT_IS_TOP_LEVEL)
    set(ENGINE_BUILD_TESTS_DEFAULT ON)
endif()
option(
    ENGINE_BUILD_TESTS
    "Build Engine tests"
    ${ENGINE_BUILD_TESTS_DEFAULT}
)

set(ENGINE_LIBRARY_TYPE STATIC)
if(ENGINE_BUILD_SHARED)
    set(ENGINE_LIBRARY_TYPE SHARED)
endif()

add_library(engine ${ENGINE_LIBRARY_TYPE})
add_library(Engine::Engine ALIAS engine)

target_sources(
    engine
    PRIVATE
        src/core/engine.cpp
        src/core/engine_runtime.cpp
        src/cache/verified_piece_cache.cpp
        src/cache/disk_cache_manager.cpp
        src/http/byte_range.cpp
        src/http/http_protocol.cpp
        src/http/loopback_server.cpp
        src/scheduler/piece_scheduler.cpp
        src/scheduler/stream_demand_plan.cpp
        src/security/random_bytes.cpp
        src/storage/atomic_file.cpp
        src/torrent/file_selection.cpp
)

if(ENGINE_ENABLE_LIBTORRENT)
    include(cmake/Dependencies.cmake)
    engine_add_libtorrent()
    target_sources(
        engine
        PRIVATE
            src/torrent/libtorrent_backend.cpp
            src/torrent/libtorrent_stream_bridge.cpp
    )
    target_link_libraries(engine PRIVATE $<BUILD_INTERFACE:torrent-rasterbar>)
    target_compile_definitions(engine PRIVATE ENGINE_HAS_LIBTORRENT=1)
    if(APPLE)
        find_package(OpenSSL REQUIRED)
        find_library(SECURITY_FRAMEWORK Security REQUIRED)
        target_sources(engine PRIVATE src/security/apple_trust.cpp)
        target_link_libraries(
            engine
            PRIVATE
                OpenSSL::SSL
                "${SECURITY_FRAMEWORK}"
        )
    elseif(WIN32)
        find_package(OpenSSL REQUIRED)
        target_sources(engine PRIVATE src/security/windows_trust.cpp)
        target_link_libraries(engine PRIVATE OpenSSL::SSL)
    endif()
else()
    target_sources(engine PRIVATE src/torrent/protocol_backend_stub.cpp)
endif()

target_include_directories(
    engine
    PUBLIC
        $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
        $<INSTALL_INTERFACE:${CMAKE_INSTALL_INCLUDEDIR}>
    PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/src
)

target_compile_features(engine PUBLIC cxx_std_20)
target_link_libraries(engine PUBLIC Threads::Threads)
if(WIN32)
    target_link_libraries(engine PRIVATE ws2_32 bcrypt crypt32)
    set_target_properties(engine PROPERTIES PREFIX "")
    if(ENGINE_BUILD_SHARED)
        target_sources(engine PRIVATE cmake/engine-windows.def)
    endif()
endif()
if(CMAKE_SYSTEM_NAME STREQUAL "Linux" AND ENGINE_BUILD_SHARED)
    target_link_options(
        engine
        PRIVATE
            "LINKER:--version-script=${CMAKE_CURRENT_SOURCE_DIR}/cmake/engine-linux.map"
            "LINKER:--exclude-libs,ALL"
            "LINKER:-z,defs"
            "LINKER:-z,relro,-z,now"
    )
endif()
set_target_properties(
    engine
    PROPERTIES
        CXX_EXTENSIONS OFF
        CXX_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN YES
        VERSION ${PROJECT_VERSION}
        SOVERSION ${PROJECT_VERSION_MAJOR}
)

if(ENGINE_BUILD_SHARED)
    target_compile_definitions(engine PRIVATE ENGINE_BUILDING_SHARED)
    target_compile_definitions(engine INTERFACE ENGINE_USING_SHARED)
endif()

if(MSVC)
    target_compile_options(engine PRIVATE /W4 /permissive-)
else()
    target_compile_options(engine PRIVATE -Wall -Wextra -Wpedantic -Wconversion -Wshadow)
endif()

if(ENGINE_BUILD_TESTS)
    enable_testing()
    add_subdirectory(tests)
    if(ENGINE_ENABLE_LIBTORRENT)
        target_compile_definitions(
            engine_tests
            PRIVATE ENGINE_EXPECT_LIBTORRENT=1
        )
    endif()
endif()

install(
    TARGETS engine
    EXPORT EngineTargets
    ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
)
install(DIRECTORY include/ DESTINATION ${CMAKE_INSTALL_INCLUDEDIR})
install(
    EXPORT EngineTargets
    FILE EngineTargets.cmake
    NAMESPACE Engine::
    DESTINATION ${CMAKE_INSTALL_LIBDIR}/cmake/Engine
)
