#ifndef ENGINE_PROTOCOL_BACKEND_HPP
#define ENGINE_PROTOCOL_BACKEND_HPP

#include <cstdint>
#include <limits>
#include <memory>
#include <string>
#include <vector>

#include "engine/engine.h"

namespace torrent {

struct ProtocolBackendConfig {
    engine_upload_mode upload_mode;
    std::uint64_t upload_limit_bytes_per_second;
    std::string state_directory;
    std::string download_directory;
    std::uint16_t listen_port;
    std::uint64_t memory_cache_capacity_bytes;
    std::uint64_t disk_cache_capacity_bytes;
    std::uint32_t stream_inactivity_timeout_milliseconds;
    std::uint32_t warm_torrent_timeout_milliseconds;
    engine_torrent_profile torrent_profile;
    std::string tls_ca_bundle_path;
};

enum class TorrentInputType {
    magnet,
    torrent_data,
};

struct TorrentInput {
    TorrentInputType type;
    std::string magnet_uri;
    std::vector<char> torrent_data;
};

struct TorrentFileInfo {
    std::string path;
    std::uint64_t offset;
    std::uint64_t size;
};

enum class BackendEventType {
    torrent_added,
    metadata_ready,
    torrent_error,
    stream_prepared,
    stream_stopped,
    disk_cache_reclaimed,
    torrent_removed,
};

struct BackendEvent {
    BackendEventType type;
    std::uint64_t request_id;
    std::string torrent_id;
    std::string message;
    std::vector<TorrentFileInfo> files{};
    std::uint32_t file_index = std::numeric_limits<std::uint32_t>::max();
    std::uint64_t file_size = 0;
    std::string stream_id{};
    std::string stream_url{};
};

struct BackendStats {
    std::uint32_t active_torrents = 0;
    std::uint32_t active_streams = 0;
    std::uint32_t active_http_requests = 0;
    std::uint32_t connected_peers = 0;
    std::uint32_t connected_seeds = 0;
    std::uint32_t known_peers = 0;
    std::uint32_t connect_candidates = 0;
    std::uint32_t interested_peers = 0;
    std::uint32_t unchoked_peers = 0;
    std::uint32_t downloading_peers = 0;
    std::uint32_t snubbed_peers = 0;
    std::uint32_t pending_block_requests = 0;
    std::uint32_t target_block_requests = 0;
    std::uint32_t timed_out_block_requests = 0;
    std::uint32_t pending_piece_reads = 0;
    std::uint32_t connecting_peers = 0;
    std::uint32_t handshaking_peers = 0;
    std::uint32_t target_piece_peers = 0;
    std::uint32_t target_piece_unchoked_peers = 0;
    std::uint32_t target_piece_downloading_peers = 0;
    std::uint32_t off_target_downloading_peers = 0;
    std::uint32_t tracker_reply_events = 0;
    std::uint32_t tracker_error_events = 0;
    std::uint32_t dht_reply_events = 0;
    std::uint64_t download_rate_bytes_per_second = 0;
    std::uint64_t upload_rate_bytes_per_second = 0;
    std::uint64_t total_payload_download_bytes = 0;
    std::uint64_t total_payload_upload_bytes = 0;
    std::uint64_t memory_cache_capacity_bytes = 0;
    std::uint64_t memory_cache_used_bytes = 0;
    std::uint64_t memory_cache_hits = 0;
    std::uint64_t memory_cache_misses = 0;
    std::uint64_t memory_cache_evictions = 0;
    std::uint64_t memory_cache_entries = 0;
    std::uint32_t warm_torrents = 0;
    std::uint32_t quiesced_torrents = 0;
    std::uint64_t disk_cache_capacity_bytes = 0;
    std::uint64_t disk_cache_used_bytes = 0;
    std::uint64_t disk_cache_protected_bytes = 0;
    std::uint64_t disk_cache_evictions = 0;
    std::uint64_t disk_cache_reclaimed_bytes = 0;
    std::uint64_t tracker_peers_returned = 0;
    std::uint64_t dht_peers_returned = 0;
    std::uint64_t peer_connect_events = 0;
    std::uint64_t peer_disconnect_events = 0;
    std::uint64_t peer_disconnect_timeouts = 0;
    std::uint64_t peer_disconnect_connect_failures = 0;
    std::uint64_t peer_disconnect_redundant = 0;
    std::uint64_t peer_disconnect_turnover = 0;
    std::uint64_t peer_disconnect_other = 0;
    std::uint64_t torrent_finished_events = 0;
    bool disk_cache_over_budget = false;
    struct Stream {
        std::string stream_id;
        std::uint32_t file_index = 0;
        std::uint64_t file_size = 0;
        std::uint64_t contiguous_ready_bytes = 0;
        std::uint64_t verified_file_bytes = 0;
        std::uint64_t delivered_bytes = 0;
        std::uint32_t active_demands = 0;
        std::uint32_t scheduled_pieces = 0;
        std::uint32_t blocking_pieces = 0;
        std::uint32_t primary_blocking_piece = std::numeric_limits<std::uint32_t>::max();
        std::uint32_t secondary_blocking_piece = std::numeric_limits<std::uint32_t>::max();
        std::uint32_t last_ready_piece = std::numeric_limits<std::uint32_t>::max();
        std::uint64_t primary_demand_start = 0;
        std::uint64_t primary_demand_end = 0;
        std::uint64_t secondary_demand_start = 0;
        std::uint64_t secondary_demand_end = 0;
        std::uint64_t schedule_revision = 0;
    };
    std::vector<Stream> streams{};
};

struct PeerDetails {
    std::uint32_t flags = 0;
    std::uint32_t source = 0;
    std::uint32_t progress_ppm = 0;
    std::string address;
    std::string client;
    std::uint64_t download_rate_bytes_per_second = 0;
    std::uint64_t upload_rate_bytes_per_second = 0;
    std::uint64_t total_download_bytes = 0;
    std::uint64_t total_upload_bytes = 0;
    std::uint32_t rtt_milliseconds = 0;
    std::uint32_t download_queue_length = 0;
    std::uint32_t hash_failures = 0;
    std::int32_t downloading_piece = -1;
};

struct TrackerDetails {
    std::string url;
    std::string message;
    std::uint32_t tier = 0;
    engine_tracker_status status = ENGINE_TRACKER_NOT_CONTACTED;
    std::int32_t seeds = -1;
    std::int32_t leechers = -1;
    std::int32_t downloaded = -1;
    std::uint32_t failures = 0;
    std::int64_t next_announce_seconds = -1;
};

struct TorrentDetails {
    std::string torrent_id;
    engine_torrent_state state = ENGINE_TORRENT_STATE_UNKNOWN;
    std::string name;
    std::string current_tracker;
    bool has_metadata = false;
    std::uint32_t piece_count = 0;
    std::uint32_t piece_length = 0;
    std::uint32_t pieces_have = 0;
    std::uint32_t file_count = 0;
    std::uint32_t progress_ppm = 0;
    std::int32_t distributed_copies_milli = -1;
    std::uint32_t connected_peers = 0;
    std::uint32_t connected_seeds = 0;
    std::uint32_t known_peers = 0;
    std::uint32_t known_seeds = 0;
    std::uint32_t connect_candidates = 0;
    std::int32_t swarm_seeds = -1;
    std::int32_t swarm_leechers = -1;
    std::uint64_t total_size = 0;
    std::uint64_t total_wanted = 0;
    std::uint64_t total_wanted_done = 0;
    std::uint64_t total_done = 0;
    std::uint64_t download_rate_bytes_per_second = 0;
    std::uint64_t upload_rate_bytes_per_second = 0;
    std::uint64_t download_payload_rate_bytes_per_second = 0;
    std::uint64_t upload_payload_rate_bytes_per_second = 0;
    std::uint64_t session_payload_download_bytes = 0;
    std::uint64_t session_payload_upload_bytes = 0;
    std::uint64_t all_time_download_bytes = 0;
    std::uint64_t all_time_upload_bytes = 0;
    std::uint64_t failed_bytes = 0;
    std::uint64_t redundant_bytes = 0;
    std::int64_t added_time_unix_seconds = 0;
    std::int64_t active_seconds = 0;
    std::int64_t next_announce_seconds = -1;
    std::vector<PeerDetails> peers{};
    std::vector<TrackerDetails> trackers{};
    std::vector<std::uint8_t> piece_states{};
    std::vector<std::uint8_t> piece_availability{};
};

class ProtocolBackend {
public:
    virtual ~ProtocolBackend() = default;
    virtual void add_torrent(
        std::uint64_t request_id,
        TorrentInput input,
        const std::string& save_path
    ) = 0;
    virtual void prepare_file(
        std::uint64_t request_id,
        const std::string& torrent_id,
        std::uint32_t file_index,
        TorrentFileInfo file
    ) = 0;
    virtual void remove_torrent(
        std::uint64_t request_id,
        const std::string& torrent_id
    ) = 0;
    virtual void stop_stream(
        std::uint64_t request_id,
        const std::string& stream_id
    ) = 0;
    virtual void reclaim_disk_cache(
        std::uint64_t request_id,
        std::uint64_t target_bytes
    ) = 0;
    virtual void shutdown() = 0;
    [[nodiscard]] virtual std::vector<BackendEvent> pop_events() = 0;
    [[nodiscard]] virtual BackendStats statistics() = 0;
    virtual void set_upload_mode(
        engine_upload_mode /*upload_mode*/,
        std::uint64_t /*upload_limit_bytes_per_second*/
    ) {
    }
    virtual void set_stream_duration(
        const std::string& /*stream_id*/,
        std::uint64_t /*duration_milliseconds*/
    ) {
    }
    [[nodiscard]] virtual std::vector<TorrentDetails> torrent_details() {
        return {};
    }
};

[[nodiscard]] std::unique_ptr<ProtocolBackend> create_protocol_backend(
    const ProtocolBackendConfig& config
);

}

#endif
