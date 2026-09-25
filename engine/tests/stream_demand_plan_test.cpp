#include "test_support.hpp"

#include "scheduler/stream_demand_plan.hpp"

#include <algorithm>
#include <optional>

using http::ByteRange;
using scheduler::PiecePriority;
using scheduler::PriorityClass;
using scheduler::StreamDemand;
using scheduler::StreamDemandPlan;
using scheduler::build_stream_demand_plan;

namespace {

constexpr std::uint64_t mebibyte = 1024ULL * 1024ULL;
constexpr std::uint64_t file_size = 64 * mebibyte;
constexpr std::uint32_t piece_size = static_cast<std::uint32_t>(mebibyte);
constexpr std::uint64_t selection_bytes = 15 * mebibyte;
constexpr std::uint64_t critical_front_bytes = mebibyte;

StreamDemand demand(const std::uint64_t id, const std::uint32_t piece) {
    return {
        id,
        0,
        file_size,
        piece_size,
        ByteRange{
            static_cast<std::uint64_t>(piece) * piece_size,
            (static_cast<std::uint64_t>(piece) + 1) * piece_size - 1,
        },
    };
}

std::optional<PriorityClass> priority_for(
    const StreamDemandPlan& plan,
    const std::uint32_t piece
) {
    const auto found = std::ranges::find(plan.pieces, piece, &PiecePriority::piece);
    return found == plan.pieces.end()
        ? std::nullopt
        : std::optional(found->priority);
}

StreamDemandPlan three_range_plan(std::vector<StreamDemand> demands) {
    return build_stream_demand_plan(
        std::move(demands),
        selection_bytes,
        critical_front_bytes
    );
}

}

TEST("newest stream demand owns deadline focus and lookahead") {
    const auto plan = three_range_plan({demand(10, 0), demand(20, 63), demand(30, 32)});

    EXPECT_EQ(plan.focused_demand_id, std::uint64_t(30));
    EXPECT_EQ(
        plan.blocking_deadline_order,
        (std::vector<std::uint32_t>{32, 63, 0})
    );
    EXPECT_EQ(priority_for(plan, 0), std::optional(PriorityClass::blocking));
    EXPECT_EQ(priority_for(plan, 32), std::optional(PriorityClass::blocking));
    EXPECT_EQ(priority_for(plan, 63), std::optional(PriorityClass::blocking));
    for (std::uint32_t piece = 33; piece <= 44; ++piece) {
        EXPECT_EQ(priority_for(plan, piece), std::optional(PriorityClass::playback));
    }
    EXPECT_TRUE(!priority_for(plan, 1).has_value());
    EXPECT_TRUE(!priority_for(plan, 62).has_value());
    EXPECT_EQ(plan.pieces.size(), std::size_t(15));
}

TEST("stream demand planning is independent of input iteration order") {
    const auto expected = three_range_plan({demand(10, 0), demand(20, 63), demand(30, 32)});
    const auto reversed = three_range_plan({demand(30, 32), demand(20, 63), demand(10, 0)});
    const auto shuffled = three_range_plan({demand(20, 63), demand(10, 0), demand(30, 32)});

    EXPECT_EQ(reversed.focused_demand_id, expected.focused_demand_id);
    EXPECT_EQ(reversed.blocking_deadline_order, expected.blocking_deadline_order);
    EXPECT_EQ(reversed.pieces, expected.pieces);
    EXPECT_EQ(shuffled.focused_demand_id, expected.focused_demand_id);
    EXPECT_EQ(shuffled.blocking_deadline_order, expected.blocking_deadline_order);
    EXPECT_EQ(shuffled.pieces, expected.pieces);
}

TEST("blocking stream pieces survive a saturated selection budget") {
    const auto plan = build_stream_demand_plan(
        {demand(10, 0), demand(20, 63), demand(30, 32)},
        2 * mebibyte,
        critical_front_bytes
    );

    EXPECT_EQ(plan.pieces.size(), std::size_t(3));
    EXPECT_EQ(
        plan.blocking_deadline_order,
        (std::vector<std::uint32_t>{32, 63, 0})
    );
    EXPECT_TRUE(std::ranges::all_of(plan.pieces, [](const PiecePriority& piece) {
        return piece.priority == PriorityClass::blocking;
    }));
}

TEST("overlapping stream blockers are charged and scheduled once") {
    const auto plan = three_range_plan({demand(10, 32), demand(20, 63), demand(30, 32)});

    EXPECT_EQ(plan.focused_demand_id, std::uint64_t(30));
    EXPECT_EQ(
        plan.blocking_deadline_order,
        (std::vector<std::uint32_t>{32, 63})
    );
    EXPECT_EQ(plan.pieces.size(), std::size_t(15));
}

TEST("a blocker inside focused lookahead is not charged twice") {
    const auto plan = three_range_plan({demand(10, 33), demand(20, 32)});

    EXPECT_EQ(plan.pieces.size(), std::size_t(15));
    EXPECT_EQ(priority_for(plan, 32), std::optional(PriorityClass::blocking));
    EXPECT_EQ(priority_for(plan, 33), std::optional(PriorityClass::blocking));
    EXPECT_EQ(priority_for(plan, 46), std::optional(PriorityClass::playback));
    EXPECT_TRUE(!priority_for(plan, 47).has_value());
}

TEST("ending the newest stream demand promotes the next live range") {
    const auto plan = three_range_plan({demand(10, 0), demand(20, 63)});

    EXPECT_EQ(plan.focused_demand_id, std::uint64_t(20));
    EXPECT_EQ(
        plan.blocking_deadline_order,
        (std::vector<std::uint32_t>{63, 0})
    );
    EXPECT_EQ(priority_for(plan, 0), std::optional(PriorityClass::blocking));
    EXPECT_EQ(priority_for(plan, 63), std::optional(PriorityClass::blocking));
}

TEST("lookahead budget charges a full physical torrent piece") {
    constexpr std::uint32_t large_piece = 8 * static_cast<std::uint32_t>(mebibyte);
    const StreamDemand unaligned{
        1,
        0,
        128 * mebibyte,
        large_piece,
        ByteRange{6 * mebibyte, 8 * mebibyte - 1},
    };
    const auto plan = build_stream_demand_plan(
        {unaligned},
        selection_bytes,
        critical_front_bytes
    );

    EXPECT_EQ(plan.pieces.size(), std::size_t(2));
    EXPECT_EQ(priority_for(plan, 0), std::optional(PriorityClass::blocking));
    EXPECT_EQ(priority_for(plan, 1), std::optional(PriorityClass::playback));
    EXPECT_TRUE(!priority_for(plan, 2).has_value());
}

TEST("physical budgeting preserves logical critical coverage") {
    const StreamDemand unaligned{
        1,
        0,
        file_size,
        piece_size,
        ByteRange{mebibyte / 2, mebibyte - 1},
    };
    const auto plan = build_stream_demand_plan(
        {unaligned},
        selection_bytes,
        critical_front_bytes
    );

    EXPECT_EQ(plan.pieces.size(), std::size_t(15));
    EXPECT_EQ(priority_for(plan, 0), std::optional(PriorityClass::blocking));
    EXPECT_EQ(priority_for(plan, 1), std::optional(PriorityClass::critical));
    EXPECT_EQ(priority_for(plan, 2), std::optional(PriorityClass::playback));
}

TEST("a stream window splits lookahead into playback and readahead") {
    const auto plan = build_stream_demand_plan(
        {demand(1, 10)},
        scheduler::StreamWindow{20 * mebibyte, 5 * mebibyte, 0},
        critical_front_bytes
    );

    EXPECT_EQ(priority_for(plan, 10), std::optional(PriorityClass::blocking));
    EXPECT_EQ(priority_for(plan, 11), std::optional(PriorityClass::playback));
    EXPECT_EQ(priority_for(plan, 15), std::optional(PriorityClass::playback));
    EXPECT_EQ(priority_for(plan, 16), std::optional(PriorityClass::readahead));
    EXPECT_EQ(priority_for(plan, 29), std::optional(PriorityClass::readahead));
    EXPECT_TRUE(!priority_for(plan, 30).has_value());
    EXPECT_TRUE(!priority_for(plan, 9).has_value());
}

TEST("a stream window backfills pieces behind the focused demand") {
    const auto plan = build_stream_demand_plan(
        {demand(1, 10)},
        scheduler::StreamWindow{4 * mebibyte, 4 * mebibyte, 3 * mebibyte},
        critical_front_bytes
    );

    EXPECT_EQ(priority_for(plan, 9), std::optional(PriorityClass::backfill));
    EXPECT_EQ(priority_for(plan, 7), std::optional(PriorityClass::backfill));
    EXPECT_TRUE(!priority_for(plan, 6).has_value());
    EXPECT_EQ(priority_for(plan, 10), std::optional(PriorityClass::blocking));
    EXPECT_EQ(plan.pieces.back().priority, PriorityClass::backfill);
}

TEST("backfill stops at the first piece of the file") {
    const auto plan = build_stream_demand_plan(
        {demand(1, 2)},
        scheduler::StreamWindow{4 * mebibyte, 4 * mebibyte, 10 * mebibyte},
        critical_front_bytes
    );

    EXPECT_EQ(priority_for(plan, 0), std::optional(PriorityClass::backfill));
    EXPECT_EQ(priority_for(plan, 1), std::optional(PriorityClass::backfill));
    EXPECT_EQ(priority_for(plan, 2), std::optional(PriorityClass::blocking));
}

TEST("backfill never downgrades another demand's blocking piece") {
    const auto plan = build_stream_demand_plan(
        {demand(1, 8), demand(2, 10)},
        scheduler::StreamWindow{4 * mebibyte, 4 * mebibyte, 4 * mebibyte},
        critical_front_bytes
    );

    EXPECT_EQ(priority_for(plan, 8), std::optional(PriorityClass::blocking));
    EXPECT_EQ(priority_for(plan, 9), std::optional(PriorityClass::backfill));
    EXPECT_EQ(priority_for(plan, 6), std::optional(PriorityClass::backfill));
}
