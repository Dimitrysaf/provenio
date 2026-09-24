#ifndef ENGINE_DEMAND_WINDOW_HPP
#define ENGINE_DEMAND_WINDOW_HPP

#include "engine/byte_range.hpp"

#include <cstdint>

namespace scheduler {

struct RollingLookahead {
    std::uint64_t critical_bytes;
    std::uint64_t playback_bytes;

    [[nodiscard]] bool operator==(const RollingLookahead&) const = default;
};

[[nodiscard]] http::ByteRange blocking_demand_range(
    http::ByteRange response_range,
    std::uint64_t position,
    std::uint64_t file_offset,
    std::uint32_t piece_size
);

[[nodiscard]] RollingLookahead rolling_lookahead(
    http::ByteRange blocking_range,
    std::uint64_t selection_bytes,
    std::uint64_t critical_front_bytes
);

}

#endif
