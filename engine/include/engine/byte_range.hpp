#ifndef ENGINE_BYTE_RANGE_HPP
#define ENGINE_BYTE_RANGE_HPP

#include <cstdint>
#include <optional>
#include <string_view>

#include "engine/export.h"

namespace http {

struct ByteRange {
    std::uint64_t start;
    std::uint64_t end;

    [[nodiscard]] ENGINE_CPP_API std::uint64_t length() const;
    [[nodiscard]] bool operator==(const ByteRange&) const = default;
};

enum class RangeStatus {
    full,
    partial,
    malformed,
    unsatisfiable,
    multiple_ranges_unsupported,
};

struct RangeResult {
    RangeStatus status;
    std::optional<ByteRange> range;

    [[nodiscard]] ENGINE_CPP_API bool is_success() const;
};

[[nodiscard]] ENGINE_CPP_API RangeResult parse_range_header(
    std::string_view header,
    std::uint64_t resource_size
);

}

#endif
