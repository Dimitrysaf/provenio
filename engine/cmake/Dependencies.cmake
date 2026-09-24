include(FetchContent)

function(engine_add_libtorrent)
    if(TARGET torrent-rasterbar)
        return()
    endif()

    cmake_policy(PUSH)
    if(POLICY CMP0144)
        cmake_policy(SET CMP0144 NEW)
    endif()
    if(POLICY CMP0167)
        cmake_policy(SET CMP0167 OLD)
    endif()
    if(POLICY CMP0169)
        cmake_policy(SET CMP0169 OLD)
    endif()
    if(POLICY CMP0183)
        cmake_policy(SET CMP0183 OLD)
    endif()

    FetchContent_Declare(
        boost
        URL https://archives.boost.io/release/1.86.0/source/boost_1_86_0.tar.bz2
        URL_HASH SHA256=1bed88e40401b2cb7a1f76d4bab499e352fa4d0c5f31c0dbae64e24d34d7513b
        DOWNLOAD_EXTRACT_TIMESTAMP TRUE
    )
    FetchContent_GetProperties(boost)
    if(NOT boost_POPULATED)
        FetchContent_Populate(boost)
    endif()

    set(BOOST_ROOT "${boost_SOURCE_DIR}")
    # FindBoost applies toolchain root-path rules while cross-compiling. Point it
    # at the populated, checksummed headers explicitly so Android and Apple
    # builds cannot accidentally fall back to host Boost installations.
    set(Boost_INCLUDE_DIR "${boost_SOURCE_DIR}")
    set(Boost_INCLUDE_DIRS "${boost_SOURCE_DIR}")
    set(Boost_NO_SYSTEM_PATHS ON)
    set(Boost_NO_BOOST_CMAKE ON)
    set(CMAKE_POLICY_DEFAULT_CMP0144 NEW)
    set(CMAKE_POLICY_DEFAULT_CMP0167 OLD)
    set(CMAKE_POLICY_DEFAULT_CMP0183 OLD)

    FetchContent_Declare(
        libtorrent
        GIT_REPOSITORY https://github.com/arvidn/libtorrent.git
        GIT_TAG 740a0b9aeabe00e762cc0efe4a0f27593db2550b
        GIT_SHALLOW FALSE
        GIT_PROGRESS TRUE
        # try_signal is part of libtorrent's portable signal handling. The
        # simulation and optional GnuTLS adapter submodules are not used by this
        # build and would multiply every cross-ABI checkout.
        GIT_SUBMODULES deps/try_signal
        GIT_SUBMODULES_RECURSE FALSE
    )
    FetchContent_GetProperties(libtorrent)
    if(NOT libtorrent_POPULATED)
        FetchContent_Populate(libtorrent)
    endif()

    set(
        libtorrent_patches
        "${CMAKE_CURRENT_FUNCTION_LIST_DIR}/patches/libtorrent-2.0.12-macos-route-bounds.patch"
        "${CMAKE_CURRENT_FUNCTION_LIST_DIR}/patches/libtorrent-2.0.12-ca-bundle.patch"
        "${CMAKE_CURRENT_FUNCTION_LIST_DIR}/patches/libtorrent-2.0.12-apple-trust.patch"
        "${CMAKE_CURRENT_FUNCTION_LIST_DIR}/patches/libtorrent-2.0.12-windows-trust.patch"
        "${CMAKE_CURRENT_FUNCTION_LIST_DIR}/patches/libtorrent-2.0.12-windows-system-trust.patch"
    )
    set(
        libtorrent_patch_files
        "src/enum_net.cpp"
        "include/libtorrent/settings_pack.hpp"
        "src/session_impl.cpp"
        "src/session_impl.cpp"
        "src/session_impl.cpp"
    )
    set(
        libtorrent_patch_sentinels
        "message_end = reinterpret_cast<char*>"
        "ssl_ca_bundle"
        "apple_configure_tls_trust"
        "windows_configure_tls_trust"
        "live Windows store without replacing an explicit PEM store"
    )
    list(LENGTH libtorrent_patches libtorrent_patch_count)
    math(EXPR libtorrent_last_patch "${libtorrent_patch_count} - 1")
    foreach(libtorrent_patch_index RANGE 0 ${libtorrent_last_patch})
        list(GET libtorrent_patches ${libtorrent_patch_index} libtorrent_patch)
        list(GET libtorrent_patch_files ${libtorrent_patch_index} libtorrent_patch_file)
        list(GET libtorrent_patch_sentinels ${libtorrent_patch_index} libtorrent_patch_sentinel)
        file(
            READ
            "${libtorrent_SOURCE_DIR}/${libtorrent_patch_file}"
            libtorrent_patch_source
        )
        string(
            FIND
            "${libtorrent_patch_source}"
            "${libtorrent_patch_sentinel}"
            libtorrent_patch_position
        )
        if(libtorrent_patch_position EQUAL -1)
            execute_process(
                COMMAND git apply --unidiff-zero --check "${libtorrent_patch}"
                WORKING_DIRECTORY "${libtorrent_SOURCE_DIR}"
                RESULT_VARIABLE libtorrent_patch_needed
                OUTPUT_QUIET
                ERROR_QUIET
            )
            if(NOT libtorrent_patch_needed EQUAL 0)
                get_filename_component(
                    libtorrent_patch_name
                    "${libtorrent_patch}"
                    NAME
                )
                message(FATAL_ERROR "Pinned libtorrent patch does not apply: ${libtorrent_patch_name}")
            endif()
            execute_process(
                COMMAND git apply --unidiff-zero "${libtorrent_patch}"
                WORKING_DIRECTORY "${libtorrent_SOURCE_DIR}"
                COMMAND_ERROR_IS_FATAL ANY
            )
        endif()
    endforeach()

    set(BUILD_SHARED_LIBS OFF)
    set(build_tests OFF)
    set(build_examples OFF)
    set(build_tools OFF)
    set(python-bindings OFF)
    set(developer-options OFF)
    set(logging OFF)
    add_subdirectory(
        "${libtorrent_SOURCE_DIR}"
        "${libtorrent_BINARY_DIR}"
        EXCLUDE_FROM_ALL
    )
    cmake_policy(POP)
endfunction()
