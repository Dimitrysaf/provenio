#ifndef ENGINE_ENGINE_RUNTIME_HPP
#define ENGINE_ENGINE_RUNTIME_HPP

#include <chrono>
#include <condition_variable>
#include <cstddef>
#include <cstdint>
#include <deque>
#include <memory>
#include <mutex>
#include <optional>
#include <string>
#include <thread>
#include <unordered_map>
#include <vector>

#include "engine/engine.h"
#include "torrent/protocol_backend.hpp"

namespace core {

class EngineRuntime {
public:
    EngineRuntime(
        std::unique_ptr<torrent::ProtocolBackend> backend,
        std::string save_path,
        std::size_t command_capacity = 256,
        std::size_t event_capacity = 1024
    );
    ~EngineRuntime();

    EngineRuntime(const EngineRuntime&) = delete;
    EngineRuntime& operator=(const EngineRuntime&) = delete;

    [[nodiscard]] engine_status add_torrent(
        torrent::TorrentInput input,
        std::uint64_t& request_id
    );
    [[nodiscard]] engine_status poll_event(engine_event& event);
    [[nodiscard]] engine_status get_file_count(
        const std::string& torrent_id,
        std::size_t& file_count
    );
    [[nodiscard]] engine_status get_file(
        const std::string& torrent_id,
        std::size_t file_index,
        engine_file& file
    );
    [[nodiscard]] engine_status prepare_stream(
        std::string torrent_id,
        std::optional<std::size_t> requested_index,
        std::string filename_hint,
        std::uint64_t& request_id
    );
    [[nodiscard]] engine_status remove_torrent(
        std::string torrent_id,
        std::uint64_t& request_id
    );
    [[nodiscard]] engine_status stop_stream(
        std::string stream_id,
        std::uint64_t& request_id
    );
    [[nodiscard]] engine_stats get_stats();
    [[nodiscard]] engine_status get_stream_stats(
        const std::string& stream_id,
        engine_stream_stats& stats
    );
    [[nodiscard]] engine_status reclaim_disk_cache(
        std::uint64_t target_bytes,
        std::uint64_t& request_id
    );
    [[nodiscard]] engine_status set_upload_mode(
        engine_upload_mode upload_mode,
        std::uint64_t upload_limit_bytes_per_second
    );
    [[nodiscard]] engine_status get_torrent_details(
        const std::string& torrent_id,
        engine_torrent_details& details
    );
    [[nodiscard]] engine_status get_peers(
        const std::string& torrent_id,
        std::vector<torrent::PeerDetails>& peers
    );
    [[nodiscard]] engine_status get_trackers(
        const std::string& torrent_id,
        std::vector<torrent::TrackerDetails>& trackers
    );
    [[nodiscard]] engine_status get_piece_map(
        const std::string& torrent_id,
        std::vector<std::uint8_t>& states,
        std::vector<std::uint8_t>& availability
    );

private:
    enum class CommandType {
        add_torrent,
        prepare_stream,
        stop_stream,
        reclaim_disk_cache,
        remove_torrent,
        set_upload_mode,
    };

    struct Command {
        CommandType type;
        std::uint64_t request_id;
        torrent::TorrentInput input;
        std::string torrent_id;
        std::string stream_id;
        std::uint32_t file_index = 0;
        torrent::TorrentFileInfo file;
        std::uint64_t target_bytes = 0;
        engine_upload_mode upload_mode = ENGINE_UPLOAD_DISABLED;
    };

    [[nodiscard]] engine_status enqueue(Command command, std::uint64_t& request_id);
    void run();
    void process_command(Command command);
    void collect_backend_events();
    void push_event(torrent::BackendEvent event);

    std::unique_ptr<torrent::ProtocolBackend> backend_;
    std::string save_path_;
    const std::size_t command_capacity_;
    const std::size_t event_capacity_;
    std::mutex command_mutex_;
    std::condition_variable command_ready_;
    std::deque<Command> commands_;
    std::mutex event_mutex_;
    std::deque<engine_event> events_;
    std::mutex metadata_mutex_;
    std::unordered_map<std::string, std::vector<torrent::TorrentFileInfo>> files_;
    std::mutex stats_mutex_;
    engine_stats stats_{};
    std::unordered_map<std::string, engine_stream_stats> stream_stats_;
    std::mutex details_mutex_;
    std::unordered_map<std::string, torrent::TorrentDetails> torrent_details_;
    std::chrono::steady_clock::time_point next_details_refresh_{};
    std::thread worker_;
    bool stopping_ = false;
    std::uint64_t next_request_id_ = 1;
    std::uint64_t next_sequence_ = 1;
    std::uint64_t dropped_events_ = 0;
};

}

#endif
