#ifndef ENGINE_STREAM_DEMAND_PLAN_HPP
#define ENGINE_STREAM_DEMAND_PLAN_HPP

#include "engine/byte_range.hpp"
#include "engine/piece_scheduler.hpp"

#include <cstdint>
#include <vector>

namespace scheduler {

struct StreamDemand {
    std::uint64_t id;
    std::uint64_t file_offset;
    std::uint64_t file_size;
    std::uint32_t piece_size;
    http::ByteRange range;
};

// Bytes kept around the focused demand: near is playback, the rest ahead readahead, behind backfill.
struct StreamWindow {
    std::uint64_t forward_bytes = 0;
    std::uint64_t near_bytes = 0;
    std::uint64_t backward_bytes = 0;
};

struct StreamDemandPlan {
    std::uint64_t focused_demand_id = 0;
    std::vector<PiecePriority> pieces;
    std::vector<std::uint32_t> blocking_deadline_order;
};

[[nodiscard]] StreamDemandPlan build_stream_demand_plan(
    std::vector<StreamDemand> demands,
    std::uint64_t selection_bytes,
    std::uint64_t critical_front_bytes
);

[[nodiscard]] StreamDemandPlan build_stream_demand_plan(
    std::vector<StreamDemand> demands,
    StreamWindow window,
    std::uint64_t critical_front_bytes
);

}

#endif
