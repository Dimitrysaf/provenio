package io.github.dimitrysaf.provenio.p2p

import kotlinx.coroutines.delay
import org.libtorrent4j.Priority
import org.libtorrent4j.TorrentFlags
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile

/**
 * One torrent, read as a stream rather than downloaded and then opened.
 *
 * The whole trick is ordering. A torrent left alone fetches whichever pieces are rarest,
 * which is the right answer for finishing a download and the wrong one for watching: the
 * player wants the byte after the one it just read. So the file is put in sequential mode
 * and, on top of that, the piece under the read head is given a deadline so libtorrent
 * fetches it ahead of everything else — including when the user seeks and the read head
 * jumps somewhere the sequential order would not reach for another hour.
 */
class StreamingTorrent(
    private val handle: TorrentHandle,
    private val info: TorrentInfo,
    savePath: String,
    preferredFileIndex: Int?,
) {
    private val files = info.files()
    private val index = preferredFileIndex?.takeIf { it in 0 until info.numFiles() }
        ?: largestVideoFile(info)

    private val fileOffset: Long = files.fileOffset(index)
    private val pieceLength: Long = info.pieceLength().toLong()
    private val path = File(files.filePath(index, savePath))

    /** Total size of the file being served, which is what a Range request is relative to. */
    val length: Long = files.fileSize(index)

    val fileName: String = files.fileName(index)

    /**
     * Everything that only has to happen once: ignore the other files, switch to
     * sequential, and pull in both ends.
     *
     * Both ends, because a container keeps its index at one or the other — MP4 may carry
     * its moov atom at the tail, and a player that cannot read the index cannot start at
     * all, let alone seek.
     */
    fun prepare() {
        for (other in 0 until info.numFiles()) {
            handle.filePriority(other, if (other == index) Priority.DEFAULT else Priority.IGNORE)
        }
        handle.setFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)

        val first = pieceOf(0)
        val last = pieceOf(length - 1)
        for (piece in first until minOf(first + EndPieces, last)) {
            urgent(piece, DeadlineNowMillis)
        }
        for (piece in maxOf(last - EndPieces + 1, first)..last) {
            urgent(piece, DeadlineNowMillis)
        }
    }

    /**
     * Writes [count] bytes starting at [start] into [out], waiting for each piece to
     * arrive rather than failing when it has not.
     *
     * This is what makes seeking work: the player asks for a range, and the range it asks
     * for is what gets fetched first.
     */
    suspend fun writeRange(out: OutputStream, start: Long, count: Long) {
        // The file exists on disk only once libtorrent has allocated it, so waiting for
        // the first piece is also waiting for something to open.
        awaitPiece(pieceOf(start))
        if (!path.exists()) return

        RandomAccessFile(path, "r").use { source ->
            val buffer = ByteArray(ChunkBytes)
            var position = start
            var remaining = count
            var emptyReads = 0

            while (remaining > 0 && handle.isValid && position < length) {
                val piece = pieceOf(position)
                awaitPiece(piece)
                readAhead(piece + 1)

                // Never read past the end of a piece that has arrived into one that has
                // not: the bytes would be zeroes and the player would see corruption
                // rather than a wait.
                val untilPieceEnd = endOf(piece) - position + 1
                val wanted = minOf(remaining, ChunkBytes.toLong(), untilPieceEnd).toInt()
                source.seek(position)
                val read = source.read(buffer, 0, wanted)
                if (read <= 0) {
                    // The piece is accounted for but the bytes are not on disk yet.
                    // Bounded, because a file shorter than the torrent claims would
                    // otherwise spin here for as long as the player kept reading.
                    if (++emptyReads > MaxEmptyReads) break
                    delay(PollIntervalMillis)
                    continue
                }

                emptyReads = 0
                out.write(buffer, 0, read)
                position += read
                remaining -= read
            }
            out.flush()
        }
    }

    /** Lets go of the deadlines this stream asked for, leaving the torrent itself alone. */
    fun release() {
        if (handle.isValid) handle.clearPieceDeadlines()
    }

    /** Which piece holds [positionInFile], in the torrent's own piece numbering. */
    private fun pieceOf(positionInFile: Long): Int =
        ((fileOffset + positionInFile) / pieceLength).toInt()

    /** The last byte of [piece], as an offset into the file being served. */
    private fun endOf(piece: Int): Long = (piece + 1) * pieceLength - 1 - fileOffset

    private suspend fun awaitPiece(piece: Int) {
        if (handle.havePiece(piece)) return
        urgent(piece, DeadlineNowMillis)
        while (handle.isValid && !handle.havePiece(piece)) {
            delay(PollIntervalMillis)
        }
    }

    /**
     * Queues the pieces just past the read head so playback does not stall the moment the
     * current one is consumed. Deadlines step up with distance, which is how libtorrent is
     * told the order to want them in.
     */
    private fun readAhead(from: Int) {
        val last = minOf(from + ReadAheadPieces, info.numPieces() - 1)
        for (piece in from..last) {
            if (!handle.havePiece(piece)) {
                urgent(piece, (piece - from + 1) * DeadlineStepMillis)
            }
        }
    }

    private fun urgent(piece: Int, deadlineMillis: Int) {
        if (piece < 0 || piece >= info.numPieces()) return
        handle.piecePriority(piece, Priority.TOP_PRIORITY)
        handle.setPieceDeadline(piece, deadlineMillis)
    }

    private companion object {
        const val ChunkBytes = 64 * 1024
        const val PollIntervalMillis = 50L
        const val ReadAheadPieces = 8
        const val EndPieces = 4
        const val DeadlineNowMillis = 0
        const val DeadlineStepMillis = 300
        const val MaxEmptyReads = 200
    }
}

/**
 * The file worth playing.
 *
 * A release torrent is rarely one file: there are samples, subtitles, artwork and a readme
 * beside the feature. The largest video is the feature in every case worth handling, and
 * an addon that knows better can say so instead.
 */
private fun largestVideoFile(info: TorrentInfo): Int {
    val files = info.files()
    var best = 0
    var bestSize = -1L
    var bestIsVideo = false

    for (index in 0 until info.numFiles()) {
        val size = files.fileSize(index)
        val isVideo = files.fileName(index)
            .substringAfterLast('.', "")
            .lowercase() in VideoExtensions
        val better = when {
            isVideo && !bestIsVideo -> true
            isVideo == bestIsVideo -> size > bestSize
            else -> false
        }
        if (better) {
            best = index
            bestSize = size
            bestIsVideo = isVideo
        }
    }
    return best
}

private val VideoExtensions = setOf(
    "mkv", "mp4", "avi", "mov", "m4v", "webm", "ts", "m2ts", "mpg", "mpeg", "wmv", "flv",
)
