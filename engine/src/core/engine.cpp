#include "engine/engine.h"
#include "core/engine_runtime.hpp"
#include "torrent/protocol_backend.hpp"

#include <algorithm>
#include <cctype>
#include <cstring>
#include <limits>
#include <memory>
#include <new>
#include <optional>
#include <string>
#include <vector>

#if defined(ENGINE_HAS_LIBTORRENT)
const char* libtorrent_version_string();
#endif

struct engine {
    std::string data_directory;
    std::string cache_directory;
    std::uint64_t memory_cache_capacity_bytes;
    std::uint64_t disk_cache_capacity_bytes;
    std::uint16_t listen_port;
    engine_upload_mode upload_mode;
    std::uint64_t upload_limit_bytes_per_second;
    std::uint32_t stream_inactivity_timeout_milliseconds;
    std::uint32_t warm_torrent_timeout_milliseconds;
    engine_torrent_profile torrent_profile;
    std::unique_ptr<core::EngineRuntime> runtime;
};

namespace {

constexpr std::uint64_t default_memory_cache_capacity = 64ULL * 1024ULL * 1024ULL;
constexpr std::uint64_t default_disk_cache_capacity = 2ULL * 1024ULL * 1024ULL * 1024ULL;
constexpr std::uint32_t default_stream_inactivity_timeout_milliseconds = 30'000;
constexpr std::uint32_t default_warm_torrent_timeout_milliseconds = 60'000;
constexpr engine_torrent_profile default_torrent_profile =
    ENGINE_TORRENT_PROFILE_BALANCED;
constexpr std::size_t config_v1_size =
    offsetof(engine_config, upload_limit_bytes_per_second) + sizeof(std::uint64_t);
constexpr std::size_t config_v2_size =
    offsetof(engine_config, reserved_1) + sizeof(std::uint32_t);
constexpr std::size_t config_v3_size =
    offsetof(engine_config, reserved_2) + sizeof(std::uint32_t);
constexpr std::size_t config_v4_size =
    offsetof(engine_config, tls_ca_bundle_path) + sizeof(const char*);
constexpr std::size_t config_v5_size =
    offsetof(engine_config, reserved_3) + sizeof(std::uint32_t);
constexpr std::size_t torrent_request_v1_size =
    offsetof(engine_torrent_request, magnet_uri) + sizeof(const char*);
constexpr std::size_t torrent_request_v2_size =
    offsetof(engine_torrent_request, torrent_data_size) + sizeof(std::size_t);
constexpr std::size_t stream_request_v1_size =
    offsetof(engine_stream_request, filename_hint) + sizeof(const char*);
constexpr std::size_t event_v1_size =
    offsetof(engine_event, message) + sizeof(engine_event{}.message);
constexpr std::size_t file_v1_size =
    offsetof(engine_file, path) + sizeof(engine_file{}.path);
constexpr std::size_t stats_v1_size =
    offsetof(engine_stats, memory_cache_entries) + sizeof(std::uint64_t);
constexpr std::size_t stream_stats_v1_size =
    offsetof(engine_stream_stats, delivered_bytes) + sizeof(std::uint64_t);
constexpr std::size_t torrent_details_v1_size =
    offsetof(engine_torrent_details, next_announce_seconds) + sizeof(std::int64_t);
constexpr std::size_t peer_v1_size =
    offsetof(engine_peer, downloading_piece) + sizeof(std::int32_t);
constexpr std::size_t tracker_v1_size =
    offsetof(engine_tracker, message) + sizeof(engine_tracker{}.message);

template <std::size_t Size>
void copy_text(char (&destination)[Size], const std::string& source) {
    const auto length = std::min(source.size(), Size - 1);
    std::memcpy(destination, source.data(), length);
    destination[length] = '\0';
}

// Writes each snapshot entry into a caller array whose elements may be an older,
// shorter revision of the structure, so the stride is the caller's element size.
template <typename Structure, typename Source, typename Convert>
void copy_elements(
    Structure* const output,
    const std::uint32_t element_size,
    const std::size_t capacity,
    const std::vector<Source>& source,
    Convert convert
) {
    const auto copied_size = std::min<std::size_t>(element_size, sizeof(Structure));
    auto* const bytes = reinterpret_cast<unsigned char*>(output);
    const auto count = std::min(capacity, source.size());
    for (std::size_t index = 0; index < count; ++index) {
        Structure element{};
        convert(source[index], element);
        element.struct_size = static_cast<std::uint32_t>(copied_size);
        auto* const destination = bytes + index * element_size;
        std::memset(destination, 0, element_size);
        std::memcpy(destination, &element, copied_size);
    }
}

template <typename Structure>
bool initialize_structure(Structure* const value, const std::uint32_t struct_size) {
    if (value == nullptr) {
        return false;
    }
    std::memset(value, 0, struct_size);
    if (struct_size < sizeof(std::uint32_t)) {
        return false;
    }
    value->struct_size = struct_size;
    return true;
}

bool valid_upload_configuration(const engine_config& config) {
    switch (config.upload_mode) {
    case ENGINE_UPLOAD_DISABLED:
    case ENGINE_UPLOAD_UNLIMITED:
        return config.upload_limit_bytes_per_second == 0;
    case ENGINE_UPLOAD_LIMITED:
        return config.upload_limit_bytes_per_second > 0;
    }
    return false;
}

bool valid_torrent_profile(const engine_torrent_profile profile) {
    switch (profile) {
    case ENGINE_TORRENT_PROFILE_SOFT:
    case ENGINE_TORRENT_PROFILE_BALANCED:
    case ENGINE_TORRENT_PROFILE_FAST:
        return true;
    }
    return false;
}

std::size_t bounded_string_length(const char* const text, const std::size_t maximum) {
    std::size_t length = 0;
    while (length <= maximum && text[length] != '\0') {
        ++length;
    }
    return length;
}

std::optional<std::string> normalize_torrent_id(const char* const torrent_id) {
    if (torrent_id == nullptr) {
        return std::nullopt;
    }
    const auto length = bounded_string_length(torrent_id, 64);
    if (length != 40 && length != 64) {
        return std::nullopt;
    }
    std::string normalized(torrent_id, length);
    for (auto& character : normalized) {
        const auto value = static_cast<unsigned char>(character);
        if (!std::isxdigit(value)) {
            return std::nullopt;
        }
        character = static_cast<char>(std::tolower(value));
    }
    return normalized;
}

std::optional<std::string> normalize_stream_id(const char* const stream_id) {
    if (stream_id == nullptr) {
        return std::nullopt;
    }
    const auto length = bounded_string_length(stream_id, 64);
    if (length != 64) {
        return std::nullopt;
    }
    std::string normalized(stream_id, length);
    for (auto& character : normalized) {
        const auto value = static_cast<unsigned char>(character);
        if (!std::isxdigit(value)) {
            return std::nullopt;
        }
        character = static_cast<char>(std::tolower(value));
    }
    return normalized;
}

}

std::uint32_t engine_api_version() {
    return ENGINE_API_VERSION;
}

const char* engine_version_string() {
    return "0.1.1";
}

const char* engine_protocol_backend_version() {
#if defined(ENGINE_HAS_LIBTORRENT)
    return libtorrent_version_string();
#else
    return "unavailable";
#endif
}

const char* engine_status_message(const engine_status status) {
    switch (status) {
    case ENGINE_STATUS_OK:
        return "ok";
    case ENGINE_STATUS_INVALID_ARGUMENT:
        return "invalid argument";
    case ENGINE_STATUS_INCOMPATIBLE_ABI:
        return "incompatible ABI";
    case ENGINE_STATUS_ALLOCATION_FAILED:
        return "allocation failed";
    case ENGINE_STATUS_INITIALIZATION_FAILED:
        return "initialization failed";
    case ENGINE_STATUS_BACKEND_UNAVAILABLE:
        return "protocol backend unavailable";
    case ENGINE_STATUS_QUEUE_FULL:
        return "command queue full";
    case ENGINE_STATUS_NO_EVENT:
        return "no event available";
    case ENGINE_STATUS_NOT_FOUND:
        return "torrent not found";
    case ENGINE_STATUS_METADATA_NOT_READY:
        return "torrent metadata not ready";
    case ENGINE_STATUS_OUT_OF_RANGE:
        return "index out of range";
    }
    return "unknown status";
}

void engine_config_init_sized(
    engine_config* const config,
    const std::uint32_t struct_size
) {
    if (!initialize_structure(config, struct_size)) {
        return;
    }
    if (struct_size < config_v1_size) {
        return;
    }
    config->memory_cache_capacity_bytes = default_memory_cache_capacity;
    config->disk_cache_capacity_bytes = default_disk_cache_capacity;
    // Uploading is opt-in: a caller that sets nothing never seeds.
    config->upload_mode = ENGINE_UPLOAD_DISABLED;
    if (struct_size >= config_v2_size) {
        config->stream_inactivity_timeout_milliseconds =
            default_stream_inactivity_timeout_milliseconds;
    }
    if (struct_size >= config_v3_size) {
        config->warm_torrent_timeout_milliseconds =
            default_warm_torrent_timeout_milliseconds;
    }
    if (struct_size >= config_v5_size) {
        config->torrent_profile = default_torrent_profile;
    }
}

void engine_torrent_request_init_sized(
    engine_torrent_request* const request,
    const std::uint32_t struct_size
) {
    initialize_structure(request, struct_size);
}

void engine_event_init_sized(
    engine_event* const event,
    const std::uint32_t struct_size
) {
    if (!initialize_structure(event, struct_size)) {
        return;
    }
    constexpr auto file_index_size =
        offsetof(engine_event, file_index) + sizeof(std::uint32_t);
    if (struct_size >= file_index_size) {
        event->file_index = std::numeric_limits<std::uint32_t>::max();
    }
}

void engine_file_init_sized(
    engine_file* const file,
    const std::uint32_t struct_size
) {
    initialize_structure(file, struct_size);
}

void engine_stream_request_init_sized(
    engine_stream_request* const request,
    const std::uint32_t struct_size
) {
    if (!initialize_structure(request, struct_size)) {
        return;
    }
    constexpr auto file_index_size =
        offsetof(engine_stream_request, file_index) + sizeof(std::uint32_t);
    if (struct_size >= file_index_size) {
        request->file_index = std::numeric_limits<std::uint32_t>::max();
    }
}

void engine_stats_init_sized(
    engine_stats* const stats,
    const std::uint32_t struct_size
) {
    initialize_structure(stats, struct_size);
}

void engine_stream_stats_init_sized(
    engine_stream_stats* const stats,
    const std::uint32_t struct_size
) {
    initialize_structure(stats, struct_size);
}

engine_status engine_create(
    const engine_config* const config,
    engine** const engine
) {
    if (engine == nullptr) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    *engine = nullptr;
    if (config == nullptr) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    if (config->struct_size < config_v1_size) {
        return ENGINE_STATUS_INCOMPATIBLE_ABI;
    }
    if (config->data_directory == nullptr || config->cache_directory == nullptr) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    if (config->data_directory[0] == '\0' || config->cache_directory[0] == '\0') {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    if (!valid_upload_configuration(*config)) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    const auto torrent_profile = config->struct_size >= config_v5_size
        ? config->torrent_profile
        : default_torrent_profile;
    if (!valid_torrent_profile(torrent_profile)) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }

    try {
        const auto stream_inactivity_timeout = config->struct_size >= config_v2_size
            ? config->stream_inactivity_timeout_milliseconds
            : default_stream_inactivity_timeout_milliseconds;
        const auto warm_torrent_timeout = config->struct_size >= config_v3_size
            ? config->warm_torrent_timeout_milliseconds
            : default_warm_torrent_timeout_milliseconds;
        std::string tls_ca_bundle_path;
        if (config->struct_size >= config_v4_size && config->tls_ca_bundle_path != nullptr) {
            constexpr std::size_t maximum_path_length = 16 * 1024;
            const auto length = bounded_string_length(
                config->tls_ca_bundle_path,
                maximum_path_length
            );
            if (length == 0 || length > maximum_path_length) {
                return ENGINE_STATUS_INVALID_ARGUMENT;
            }
            tls_ca_bundle_path.assign(config->tls_ca_bundle_path, length);
        }
        *engine = new ::engine{
            config->data_directory,
            config->cache_directory,
            config->memory_cache_capacity_bytes,
            config->disk_cache_capacity_bytes,
            config->listen_port,
            config->upload_mode,
            config->upload_limit_bytes_per_second,
            stream_inactivity_timeout,
            warm_torrent_timeout,
            torrent_profile,
            std::make_unique<core::EngineRuntime>(
                torrent::create_protocol_backend({
                    config->upload_mode,
                    config->upload_limit_bytes_per_second,
                    config->data_directory,
                    config->cache_directory,
                    config->listen_port,
                    config->memory_cache_capacity_bytes,
                    config->disk_cache_capacity_bytes,
                    stream_inactivity_timeout,
                    warm_torrent_timeout,
                    torrent_profile,
                    std::move(tls_ca_bundle_path),
                }),
                config->cache_directory
            ),
        };
    } catch (const std::bad_alloc&) {
        return ENGINE_STATUS_ALLOCATION_FAILED;
    } catch (...) {
        return ENGINE_STATUS_INITIALIZATION_FAILED;
    }
    return ENGINE_STATUS_OK;
}

void engine_destroy(engine* const engine) {
    delete engine;
}

engine_status engine_add_torrent(
    engine* const engine,
    const engine_torrent_request* const request,
    std::uint64_t* const request_id
) {
    if (engine == nullptr || request == nullptr || request_id == nullptr) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    *request_id = 0;
    if (request->struct_size < torrent_request_v1_size) {
        return ENGINE_STATUS_INCOMPATIBLE_ABI;
    }
    try {
        torrent::TorrentInput input{};
        const auto has_v2_fields = request->struct_size >= torrent_request_v2_size;
        const auto source_type = has_v2_fields
            ? request->source_type
            : static_cast<engine_torrent_source_type>(
                  ENGINE_TORRENT_SOURCE_MAGNET
              );
        if (source_type == ENGINE_TORRENT_SOURCE_MAGNET) {
            if (request->magnet_uri == nullptr) {
                return ENGINE_STATUS_INVALID_ARGUMENT;
            }
            constexpr std::size_t maximum_magnet_length = 16 * 1024;
            const auto length = bounded_string_length(
                request->magnet_uri,
                maximum_magnet_length
            );
            if (length == 0 || length > maximum_magnet_length) {
                return ENGINE_STATUS_INVALID_ARGUMENT;
            }
            input.type = torrent::TorrentInputType::magnet;
            input.magnet_uri.assign(request->magnet_uri, length);
        } else if (source_type == ENGINE_TORRENT_SOURCE_DATA) {
            constexpr std::size_t maximum_torrent_size = 4 * 1024 * 1024;
            if (!has_v2_fields || request->torrent_data == nullptr ||
                request->torrent_data_size == 0 ||
                request->torrent_data_size > maximum_torrent_size) {
                return ENGINE_STATUS_INVALID_ARGUMENT;
            }
            input.type = torrent::TorrentInputType::torrent_data;
            const auto* begin = reinterpret_cast<const char*>(request->torrent_data);
            input.torrent_data.assign(begin, begin + request->torrent_data_size);
        } else {
            return ENGINE_STATUS_INVALID_ARGUMENT;
        }
        return engine->runtime->add_torrent(std::move(input), *request_id);
    } catch (const std::bad_alloc&) {
        return ENGINE_STATUS_ALLOCATION_FAILED;
    } catch (...) {
        return ENGINE_STATUS_INITIALIZATION_FAILED;
    }
}

engine_status engine_get_file_count(
    engine* const engine,
    const char* const torrent_id,
    std::size_t* const file_count
) {
    if (engine == nullptr || file_count == nullptr) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    *file_count = 0;
    try {
        const auto normalized = normalize_torrent_id(torrent_id);
        if (!normalized.has_value()) {
            return ENGINE_STATUS_INVALID_ARGUMENT;
        }
        return engine->runtime->get_file_count(*normalized, *file_count);
    } catch (const std::bad_alloc&) {
        return ENGINE_STATUS_ALLOCATION_FAILED;
    } catch (...) {
        return ENGINE_STATUS_INITIALIZATION_FAILED;
    }
}

engine_status engine_get_file(
    engine* const engine,
    const char* const torrent_id,
    const std::size_t file_index,
    engine_file* const file
) {
    if (engine == nullptr || file == nullptr) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    const auto caller_size = static_cast<std::size_t>(file->struct_size);
    if (caller_size < file_v1_size) {
        return ENGINE_STATUS_INCOMPATIBLE_ABI;
    }
    try {
        const auto normalized = normalize_torrent_id(torrent_id);
        if (!normalized.has_value()) {
            return ENGINE_STATUS_INVALID_ARGUMENT;
        }
        engine_file available_file{};
        const auto status = engine->runtime->get_file(
            *normalized,
            file_index,
            available_file
        );
        if (status != ENGINE_STATUS_OK) {
            return status;
        }
        std::memcpy(file, &available_file, std::min(caller_size, sizeof(available_file)));
        return ENGINE_STATUS_OK;
    } catch (const std::bad_alloc&) {
        return ENGINE_STATUS_ALLOCATION_FAILED;
    } catch (...) {
        return ENGINE_STATUS_INITIALIZATION_FAILED;
    }
}

engine_status engine_prepare_stream(
    engine* const engine,
    const engine_stream_request* const request,
    std::uint64_t* const request_id
) {
    if (engine == nullptr || request == nullptr || request_id == nullptr) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    *request_id = 0;
    if (request->struct_size < stream_request_v1_size) {
        return ENGINE_STATUS_INCOMPATIBLE_ABI;
    }
    try {
        const auto normalized = normalize_torrent_id(request->torrent_id);
        if (!normalized.has_value()) {
            return ENGINE_STATUS_INVALID_ARGUMENT;
        }
        std::string filename_hint;
        if (request->filename_hint != nullptr) {
            constexpr std::size_t maximum_hint_length = 4 * 1024;
            const auto length = bounded_string_length(
                request->filename_hint,
                maximum_hint_length
            );
            if (length > maximum_hint_length) {
                return ENGINE_STATUS_INVALID_ARGUMENT;
            }
            filename_hint.assign(request->filename_hint, length);
        }
        const auto requested_index = request->file_index ==
                std::numeric_limits<std::uint32_t>::max()
            ? std::optional<std::size_t>{}
            : std::optional<std::size_t>{request->file_index};
        return engine->runtime->prepare_stream(
            *normalized,
            requested_index,
            std::move(filename_hint),
            *request_id
        );
    } catch (const std::bad_alloc&) {
        return ENGINE_STATUS_ALLOCATION_FAILED;
    } catch (...) {
        return ENGINE_STATUS_INITIALIZATION_FAILED;
    }
}

engine_status engine_remove_torrent(
    engine* const engine,
    const char* const torrent_id,
    std::uint64_t* const request_id
) {
    if (engine == nullptr || request_id == nullptr) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    *request_id = 0;
    try {
        const auto normalized = normalize_torrent_id(torrent_id);
        if (!normalized.has_value()) {
            return ENGINE_STATUS_INVALID_ARGUMENT;
        }
        return engine->runtime->remove_torrent(*normalized, *request_id);
    } catch (const std::bad_alloc&) {
        return ENGINE_STATUS_ALLOCATION_FAILED;
    } catch (...) {
        return ENGINE_STATUS_INITIALIZATION_FAILED;
    }
}

engine_status engine_poll_event(
    engine* const engine,
    engine_event* const event
) {
    if (engine == nullptr || event == nullptr) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    const auto caller_size = static_cast<std::size_t>(event->struct_size);
    if (caller_size < event_v1_size) {
        return ENGINE_STATUS_INCOMPATIBLE_ABI;
    }
    engine_event available_event{};
    const auto status = engine->runtime->poll_event(available_event);
    if (status != ENGINE_STATUS_OK) {
        return status;
    }
    std::memcpy(event, &available_event, std::min(caller_size, sizeof(available_event)));
    return ENGINE_STATUS_OK;
}

engine_status engine_stop_stream(
    engine* const engine,
    const char* const stream_id,
    std::uint64_t* const request_id
) {
    if (engine == nullptr || request_id == nullptr) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    *request_id = 0;
    try {
        const auto normalized = normalize_stream_id(stream_id);
        if (!normalized.has_value()) {
            return ENGINE_STATUS_INVALID_ARGUMENT;
        }
        return engine->runtime->stop_stream(*normalized, *request_id);
    } catch (const std::bad_alloc&) {
        return ENGINE_STATUS_ALLOCATION_FAILED;
    } catch (...) {
        return ENGINE_STATUS_INITIALIZATION_FAILED;
    }
}

engine_status engine_set_stream_duration(
    engine* const engine,
    const char* const stream_id,
    const std::uint64_t duration_milliseconds
) {
    if (engine == nullptr) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    try {
        const auto normalized = normalize_stream_id(stream_id);
        if (!normalized.has_value()) {
            return ENGINE_STATUS_INVALID_ARGUMENT;
        }
        return engine->runtime->set_stream_duration(*normalized, duration_milliseconds);
    } catch (const std::bad_alloc&) {
        return ENGINE_STATUS_ALLOCATION_FAILED;
    } catch (...) {
        return ENGINE_STATUS_INITIALIZATION_FAILED;
    }
}

engine_status engine_get_stats(
    engine* const engine,
    engine_stats* const stats
) {
    if (engine == nullptr || stats == nullptr) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    const auto caller_size = static_cast<std::size_t>(stats->struct_size);
    if (caller_size < stats_v1_size) {
        return ENGINE_STATUS_INCOMPATIBLE_ABI;
    }
    try {
        const auto snapshot = engine->runtime->get_stats();
        std::memcpy(stats, &snapshot, std::min(caller_size, sizeof(snapshot)));
        return ENGINE_STATUS_OK;
    } catch (...) {
        return ENGINE_STATUS_INITIALIZATION_FAILED;
    }
}

engine_status engine_get_stream_stats(
    engine* const engine,
    const char* const stream_id,
    engine_stream_stats* const stats
) {
    if (engine == nullptr || stats == nullptr) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    const auto caller_size = static_cast<std::size_t>(stats->struct_size);
    if (caller_size < stream_stats_v1_size) {
        return ENGINE_STATUS_INCOMPATIBLE_ABI;
    }
    try {
        const auto normalized = normalize_stream_id(stream_id);
        if (!normalized.has_value()) {
            return ENGINE_STATUS_INVALID_ARGUMENT;
        }
        engine_stream_stats snapshot{};
        const auto status = engine->runtime->get_stream_stats(*normalized, snapshot);
        if (status != ENGINE_STATUS_OK) {
            return status;
        }
        std::memcpy(stats, &snapshot, std::min(caller_size, sizeof(snapshot)));
        return ENGINE_STATUS_OK;
    } catch (...) {
        return ENGINE_STATUS_INITIALIZATION_FAILED;
    }
}

engine_status engine_reclaim_disk_cache(
    engine* const engine,
    const std::uint64_t target_bytes,
    std::uint64_t* const request_id
) {
    if (engine == nullptr || request_id == nullptr) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    *request_id = 0;
    try {
        return engine->runtime->reclaim_disk_cache(target_bytes, *request_id);
    } catch (const std::bad_alloc&) {
        return ENGINE_STATUS_ALLOCATION_FAILED;
    } catch (...) {
        return ENGINE_STATUS_INITIALIZATION_FAILED;
    }
}

engine_status engine_set_upload_mode(
    engine* const engine,
    const engine_upload_mode upload_mode,
    const std::uint64_t upload_limit_bytes_per_second
) {
    if (engine == nullptr) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    engine_config config{};
    config.upload_mode = upload_mode;
    config.upload_limit_bytes_per_second = upload_limit_bytes_per_second;
    if (!valid_upload_configuration(config)) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    try {
        return engine->runtime->set_upload_mode(upload_mode, upload_limit_bytes_per_second);
    } catch (const std::bad_alloc&) {
        return ENGINE_STATUS_ALLOCATION_FAILED;
    } catch (...) {
        return ENGINE_STATUS_INITIALIZATION_FAILED;
    }
}

void engine_torrent_details_init_sized(
    engine_torrent_details* const details,
    const std::uint32_t struct_size
) {
    initialize_structure(details, struct_size);
}

engine_status engine_get_torrent_details(
    engine* const engine,
    const char* const torrent_id,
    engine_torrent_details* const details
) {
    if (engine == nullptr || details == nullptr) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    const auto caller_size = static_cast<std::size_t>(details->struct_size);
    if (caller_size < torrent_details_v1_size) {
        return ENGINE_STATUS_INCOMPATIBLE_ABI;
    }
    try {
        const auto normalized = normalize_torrent_id(torrent_id);
        if (!normalized.has_value()) {
            return ENGINE_STATUS_INVALID_ARGUMENT;
        }
        engine_torrent_details snapshot{};
        const auto status = engine->runtime->get_torrent_details(*normalized, snapshot);
        if (status != ENGINE_STATUS_OK) {
            return status;
        }
        snapshot.struct_size = static_cast<std::uint32_t>(
            std::min(caller_size, sizeof(snapshot))
        );
        std::memcpy(details, &snapshot, std::min(caller_size, sizeof(snapshot)));
        return ENGINE_STATUS_OK;
    } catch (const std::bad_alloc&) {
        return ENGINE_STATUS_ALLOCATION_FAILED;
    } catch (...) {
        return ENGINE_STATUS_INITIALIZATION_FAILED;
    }
}

engine_status engine_get_peers(
    engine* const engine,
    const char* const torrent_id,
    engine_peer* const peers,
    const std::uint32_t element_size,
    const std::size_t capacity,
    std::size_t* const count
) {
    if (engine == nullptr || count == nullptr || (capacity > 0 && peers == nullptr)) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    *count = 0;
    if (capacity > 0 && element_size < peer_v1_size) {
        return ENGINE_STATUS_INCOMPATIBLE_ABI;
    }
    try {
        const auto normalized = normalize_torrent_id(torrent_id);
        if (!normalized.has_value()) {
            return ENGINE_STATUS_INVALID_ARGUMENT;
        }
        std::vector<torrent::PeerDetails> snapshot;
        const auto status = engine->runtime->get_peers(*normalized, snapshot);
        if (status != ENGINE_STATUS_OK) {
            return status;
        }
        copy_elements(peers, element_size, capacity, snapshot, [](const auto& source, engine_peer& peer) {
            peer.flags = source.flags;
            peer.source = source.source;
            peer.progress_ppm = source.progress_ppm;
            copy_text(peer.address, source.address);
            copy_text(peer.client, source.client);
            peer.download_rate_bytes_per_second = source.download_rate_bytes_per_second;
            peer.upload_rate_bytes_per_second = source.upload_rate_bytes_per_second;
            peer.total_download_bytes = source.total_download_bytes;
            peer.total_upload_bytes = source.total_upload_bytes;
            peer.rtt_milliseconds = source.rtt_milliseconds;
            peer.download_queue_length = source.download_queue_length;
            peer.hash_failures = source.hash_failures;
            peer.downloading_piece = source.downloading_piece;
        });
        *count = snapshot.size();
        return ENGINE_STATUS_OK;
    } catch (const std::bad_alloc&) {
        return ENGINE_STATUS_ALLOCATION_FAILED;
    } catch (...) {
        return ENGINE_STATUS_INITIALIZATION_FAILED;
    }
}

engine_status engine_get_trackers(
    engine* const engine,
    const char* const torrent_id,
    engine_tracker* const trackers,
    const std::uint32_t element_size,
    const std::size_t capacity,
    std::size_t* const count
) {
    if (engine == nullptr || count == nullptr || (capacity > 0 && trackers == nullptr)) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    *count = 0;
    if (capacity > 0 && element_size < tracker_v1_size) {
        return ENGINE_STATUS_INCOMPATIBLE_ABI;
    }
    try {
        const auto normalized = normalize_torrent_id(torrent_id);
        if (!normalized.has_value()) {
            return ENGINE_STATUS_INVALID_ARGUMENT;
        }
        std::vector<torrent::TrackerDetails> snapshot;
        const auto status = engine->runtime->get_trackers(*normalized, snapshot);
        if (status != ENGINE_STATUS_OK) {
            return status;
        }
        copy_elements(
            trackers,
            element_size,
            capacity,
            snapshot,
            [](const auto& source, engine_tracker& tracker) {
                tracker.tier = source.tier;
                tracker.status = source.status;
                tracker.seeds = source.seeds;
                tracker.leechers = source.leechers;
                tracker.downloaded = source.downloaded;
                tracker.failures = source.failures;
                tracker.next_announce_seconds = source.next_announce_seconds;
                copy_text(tracker.url, source.url);
                copy_text(tracker.message, source.message);
            }
        );
        *count = snapshot.size();
        return ENGINE_STATUS_OK;
    } catch (const std::bad_alloc&) {
        return ENGINE_STATUS_ALLOCATION_FAILED;
    } catch (...) {
        return ENGINE_STATUS_INITIALIZATION_FAILED;
    }
}

engine_status engine_get_piece_map(
    engine* const engine,
    const char* const torrent_id,
    std::uint8_t* const states,
    std::uint8_t* const availability,
    const std::size_t capacity,
    std::size_t* const count
) {
    if (engine == nullptr || count == nullptr) {
        return ENGINE_STATUS_INVALID_ARGUMENT;
    }
    *count = 0;
    try {
        const auto normalized = normalize_torrent_id(torrent_id);
        if (!normalized.has_value()) {
            return ENGINE_STATUS_INVALID_ARGUMENT;
        }
        std::vector<std::uint8_t> piece_states;
        std::vector<std::uint8_t> piece_availability;
        const auto status = engine->runtime->get_piece_map(
            *normalized,
            piece_states,
            piece_availability
        );
        if (status != ENGINE_STATUS_OK) {
            return status;
        }
        const auto copied = std::min(capacity, piece_states.size());
        if (states != nullptr && copied > 0) {
            std::memcpy(states, piece_states.data(), copied);
        }
        if (availability != nullptr && copied > 0) {
            const auto available = std::min(copied, piece_availability.size());
            std::memcpy(availability, piece_availability.data(), available);
            std::memset(availability + available, 0, copied - available);
        }
        *count = piece_states.size();
        return ENGINE_STATUS_OK;
    } catch (const std::bad_alloc&) {
        return ENGINE_STATUS_ALLOCATION_FAILED;
    } catch (...) {
        return ENGINE_STATUS_INITIALIZATION_FAILED;
    }
}
