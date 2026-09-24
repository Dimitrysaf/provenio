#ifndef ENGINE_PIECE_SCHEDULER_HPP
#define ENGINE_PIECE_SCHEDULER_HPP

#include <cstdint>
#include <vector>

#include "engine/byte_range.hpp"
#include "engine/export.h"

namespace scheduler {

enum class PriorityClass : std::uint8_t {
    blocking = 0,
    critical = 1,
    playback = 2,
    metadata_tail = 3,
    readahead = 4,
};

struct PiecePriority {
    std::uint32_t piece;
    PriorityClass priority;

    [[nodiscard]] bool operator==(const PiecePriority&) const = default;
};

struct ScheduleRequest {
    std::uint64_t file_offset;
    std::uint64_t file_size;
    std::uint32_t piece_size;
    http::ByteRange demand;
    std::uint64_t critical_bytes;
    std::uint64_t playback_bytes;
    std::uint64_t readahead_bytes;
    std::uint64_t metadata_tail_bytes;
};

[[nodiscard]] ENGINE_CPP_API std::vector<PiecePriority> build_piece_schedule(
    const ScheduleRequest& request
);

}

#endif
