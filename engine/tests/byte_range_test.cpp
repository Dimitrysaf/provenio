#include "test_support.hpp"

#include "engine/byte_range.hpp"

using http::ByteRange;
using http::RangeStatus;
using http::parse_range_header;

TEST("missing range selects the full resource") {
    const auto result = parse_range_header("", 1000);
    EXPECT_EQ(result.status, RangeStatus::full);
    EXPECT_EQ(result.range, std::optional(ByteRange{0, 999}));
}

TEST("closed range is preserved") {
    const auto result = parse_range_header("bytes=100-199", 1000);
    EXPECT_EQ(result.status, RangeStatus::partial);
    EXPECT_EQ(result.range, std::optional(ByteRange{100, 199}));
}

TEST("range end is clamped to the resource") {
    const auto result = parse_range_header("bytes=900-2000", 1000);
    EXPECT_EQ(result.range, std::optional(ByteRange{900, 999}));
}

TEST("open-ended range reaches the resource end") {
    const auto result = parse_range_header("bytes=700-", 1000);
    EXPECT_EQ(result.range, std::optional(ByteRange{700, 999}));
}

TEST("suffix range selects bytes from the end") {
    const auto result = parse_range_header("bytes=-250", 1000);
    EXPECT_EQ(result.range, std::optional(ByteRange{750, 999}));
}

TEST("large suffix range selects the full resource") {
    const auto result = parse_range_header("bytes=-2000", 1000);
    EXPECT_EQ(result.range, std::optional(ByteRange{0, 999}));
}

TEST("out-of-bounds range is unsatisfiable") {
    const auto result = parse_range_header("bytes=1000-", 1000);
    EXPECT_EQ(result.status, RangeStatus::unsatisfiable);
    EXPECT_TRUE(!result.range.has_value());
}

TEST("multiple ranges are rejected explicitly") {
    const auto result = parse_range_header("bytes=0-10,20-30", 1000);
    EXPECT_EQ(result.status, RangeStatus::multiple_ranges_unsupported);
}

TEST("invalid range unit is malformed") {
    const auto result = parse_range_header("items=0-10", 1000);
    EXPECT_EQ(result.status, RangeStatus::malformed);
}

TEST("empty resource has no full range") {
    const auto result = parse_range_header("", 0);
    EXPECT_EQ(result.status, RangeStatus::full);
    EXPECT_TRUE(!result.range.has_value());
}
