package io.github.dimitrysaf.provenio.core.playback

/** A named section of the video, from its own chapter list, starting at [startMs]. */
data class PlayerChapter(val startMs: Long, val title: String)

/** The chapter playing at [positionMs]: the last one to have started by then. */
fun List<PlayerChapter>.chapterAt(positionMs: Long): PlayerChapter? = lastOrNull { it.startMs <= positionMs }
