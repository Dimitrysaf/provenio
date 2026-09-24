#include "core/engine_runtime.hpp"
#include "engine/file_selection.hpp"

#include <algorithm>
#include <chrono>
#include <cstring>
#include <exception>
#include <optional>
#include <utility>

namespace core {
namespace {

template <std::size_t Size>
void copy_text(char (&destination)[Size], const std::string& source) {
    const auto length = std::min(source.size(), Size - 1);
    std::memcpy(destination, source.data(), length);
    destination[length] = '\0';
}

}

EngineRuntime::EngineRuntime(
    std::unique_ptr<torrent::ProtocolBackend> backend,
    std::string save_path,
    const std::size_t command_capacity,
    const std::size_t event_capacity
)
    : backend_(std::move(backend)),
      save_path_(std::move(save_path)),
      command_capacity_(command_capacity),
      event_capacity_(event_capacity) {
    stats_.struct_size = sizeof(engine_stats);
    if (backend_) {
        worker_ = std::thread(&EngineRuntime::run, this);
    }
}

EngineRuntime::~EngineRuntime() {
    {
        std::lock_guard lock(command_mutex_);
        stopping_ = true;
    }
    command_ready_.notify_one();
    if (worker_.joinable()) {
        worker_.join();
    }
}

engine_status EngineRuntime::add_torrent(
    torrent::TorrentInput input,
    std::uint64_t& request_id
) {
    Command command{};
    command.type = CommandType::add_torrent;
    command.input = std::move(input);
    return enqueue(std::move(command), request_id);
}

engine_status EngineRuntime::enqueue(Command command, std::uint64_t& request_id) {
    if (!backend_) {
        return ENGINE_STATUS_BACKEND_UNAVAILABLE;
    }
    std::lock_guard lock(command_mutex_);
    if (stopping_) {
        return ENGINE_STATUS_INITIALIZATION_FAILED;
    }
    if (commands_.size() >= command_capacity_) {
        return ENGINE_STATUS_QUEUE_FULL;
    }
    const auto accepted_request_id = next_request_id_;
    command.request_id = accepted_request_id;
    commands_.push_back(std::move(command));
    ++next_request_id_;
    request_id = accepted_request_id;
    command_ready_.notify_one();
    return ENGINE_STATUS_OK;
}

engine_status EngineRuntime::prepare_stream(
    std::string torrent_id,
    const std::optional<std::size_t> requested_index,
    std::string filename_hint,
    std::uint64_t& request_id
) {
    torrent::TorrentFileInfo selected_file;
    std::size_t selected_index = 0;
    {
        std::lock_guard lock(metadata_mutex_);
        const auto snapshot = files_.find(torrent_id);
        if (snapshot == files_.end()) {
            return ENGINE_STATUS_METADATA_NOT_READY;
        }
        std::vector<torrent::TorrentFile> selectable;
        selectable.reserve(snapshot->second.size());
        for (const auto& file : snapshot->second) {
            selectable.push_back({file.path, file.size});
        }
        const auto selection = torrent::select_file(
            selectable,
            requested_index,
            filename_hint
        );
        if (!selection.index.has_value()) {
            return ENGINE_STATUS_NOT_FOUND;
        }
        selected_index = *selection.index;
        selected_file = snapshot->second[selected_index];
    }
    Command command{};
    command.type = CommandType::prepare_stream;
    command.torrent_id = std::move(torrent_id);
    command.file_index = static_cast<std::uint32_t>(selected_index);
    command.file = std::move(selected_file);
    return enqueue(std::move(command), request_id);
}

engine_status EngineRuntime::remove_torrent(
    std::string torrent_id,
    std::uint64_t& request_id
) {
    Command command{};
    command.type = CommandType::remove_torrent;
    command.torrent_id = std::move(torrent_id);
    return enqueue(std::move(command), request_id);
}

engine_status EngineRuntime::stop_stream(
    std::string stream_id,
    std::uint64_t& request_id
) {
    Command command{};
    command.type = CommandType::stop_stream;
    command.stream_id = std::move(stream_id);
    return enqueue(std::move(command), request_id);
}

engine_status EngineRuntime::get_file_count(
    const std::string& torrent_id,
    std::size_t& file_count
) {
    std::lock_guard lock(metadata_mutex_);
    const auto files = files_.find(torrent_id);
    if (files == files_.end()) {
        return ENGINE_STATUS_METADATA_NOT_READY;
    }
    file_count = files->second.size();
    return ENGINE_STATUS_OK;
}

engine_status EngineRuntime::get_file(
    const std::string& torrent_id,
    const std::size_t file_index,
    engine_file& file
) {
    std::lock_guard lock(metadata_mutex_);
    const auto files = files_.find(torrent_id);
    if (files == files_.end()) {
        return ENGINE_STATUS_METADATA_NOT_READY;
    }
    if (file_index >= files->second.size()) {
        return ENGINE_STATUS_OUT_OF_RANGE;
    }
    const auto& source = files->second[file_index];
    file = {};
    file.struct_size = sizeof(engine_file);
    file.index = static_cast<std::uint32_t>(file_index);
    file.offset = source.offset;
    file.size = source.size;
    file.path_truncated = source.path.size() >= sizeof(file.path) ? 1 : 0;
    copy_text(file.path, source.path);
    return ENGINE_STATUS_OK;
}

engine_status EngineRuntime::poll_event(engine_event& event) {
    std::lock_guard lock(event_mutex_);
    if (events_.empty()) {
        return ENGINE_STATUS_NO_EVENT;
    }
    event = events_.front();
    events_.pop_front();
    return ENGINE_STATUS_OK;
}

engine_stats EngineRuntime::get_stats() {
    std::lock_guard lock(stats_mutex_);
    return stats_;
}

engine_status EngineRuntime::get_stream_stats(
    const std::string& stream_id,
    engine_stream_stats& stats
) {
    std::lock_guard lock(stats_mutex_);
    const auto found = stream_stats_.find(stream_id);
    if (found == stream_stats_.end()) {
        return ENGINE_STATUS_NOT_FOUND;
    }
    stats = found->second;
    return ENGINE_STATUS_OK;
}

engine_status EngineRuntime::reclaim_disk_cache(
    const std::uint64_t target_bytes,
    std::uint64_t& request_id
) {
    Command command{};
    command.type = CommandType::reclaim_disk_cache;
    command.target_bytes = target_bytes;
    return enqueue(std::move(command), request_id);
}

engine_status EngineRuntime::set_upload_mode(
    const engine_upload_mode upload_mode,
    const std::uint64_t upload_limit_bytes_per_second
) {
    Command command{};
    command.type = CommandType::set_upload_mode;
    command.upload_mode = upload_mode;
    command.target_bytes = upload_limit_bytes_per_second;
    std::uint64_t request_id = 0;
    return enqueue(std::move(command), request_id);
}

engine_status EngineRuntime::get_torrent_details(
    const std::string& torrent_id,
    engine_torrent_details& details
) {
    std::lock_guard lock(details_mutex_);
    const auto found = torrent_details_.find(torrent_id);
    if (found == torrent_details_.end()) {
        return ENGINE_STATUS_NOT_FOUND;
    }
    const auto& source = found->second;
    details = {};
    details.struct_size = sizeof(engine_torrent_details);
    details.state = source.state;
    details.name_truncated = source.name.size() >= sizeof(details.name) ? 1 : 0;
    copy_text(details.name, source.name);
    details.has_metadata = source.has_metadata ? 1 : 0;
    copy_text(details.current_tracker, source.current_tracker);
    details.piece_count = source.piece_count;
    details.piece_length = source.piece_length;
    details.pieces_have = source.pieces_have;
    details.file_count = source.file_count;
    details.progress_ppm = source.progress_ppm;
    details.distributed_copies_milli = source.distributed_copies_milli;
    details.connected_peers = source.connected_peers;
    details.connected_seeds = source.connected_seeds;
    details.known_peers = source.known_peers;
    details.known_seeds = source.known_seeds;
    details.connect_candidates = source.connect_candidates;
    details.swarm_seeds = source.swarm_seeds;
    details.swarm_leechers = source.swarm_leechers;
    details.peer_count = static_cast<std::uint32_t>(source.peers.size());
    details.tracker_count = static_cast<std::uint32_t>(source.trackers.size());
    details.total_size = source.total_size;
    details.total_wanted = source.total_wanted;
    details.total_wanted_done = source.total_wanted_done;
    details.total_done = source.total_done;
    details.download_rate_bytes_per_second = source.download_rate_bytes_per_second;
    details.upload_rate_bytes_per_second = source.upload_rate_bytes_per_second;
    details.download_payload_rate_bytes_per_second =
        source.download_payload_rate_bytes_per_second;
    details.upload_payload_rate_bytes_per_second = source.upload_payload_rate_bytes_per_second;
    details.session_payload_download_bytes = source.session_payload_download_bytes;
    details.session_payload_upload_bytes = source.session_payload_upload_bytes;
    details.all_time_download_bytes = source.all_time_download_bytes;
    details.all_time_upload_bytes = source.all_time_upload_bytes;
    details.failed_bytes = source.failed_bytes;
    details.redundant_bytes = source.redundant_bytes;
    details.added_time_unix_seconds = source.added_time_unix_seconds;
    details.active_seconds = source.active_seconds;
    details.next_announce_seconds = source.next_announce_seconds;
    return ENGINE_STATUS_OK;
}

engine_status EngineRuntime::get_peers(
    const std::string& torrent_id,
    std::vector<torrent::PeerDetails>& peers
) {
    std::lock_guard lock(details_mutex_);
    const auto found = torrent_details_.find(torrent_id);
    if (found == torrent_details_.end()) {
        return ENGINE_STATUS_NOT_FOUND;
    }
    peers = found->second.peers;
    return ENGINE_STATUS_OK;
}

engine_status EngineRuntime::get_trackers(
    const std::string& torrent_id,
    std::vector<torrent::TrackerDetails>& trackers
) {
    std::lock_guard lock(details_mutex_);
    const auto found = torrent_details_.find(torrent_id);
    if (found == torrent_details_.end()) {
        return ENGINE_STATUS_NOT_FOUND;
    }
    trackers = found->second.trackers;
    return ENGINE_STATUS_OK;
}

engine_status EngineRuntime::get_piece_map(
    const std::string& torrent_id,
    std::vector<std::uint8_t>& states,
    std::vector<std::uint8_t>& availability
) {
    std::lock_guard lock(details_mutex_);
    const auto found = torrent_details_.find(torrent_id);
    if (found == torrent_details_.end()) {
        return ENGINE_STATUS_NOT_FOUND;
    }
    states = found->second.piece_states;
    availability = found->second.piece_availability;
    return ENGINE_STATUS_OK;
}

void EngineRuntime::run() {
    while (true) {
        std::optional<Command> command;
        bool shutdown_requested = false;
        {
            std::unique_lock lock(command_mutex_);
            command_ready_.wait_for(lock, std::chrono::milliseconds(25), [this] {
                return stopping_ || !commands_.empty();
            });
            if (!commands_.empty()) {
                command = std::move(commands_.front());
                commands_.pop_front();
            } else if (stopping_) {
                shutdown_requested = true;
            }
        }
        if (shutdown_requested) {
            try {
                backend_->shutdown();
            } catch (...) {
            }
            break;
        }
        if (command.has_value()) {
            process_command(std::move(*command));
        }
        try {
            collect_backend_events();
        } catch (const std::exception& error) {
            push_event({torrent::BackendEventType::torrent_error, 0, {}, error.what(), {}});
        } catch (...) {
            push_event({
                torrent::BackendEventType::torrent_error,
                0,
                {},
                "unknown protocol event error",
                {},
            });
        }
    }
}

void EngineRuntime::process_command(Command command) {
    try {
        switch (command.type) {
        case CommandType::add_torrent:
            backend_->add_torrent(command.request_id, std::move(command.input), save_path_);
            break;
        case CommandType::prepare_stream:
            backend_->prepare_file(
                command.request_id,
                command.torrent_id,
                command.file_index,
                std::move(command.file)
            );
            break;
        case CommandType::stop_stream:
            backend_->stop_stream(command.request_id, command.stream_id);
            break;
        case CommandType::reclaim_disk_cache:
            backend_->reclaim_disk_cache(command.request_id, command.target_bytes);
            break;
        case CommandType::remove_torrent:
            backend_->remove_torrent(command.request_id, command.torrent_id);
            break;
        case CommandType::set_upload_mode:
            backend_->set_upload_mode(command.upload_mode, command.target_bytes);
            break;
        }
    } catch (const std::exception& error) {
        push_event({
            torrent::BackendEventType::torrent_error,
            command.request_id,
            {},
            error.what(),
            {},
        });
    } catch (...) {
        push_event({
            torrent::BackendEventType::torrent_error,
            command.request_id,
            {},
            "unknown protocol backend error",
            {},
        });
    }
}

void EngineRuntime::collect_backend_events() {
    if (!backend_) {
        return;
    }
    for (auto& event : backend_->pop_events()) {
        push_event(std::move(event));
    }
    // Details carry peer lists and piece maps, so they refresh at the backend's
    // telemetry cadence rather than on every 25 ms pass.
    const auto now = std::chrono::steady_clock::now();
    if (now >= next_details_refresh_) {
        next_details_refresh_ = now + std::chrono::milliseconds(500);
        std::unordered_map<std::string, torrent::TorrentDetails> details;
        for (auto& torrent : backend_->torrent_details()) {
            auto id = torrent.torrent_id;
            details.insert_or_assign(std::move(id), std::move(torrent));
        }
        std::lock_guard lock(details_mutex_);
        torrent_details_ = std::move(details);
    }
    const auto backend_stats = backend_->statistics();
    engine_stats stats{};
    stats.struct_size = sizeof(engine_stats);
    stats.active_torrents = backend_stats.active_torrents;
    stats.active_streams = backend_stats.active_streams;
    stats.active_http_requests = backend_stats.active_http_requests;
    stats.connected_peers = backend_stats.connected_peers;
    stats.connected_seeds = backend_stats.connected_seeds;
    stats.known_peers = backend_stats.known_peers;
    stats.connect_candidates = backend_stats.connect_candidates;
    stats.interested_peers = backend_stats.interested_peers;
    stats.unchoked_peers = backend_stats.unchoked_peers;
    stats.downloading_peers = backend_stats.downloading_peers;
    stats.snubbed_peers = backend_stats.snubbed_peers;
    stats.pending_block_requests = backend_stats.pending_block_requests;
    stats.target_block_requests = backend_stats.target_block_requests;
    stats.timed_out_block_requests = backend_stats.timed_out_block_requests;
    stats.connecting_peers = backend_stats.connecting_peers;
    stats.handshaking_peers = backend_stats.handshaking_peers;
    stats.target_piece_peers = backend_stats.target_piece_peers;
    stats.target_piece_unchoked_peers = backend_stats.target_piece_unchoked_peers;
    stats.target_piece_downloading_peers = backend_stats.target_piece_downloading_peers;
    stats.off_target_downloading_peers = backend_stats.off_target_downloading_peers;
    stats.tracker_reply_events = backend_stats.tracker_reply_events;
    stats.tracker_error_events = backend_stats.tracker_error_events;
    stats.dht_reply_events = backend_stats.dht_reply_events;
    stats.pending_piece_reads = backend_stats.pending_piece_reads;
    stats.download_rate_bytes_per_second =
        backend_stats.download_rate_bytes_per_second;
    stats.upload_rate_bytes_per_second = backend_stats.upload_rate_bytes_per_second;
    stats.total_payload_download_bytes = backend_stats.total_payload_download_bytes;
    stats.total_payload_upload_bytes = backend_stats.total_payload_upload_bytes;
    stats.memory_cache_capacity_bytes = backend_stats.memory_cache_capacity_bytes;
    stats.memory_cache_used_bytes = backend_stats.memory_cache_used_bytes;
    stats.memory_cache_hits = backend_stats.memory_cache_hits;
    stats.memory_cache_misses = backend_stats.memory_cache_misses;
    stats.memory_cache_evictions = backend_stats.memory_cache_evictions;
    stats.memory_cache_entries = backend_stats.memory_cache_entries;
    stats.warm_torrents = backend_stats.warm_torrents;
    stats.quiesced_torrents = backend_stats.quiesced_torrents;
    stats.disk_cache_capacity_bytes = backend_stats.disk_cache_capacity_bytes;
    stats.disk_cache_used_bytes = backend_stats.disk_cache_used_bytes;
    stats.disk_cache_protected_bytes = backend_stats.disk_cache_protected_bytes;
    stats.disk_cache_evictions = backend_stats.disk_cache_evictions;
    stats.disk_cache_reclaimed_bytes = backend_stats.disk_cache_reclaimed_bytes;
    stats.disk_cache_over_budget = backend_stats.disk_cache_over_budget ? 1 : 0;
    stats.tracker_peers_returned = backend_stats.tracker_peers_returned;
    stats.dht_peers_returned = backend_stats.dht_peers_returned;
    stats.peer_connect_events = backend_stats.peer_connect_events;
    stats.peer_disconnect_events = backend_stats.peer_disconnect_events;
    stats.peer_disconnect_timeouts = backend_stats.peer_disconnect_timeouts;
    stats.peer_disconnect_connect_failures =
        backend_stats.peer_disconnect_connect_failures;
    stats.peer_disconnect_redundant = backend_stats.peer_disconnect_redundant;
    stats.peer_disconnect_turnover = backend_stats.peer_disconnect_turnover;
    stats.peer_disconnect_other = backend_stats.peer_disconnect_other;
    stats.torrent_finished_events = backend_stats.torrent_finished_events;
    std::lock_guard lock(stats_mutex_);
    stats_ = stats;
    stream_stats_.clear();
    for (const auto& stream : backend_stats.streams) {
        engine_stream_stats snapshot{};
        snapshot.struct_size = sizeof(engine_stream_stats);
        snapshot.file_index = stream.file_index;
        snapshot.file_size = stream.file_size;
        snapshot.contiguous_ready_bytes = stream.contiguous_ready_bytes;
        snapshot.verified_file_bytes = stream.verified_file_bytes;
        snapshot.delivered_bytes = stream.delivered_bytes;
        snapshot.active_demands = stream.active_demands;
        snapshot.scheduled_pieces = stream.scheduled_pieces;
        snapshot.blocking_pieces = stream.blocking_pieces;
        snapshot.primary_blocking_piece = stream.primary_blocking_piece;
        snapshot.secondary_blocking_piece = stream.secondary_blocking_piece;
        snapshot.last_ready_piece = stream.last_ready_piece;
        snapshot.primary_demand_start = stream.primary_demand_start;
        snapshot.primary_demand_end = stream.primary_demand_end;
        snapshot.secondary_demand_start = stream.secondary_demand_start;
        snapshot.secondary_demand_end = stream.secondary_demand_end;
        snapshot.schedule_revision = stream.schedule_revision;
        stream_stats_.insert_or_assign(stream.stream_id, snapshot);
    }
}

void EngineRuntime::push_event(torrent::BackendEvent backend_event) {
    if (backend_event.type == torrent::BackendEventType::metadata_ready) {
        std::lock_guard lock(metadata_mutex_);
        files_.insert_or_assign(backend_event.torrent_id, std::move(backend_event.files));
    } else if (backend_event.type == torrent::BackendEventType::torrent_removed) {
        std::lock_guard lock(metadata_mutex_);
        files_.erase(backend_event.torrent_id);
    }
    engine_event event{};
    event.struct_size = sizeof(engine_event);
    switch (backend_event.type) {
    case torrent::BackendEventType::torrent_added:
        event.type = ENGINE_EVENT_TORRENT_ADDED;
        break;
    case torrent::BackendEventType::metadata_ready:
        event.type = ENGINE_EVENT_TORRENT_METADATA_READY;
        break;
    case torrent::BackendEventType::torrent_error:
        event.type = ENGINE_EVENT_TORRENT_ERROR;
        break;
    case torrent::BackendEventType::stream_prepared:
        event.type = ENGINE_EVENT_STREAM_PREPARED;
        break;
    case torrent::BackendEventType::stream_stopped:
        event.type = ENGINE_EVENT_STREAM_STOPPED;
        break;
    case torrent::BackendEventType::disk_cache_reclaimed:
        event.type = ENGINE_EVENT_DISK_CACHE_RECLAIMED;
        break;
    case torrent::BackendEventType::torrent_removed:
        event.type = ENGINE_EVENT_TORRENT_REMOVED;
        break;
    }
    event.sequence = next_sequence_++;
    event.request_id = backend_event.request_id;
    copy_text(event.torrent_id, backend_event.torrent_id);
    copy_text(event.message, backend_event.message);
    event.file_index = backend_event.file_index;
    event.file_size = backend_event.file_size;
    copy_text(event.stream_id, backend_event.stream_id);
    copy_text(event.stream_url, backend_event.stream_url);

    std::lock_guard lock(event_mutex_);
    if (events_.size() >= event_capacity_) {
        events_.pop_front();
        ++dropped_events_;
    }
    event.dropped_events = dropped_events_;
    events_.push_back(event);
}

}
