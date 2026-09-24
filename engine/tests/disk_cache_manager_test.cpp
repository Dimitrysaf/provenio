#include "test_support.hpp"

#include "cache/disk_cache_manager.hpp"

#include <chrono>
#include <filesystem>
#include <fstream>
#include <string>
#include <unordered_set>

namespace {

class DiskCacheFixture {
public:
    DiskCacheFixture() {
        const auto nonce = std::chrono::steady_clock::now().time_since_epoch().count();
        root_ = std::filesystem::temp_directory_path() /
            ("disk-cache-" + std::to_string(nonce));
        outside_ = root_.string() + "-outside";
        std::filesystem::create_directories(root_);
    }

    ~DiskCacheFixture() {
        std::error_code ignored;
        std::filesystem::remove_all(root_, ignored);
        std::filesystem::remove_all(outside_, ignored);
    }

    void put(const std::string& id, const std::string& bytes) {
        const auto directory = root_ / id;
        std::filesystem::create_directories(directory);
        std::ofstream output(directory / "payload.bin", std::ios::binary);
        output << bytes;
    }

    void put_sparse(const std::string& id, const std::uint64_t logical_size) {
        const auto directory = root_ / id;
        std::filesystem::create_directories(directory);
        std::ofstream output(directory / "payload.bin", std::ios::binary);
        output.seekp(static_cast<std::streamoff>(logical_size - 1));
        output.put('\0');
    }

    [[nodiscard]] const std::filesystem::path& root() const {
        return root_;
    }

    [[nodiscard]] const std::filesystem::path& outside() const {
        return outside_;
    }

private:
    std::filesystem::path root_;
    std::filesystem::path outside_;
};

constexpr auto first_id = "0000000000000000000000000000000000000001";
constexpr auto second_id = "0000000000000000000000000000000000000002";

}

TEST("disk cache evicts inactive hash trees but protects live torrents") {
    DiskCacheFixture fixture;
    fixture.put(first_id, "1234");
    fixture.put(second_id, "5678");
    cache::DiskCacheManager cache(fixture.root(), 4);

    const auto stats = cache.enforce({second_id});

    EXPECT_TRUE(!std::filesystem::exists(fixture.root() / first_id));
    EXPECT_TRUE(std::filesystem::exists(fixture.root() / second_id));
    EXPECT_EQ(stats.used_bytes, std::uint64_t(4));
    EXPECT_EQ(stats.protected_bytes, std::uint64_t(4));
    EXPECT_EQ(stats.evictions, std::uint64_t(1));
    EXPECT_TRUE(!stats.over_budget);
}

TEST("disk cache reports protected bytes over budget without deleting them") {
    DiskCacheFixture fixture;
    fixture.put(first_id, "12345");
    cache::DiskCacheManager cache(fixture.root(), 4);

    const auto stats = cache.enforce({first_id});

    EXPECT_TRUE(std::filesystem::exists(fixture.root() / first_id));
    EXPECT_EQ(stats.used_bytes, std::uint64_t(5));
    EXPECT_EQ(stats.protected_bytes, std::uint64_t(5));
    EXPECT_TRUE(stats.over_budget);
}

TEST("disk cache accounts sparse payload allocation instead of logical length") {
    DiskCacheFixture fixture;
    constexpr std::uint64_t logical_size = 16 * 1024 * 1024;
    fixture.put_sparse(first_id, logical_size);
    cache::DiskCacheManager cache(fixture.root(), logical_size / 2);

    const auto stats = cache.enforce({first_id});

    EXPECT_TRUE(std::filesystem::exists(fixture.root() / first_id));
    if (stats.used_bytes < logical_size) {
        EXPECT_TRUE(stats.used_bytes < logical_size / 2);
        EXPECT_TRUE(!stats.over_budget);
    }
}

TEST("disk cache evicts the least recently touched inactive torrent") {
    DiskCacheFixture fixture;
    fixture.put(first_id, "1234");
    fixture.put(second_id, "5678");
    const auto old_time = std::filesystem::file_time_type::clock::now() -
        std::chrono::hours(1);
    std::filesystem::last_write_time(fixture.root() / first_id, old_time);
    std::filesystem::last_write_time(fixture.root() / second_id, old_time);
    cache::DiskCacheManager cache(fixture.root(), 4);
    cache.touch(second_id);

    const auto stats = cache.enforce({});

    EXPECT_TRUE(!std::filesystem::exists(fixture.root() / first_id));
    EXPECT_TRUE(std::filesystem::exists(fixture.root() / second_id));
    EXPECT_EQ(stats.used_bytes, std::uint64_t(4));
    EXPECT_EQ(stats.evictions, std::uint64_t(1));
}

TEST("disk cache supports an immediate reclaim target below its normal capacity") {
    DiskCacheFixture fixture;
    fixture.put(first_id, "1234");
    fixture.put(second_id, "5678");
    cache::DiskCacheManager cache(fixture.root(), 8);

    const auto stats = cache.enforce({}, 0);

    EXPECT_TRUE(!std::filesystem::exists(fixture.root() / first_id));
    EXPECT_TRUE(!std::filesystem::exists(fixture.root() / second_id));
    EXPECT_EQ(stats.capacity_bytes, std::uint64_t(8));
    EXPECT_EQ(stats.used_bytes, std::uint64_t(0));
    EXPECT_EQ(stats.evictions, std::uint64_t(2));
    EXPECT_TRUE(!stats.over_budget);
}

TEST("disk cache scan never follows a hash-shaped symlink") {
    DiskCacheFixture fixture;
    const auto outside = fixture.outside();
    std::filesystem::create_directories(outside);
    {
        std::ofstream output(outside / "valuable.bin", std::ios::binary);
        output << "keep";
    }
    std::error_code error;
    std::filesystem::create_directory_symlink(outside, fixture.root() / first_id, error);
    if (error) {
        return;
    }
    cache::DiskCacheManager cache(fixture.root(), 0);

    bool rejected = false;
    try {
        static_cast<void>(cache.enforce({}));
    } catch (const std::runtime_error&) {
        rejected = true;
    }

    EXPECT_TRUE(rejected);
    EXPECT_TRUE(std::filesystem::exists(outside / "valuable.bin"));
}
