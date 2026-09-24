#include "test_support.hpp"

#include "cache/verified_piece_cache.hpp"

#include <memory>
#include <string>
#include <vector>

namespace {

std::shared_ptr<std::vector<char>> bytes(const std::string& value) {
    return std::make_shared<std::vector<char>>(value.begin(), value.end());
}

}

TEST("verified piece cache evicts least recently used data within its byte budget") {
    cache::VerifiedPieceCache cache(8);
    cache.put({"torrent", 0}, bytes("aaaa"));
    cache.put({"torrent", 1}, bytes("bbbb"));
    EXPECT_TRUE(cache.get({"torrent", 0}) != nullptr);
    cache.put({"torrent", 2}, bytes("cccc"));

    EXPECT_TRUE(cache.get({"torrent", 0}) != nullptr);
    EXPECT_TRUE(cache.get({"torrent", 1}) == nullptr);
    EXPECT_TRUE(cache.get({"torrent", 2}) != nullptr);
    const auto stats = cache.stats();
    EXPECT_EQ(stats.used_bytes, std::uint64_t(8));
    EXPECT_EQ(stats.entries, std::uint64_t(2));
    EXPECT_EQ(stats.evictions, std::uint64_t(1));
}

TEST("verified piece cache removes one torrent without touching another") {
    cache::VerifiedPieceCache cache(16);
    cache.put({"first", 0}, bytes("aaaa"));
    cache.put({"second", 0}, bytes("bbbb"));
    cache.erase_torrent("first");

    EXPECT_TRUE(cache.get({"first", 0}) == nullptr);
    EXPECT_TRUE(cache.get({"second", 0}) != nullptr);
    EXPECT_EQ(cache.stats().used_bytes, std::uint64_t(4));
}

TEST("verified piece cache refuses an entry larger than its capacity") {
    cache::VerifiedPieceCache cache(3);
    cache.put({"torrent", 0}, bytes("four"));
    EXPECT_TRUE(cache.get({"torrent", 0}) == nullptr);
    EXPECT_EQ(cache.stats().used_bytes, std::uint64_t(0));
}
