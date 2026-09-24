#include "test_support.hpp"

#include "engine/file_selection.hpp"

using torrent::SelectionReason;
using torrent::TorrentFile;
using torrent::select_file;

namespace {

const std::vector<TorrentFile> files{
    {"Show/readme.txt", 100},
    {"Show/S01E01.mkv", 1'000},
    {"Show/S01E02.mkv", 2'000},
    {"Show/poster.jpg", 5'000},
};

}

TEST("exact filename hint wins over a stale requested index") {
    const auto result = select_file(files, 1, "S01E02.mkv");
    EXPECT_EQ(result.index, std::optional<std::size_t>(2));
    EXPECT_EQ(result.reason, SelectionReason::exact_filename);
}

TEST("canonical requested index wins when the hint does not match") {
    const auto result = select_file(files, 1, "missing.mkv");
    EXPECT_EQ(result.index, std::optional<std::size_t>(1));
    EXPECT_EQ(result.reason, SelectionReason::requested_index);
}

TEST("exact torrent path resolves case-insensitively") {
    const auto result = select_file(files, std::nullopt, "show/s01e01.MKV");
    EXPECT_EQ(result.index, std::optional<std::size_t>(1));
    EXPECT_EQ(result.reason, SelectionReason::exact_path);
}

TEST("exact basename resolves a packed episode") {
    const auto result = select_file(files, std::nullopt, "s01e02.mkv");
    EXPECT_EQ(result.index, std::optional<std::size_t>(2));
    EXPECT_EQ(result.reason, SelectionReason::exact_filename);
}

TEST("largest playable file beats a larger non-media file") {
    const auto result = select_file(files, 99, "");
    EXPECT_EQ(result.index, std::optional<std::size_t>(2));
    EXPECT_EQ(result.reason, SelectionReason::largest_playable_file);
}

TEST("empty metadata produces no selection") {
    const auto result = select_file({}, std::nullopt, "");
    EXPECT_TRUE(!result.index.has_value());
    EXPECT_EQ(result.reason, SelectionReason::no_files);
}
