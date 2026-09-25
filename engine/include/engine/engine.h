#ifndef ENGINE_ENGINE_H
#define ENGINE_ENGINE_H

#include <stddef.h>
#include <stdint.h>

#include "engine/export.h"

#ifdef __cplusplus
extern "C" {
#endif

#define ENGINE_API_VERSION 4U

typedef struct engine engine;

typedef uint32_t engine_status;
enum engine_status_value {
    ENGINE_STATUS_OK = 0,
    ENGINE_STATUS_INVALID_ARGUMENT = 1,
    ENGINE_STATUS_INCOMPATIBLE_ABI = 2,
    ENGINE_STATUS_ALLOCATION_FAILED = 3,
    ENGINE_STATUS_INITIALIZATION_FAILED = 4,
    ENGINE_STATUS_BACKEND_UNAVAILABLE = 5,
    ENGINE_STATUS_QUEUE_FULL = 6,
    ENGINE_STATUS_NO_EVENT = 7,
    ENGINE_STATUS_NOT_FOUND = 8,
    ENGINE_STATUS_METADATA_NOT_READY = 9,
    ENGINE_STATUS_OUT_OF_RANGE = 10
};

typedef uint32_t engine_upload_mode;
enum engine_upload_mode_value {
    ENGINE_UPLOAD_DISABLED = 0,
    ENGINE_UPLOAD_UNLIMITED = 1,
    ENGINE_UPLOAD_LIMITED = 2
};

typedef uint32_t engine_torrent_profile;
enum engine_torrent_profile_value {
    ENGINE_TORRENT_PROFILE_SOFT = 0,
    ENGINE_TORRENT_PROFILE_BALANCED = 1,
    ENGINE_TORRENT_PROFILE_FAST = 2
};

typedef struct engine_config {
    uint32_t struct_size;
    const char* data_directory;
    const char* cache_directory;
    uint64_t memory_cache_capacity_bytes;
    uint64_t disk_cache_capacity_bytes;
    uint16_t listen_port;
    uint16_t reserved_0;
    engine_upload_mode upload_mode;
    uint64_t upload_limit_bytes_per_second;
    uint32_t stream_inactivity_timeout_milliseconds;
    uint32_t reserved_1;
    uint32_t warm_torrent_timeout_milliseconds;
    uint32_t reserved_2;
    const char* tls_ca_bundle_path;
    engine_torrent_profile torrent_profile;
    uint32_t reserved_3;
} engine_config;

typedef uint32_t engine_event_type;
enum engine_event_type_value {
    ENGINE_EVENT_TORRENT_ADDED = 1,
    ENGINE_EVENT_TORRENT_METADATA_READY = 2,
    ENGINE_EVENT_TORRENT_ERROR = 3,
    ENGINE_EVENT_STREAM_PREPARED = 4,
    ENGINE_EVENT_TORRENT_REMOVED = 5,
    ENGINE_EVENT_STREAM_STOPPED = 6,
    ENGINE_EVENT_DISK_CACHE_RECLAIMED = 7
};

typedef uint32_t engine_torrent_source_type;
enum engine_torrent_source_type_value {
    ENGINE_TORRENT_SOURCE_MAGNET = 0,
    ENGINE_TORRENT_SOURCE_DATA = 1
};

typedef struct engine_torrent_request {
    uint32_t struct_size;
    const char* magnet_uri;
    engine_torrent_source_type source_type;
    uint32_t reserved_0;
    const uint8_t* torrent_data;
    size_t torrent_data_size;
} engine_torrent_request;

typedef struct engine_event {
    uint32_t struct_size;
    engine_event_type type;
    uint64_t sequence;
    uint64_t request_id;
    uint64_t dropped_events;
    char torrent_id[65];
    char message[256];
    uint32_t file_index;
    uint32_t reserved_0;
    uint64_t file_size;
    char stream_id[65];
    char stream_url[512];
} engine_event;

typedef struct engine_stream_request {
    uint32_t struct_size;
    const char* torrent_id;
    uint32_t file_index;
    uint32_t reserved_0;
    const char* filename_hint;
} engine_stream_request;

typedef struct engine_file {
    uint32_t struct_size;
    uint32_t index;
    uint64_t offset;
    uint64_t size;
    uint8_t path_truncated;
    uint8_t reserved_0[7];
    char path[1024];
} engine_file;

typedef struct engine_stats {
    uint32_t struct_size;
    uint32_t active_torrents;
    uint32_t active_streams;
    uint32_t active_http_requests;
    uint32_t connected_peers;
    uint32_t connected_seeds;
    uint32_t pending_piece_reads;
    uint32_t reserved_0;
    uint64_t download_rate_bytes_per_second;
    uint64_t upload_rate_bytes_per_second;
    uint64_t total_payload_download_bytes;
    uint64_t total_payload_upload_bytes;
    uint64_t memory_cache_capacity_bytes;
    uint64_t memory_cache_used_bytes;
    uint64_t memory_cache_hits;
    uint64_t memory_cache_misses;
    uint64_t memory_cache_evictions;
    uint64_t memory_cache_entries;
    uint32_t warm_torrents;
    uint32_t quiesced_torrents;
    uint64_t disk_cache_capacity_bytes;
    uint64_t disk_cache_used_bytes;
    uint64_t disk_cache_protected_bytes;
    uint64_t disk_cache_evictions;
    uint64_t disk_cache_reclaimed_bytes;
    uint8_t disk_cache_over_budget;
    uint8_t reserved_1[7];
    uint32_t known_peers;
    uint32_t connect_candidates;
    uint32_t interested_peers;
    uint32_t unchoked_peers;
    uint32_t downloading_peers;
    uint32_t snubbed_peers;
    uint32_t pending_block_requests;
    uint32_t target_block_requests;
    uint32_t timed_out_block_requests;
    uint32_t connecting_peers;
    uint32_t handshaking_peers;
    uint32_t target_piece_peers;
    uint32_t target_piece_unchoked_peers;
    uint32_t target_piece_downloading_peers;
    uint32_t off_target_downloading_peers;
    uint32_t tracker_reply_events;
    uint32_t tracker_error_events;
    uint32_t dht_reply_events;
    uint32_t reserved_2;
    uint64_t tracker_peers_returned;
    uint64_t dht_peers_returned;
    uint64_t peer_connect_events;
    uint64_t peer_disconnect_events;
    uint64_t peer_disconnect_timeouts;
    uint64_t peer_disconnect_connect_failures;
    uint64_t peer_disconnect_redundant;
    uint64_t peer_disconnect_turnover;
    uint64_t peer_disconnect_other;
    uint64_t torrent_finished_events;
} engine_stats;

typedef struct engine_stream_stats {
    uint32_t struct_size;
    uint32_t file_index;
    uint64_t file_size;
    uint64_t contiguous_ready_bytes;
    uint64_t verified_file_bytes;
    uint64_t delivered_bytes;
    uint32_t active_demands;
    uint32_t scheduled_pieces;
    uint32_t blocking_pieces;
    uint32_t primary_blocking_piece;
    uint32_t secondary_blocking_piece;
    uint32_t last_ready_piece;
    uint64_t primary_demand_start;
    uint64_t primary_demand_end;
    uint64_t secondary_demand_start;
    uint64_t secondary_demand_end;
    uint64_t schedule_revision;
} engine_stream_stats;

typedef uint32_t engine_torrent_state;
enum engine_torrent_state_value {
    ENGINE_TORRENT_STATE_UNKNOWN = 0,
    ENGINE_TORRENT_STATE_CHECKING_FILES = 1,
    ENGINE_TORRENT_STATE_DOWNLOADING_METADATA = 2,
    ENGINE_TORRENT_STATE_DOWNLOADING = 3,
    ENGINE_TORRENT_STATE_FINISHED = 4,
    ENGINE_TORRENT_STATE_SEEDING = 5,
    ENGINE_TORRENT_STATE_CHECKING_RESUME_DATA = 6
};

/* Snapshot of one torrent, refreshed about once per second. Counts that the
 * swarm has not reported (scrape results, availability) are -1. */
typedef struct engine_torrent_details {
    uint32_t struct_size;
    engine_torrent_state state;
    char name[256];
    uint8_t name_truncated;
    uint8_t has_metadata;
    uint8_t reserved_0[6];
    char current_tracker[512];
    uint32_t piece_count;
    uint32_t piece_length;
    uint32_t pieces_have;
    uint32_t file_count;
    uint32_t progress_ppm;
    int32_t distributed_copies_milli;
    uint32_t connected_peers;
    uint32_t connected_seeds;
    uint32_t known_peers;
    uint32_t known_seeds;
    uint32_t connect_candidates;
    int32_t swarm_seeds;
    int32_t swarm_leechers;
    uint32_t peer_count;
    uint32_t tracker_count;
    uint32_t reserved_1;
    uint64_t total_size;
    uint64_t total_wanted;
    uint64_t total_wanted_done;
    uint64_t total_done;
    uint64_t download_rate_bytes_per_second;
    uint64_t upload_rate_bytes_per_second;
    uint64_t download_payload_rate_bytes_per_second;
    uint64_t upload_payload_rate_bytes_per_second;
    uint64_t session_payload_download_bytes;
    uint64_t session_payload_upload_bytes;
    uint64_t all_time_download_bytes;
    uint64_t all_time_upload_bytes;
    uint64_t failed_bytes;
    uint64_t redundant_bytes;
    int64_t added_time_unix_seconds;
    int64_t active_seconds;
    int64_t next_announce_seconds;
} engine_torrent_details;

enum engine_peer_flag_value {
    ENGINE_PEER_FLAG_SEED = 1u << 0,
    ENGINE_PEER_FLAG_INTERESTING = 1u << 1,
    ENGINE_PEER_FLAG_CHOKED = 1u << 2,
    ENGINE_PEER_FLAG_REMOTE_INTERESTED = 1u << 3,
    ENGINE_PEER_FLAG_REMOTE_CHOKED = 1u << 4,
    ENGINE_PEER_FLAG_SNUBBED = 1u << 5,
    ENGINE_PEER_FLAG_OPTIMISTIC_UNCHOKE = 1u << 6,
    ENGINE_PEER_FLAG_OUTGOING = 1u << 7,
    ENGINE_PEER_FLAG_ENCRYPTED = 1u << 8,
    ENGINE_PEER_FLAG_UTP = 1u << 9,
    ENGINE_PEER_FLAG_CONNECTING = 1u << 10,
    ENGINE_PEER_FLAG_HANDSHAKE = 1u << 11,
    ENGINE_PEER_FLAG_ON_PAROLE = 1u << 12,
    ENGINE_PEER_FLAG_UPLOAD_ONLY = 1u << 13,
    ENGINE_PEER_FLAG_ENDGAME = 1u << 14,
    ENGINE_PEER_FLAG_HOLEPUNCHED = 1u << 15,
    ENGINE_PEER_FLAG_WEB_SEED = 1u << 16
};

enum engine_peer_source_value {
    ENGINE_PEER_SOURCE_TRACKER = 1u << 0,
    ENGINE_PEER_SOURCE_DHT = 1u << 1,
    ENGINE_PEER_SOURCE_PEX = 1u << 2,
    ENGINE_PEER_SOURCE_LSD = 1u << 3,
    ENGINE_PEER_SOURCE_RESUME_DATA = 1u << 4,
    ENGINE_PEER_SOURCE_INCOMING = 1u << 5
};

typedef struct engine_peer {
    uint32_t struct_size;
    uint32_t flags;
    uint32_t source;
    uint32_t progress_ppm;
    char address[64];
    char client[64];
    uint64_t download_rate_bytes_per_second;
    uint64_t upload_rate_bytes_per_second;
    uint64_t total_download_bytes;
    uint64_t total_upload_bytes;
    uint32_t rtt_milliseconds;
    uint32_t download_queue_length;
    uint32_t hash_failures;
    int32_t downloading_piece;
} engine_peer;

typedef uint32_t engine_tracker_status;
enum engine_tracker_status_value {
    ENGINE_TRACKER_NOT_CONTACTED = 0,
    ENGINE_TRACKER_WORKING = 1,
    ENGINE_TRACKER_UPDATING = 2,
    ENGINE_TRACKER_ERROR = 3
};

typedef struct engine_tracker {
    uint32_t struct_size;
    uint32_t tier;
    engine_tracker_status status;
    int32_t seeds;
    int32_t leechers;
    int32_t downloaded;
    uint32_t failures;
    uint32_t reserved_0;
    int64_t next_announce_seconds;
    char url[512];
    char message[256];
} engine_tracker;

typedef uint8_t engine_piece_state;
enum engine_piece_state_value {
    ENGINE_PIECE_MISSING = 0,
    ENGINE_PIECE_HAVE = 1,
    ENGINE_PIECE_DOWNLOADING = 2,
    ENGINE_PIECE_BLOCKING = 3,
    /* Have, and this device has sent it to at least one peer. */
    ENGINE_PIECE_SEEDED = 4
};

ENGINE_API uint32_t engine_api_version(void);
ENGINE_API const char* engine_version_string(void);
ENGINE_API const char* engine_protocol_backend_version(void);
ENGINE_API const char* engine_status_message(engine_status status);
ENGINE_API void engine_config_init_sized(
    engine_config* config,
    uint32_t struct_size
);
ENGINE_API void engine_torrent_request_init_sized(
    engine_torrent_request* request,
    uint32_t struct_size
);
ENGINE_API void engine_event_init_sized(
    engine_event* event,
    uint32_t struct_size
);
ENGINE_API void engine_file_init_sized(
    engine_file* file,
    uint32_t struct_size
);
ENGINE_API void engine_stream_request_init_sized(
    engine_stream_request* request,
    uint32_t struct_size
);
ENGINE_API void engine_stats_init_sized(
    engine_stats* stats,
    uint32_t struct_size
);
ENGINE_API void engine_stream_stats_init_sized(
    engine_stream_stats* stats,
    uint32_t struct_size
);
ENGINE_API engine_status engine_create(
    const engine_config* config,
    engine** engine
);
ENGINE_API void engine_destroy(engine* engine);
ENGINE_API engine_status engine_add_torrent(
    engine* engine,
    const engine_torrent_request* request,
    uint64_t* request_id
);
ENGINE_API engine_status engine_poll_event(
    engine* engine,
    engine_event* event
);
ENGINE_API engine_status engine_get_file_count(
    engine* engine,
    const char* torrent_id,
    size_t* file_count
);
ENGINE_API engine_status engine_get_file(
    engine* engine,
    const char* torrent_id,
    size_t file_index,
    engine_file* file
);
ENGINE_API engine_status engine_prepare_stream(
    engine* engine,
    const engine_stream_request* request,
    uint64_t* request_id
);
ENGINE_API engine_status engine_remove_torrent(
    engine* engine,
    const char* torrent_id,
    uint64_t* request_id
);
ENGINE_API engine_status engine_stop_stream(
    engine* engine,
    const char* stream_id,
    uint64_t* request_id
);
ENGINE_API engine_status engine_get_stats(
    engine* engine,
    engine_stats* stats
);
ENGINE_API engine_status engine_get_stream_stats(
    engine* engine,
    const char* stream_id,
    engine_stream_stats* stats
);
ENGINE_API engine_status engine_reclaim_disk_cache(
    engine* engine,
    uint64_t target_bytes,
    uint64_t* request_id
);
/* Changes whether and how fast the engine uploads, effective immediately. With
 * ENGINE_UPLOAD_DISABLED no block is sent to any peer. */
ENGINE_API engine_status engine_set_upload_mode(
    engine* engine,
    engine_upload_mode upload_mode,
    uint64_t upload_limit_bytes_per_second
);
/* Sets the stream's video duration so the engine keeps minutes, not bytes, around the playhead. */
ENGINE_API engine_status engine_set_stream_duration(
    engine* engine,
    const char* stream_id,
    uint64_t duration_milliseconds
);
ENGINE_API void engine_torrent_details_init_sized(
    engine_torrent_details* details,
    uint32_t struct_size
);
ENGINE_API engine_status engine_get_torrent_details(
    engine* engine,
    const char* torrent_id,
    engine_torrent_details* details
);
/* The list getters copy up to `capacity` entries laid out `element_size`
 * bytes apart and set `*count` to the number available, which may exceed
 * `capacity`; pass a capacity of 0 to query the count alone. */
ENGINE_API engine_status engine_get_peers(
    engine* engine,
    const char* torrent_id,
    engine_peer* peers,
    uint32_t element_size,
    size_t capacity,
    size_t* count
);
ENGINE_API engine_status engine_get_trackers(
    engine* engine,
    const char* torrent_id,
    engine_tracker* trackers,
    uint32_t element_size,
    size_t capacity,
    size_t* count
);
/* One engine_piece_state per piece, and how many connected peers have it
 * (capped at 255). Either output may be null. */
ENGINE_API engine_status engine_get_piece_map(
    engine* engine,
    const char* torrent_id,
    uint8_t* states,
    uint8_t* availability,
    size_t capacity,
    size_t* count
);

#define engine_config_init(value) \
    engine_config_init_sized((value), (uint32_t)sizeof(*(value)))
#define engine_torrent_request_init(value) \
    engine_torrent_request_init_sized((value), (uint32_t)sizeof(*(value)))
#define engine_event_init(value) \
    engine_event_init_sized((value), (uint32_t)sizeof(*(value)))
#define engine_file_init(value) \
    engine_file_init_sized((value), (uint32_t)sizeof(*(value)))
#define engine_stream_request_init(value) \
    engine_stream_request_init_sized((value), (uint32_t)sizeof(*(value)))
#define engine_stats_init(value) \
    engine_stats_init_sized((value), (uint32_t)sizeof(*(value)))
#define engine_stream_stats_init(value) \
    engine_stream_stats_init_sized((value), (uint32_t)sizeof(*(value)))
#define engine_torrent_details_init(value) \
    engine_torrent_details_init_sized((value), (uint32_t)sizeof(*(value)))

#ifdef __cplusplus
}
#endif

#endif
