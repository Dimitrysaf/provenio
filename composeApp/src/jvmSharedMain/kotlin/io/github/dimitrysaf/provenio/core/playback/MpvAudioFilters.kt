package io.github.dimitrysaf.provenio.core.playback

/**
 * The volume stabilizer as an mpv audio filter, the same as ExoPlayer's: a gentle compressor that
 * holds back loud scenes and lifts the rest a little, then a limiter that only catches peaks. Its
 * attack and release are slow enough to follow scenes, not each wave of the sound, which is what
 * distorts it.
 */
private const val MpvVolumeStabilizerFilter =
    "@provenio_stabilizer:lavfi=[acompressor=threshold=-28dB:ratio=3:attack=25:release=400:knee=6dB:makeup=6dB," +
        "alimiter=limit=-1dB:level=0]"

/**
 * Measures loudness for subtitle sync without changing the sound: FFmpeg's audio statistics,
 * restarted every few frames, written into each frame's metadata.
 */
private const val MpvAudioLevelFilter = "@provenio_level:lavfi=[astats=metadata=1:reset=4]"

/** Where mpv reports the level filter's latest loudness, in dB. */
const val MpvAudioLevelProperty = "af-metadata/provenio_level/by-key/lavfi.astats.Overall.RMS_level"

/**
 * How far ahead of what is heard mpv's filters run: the audio it buffers for output, 0.2 s by
 * default.
 */
const val MpvAudioLeadMs = 200L

/**
 * The `af` value for the player's audio filters. Loudness is measured first, so the stabilizer
 * never evens out what subtitle sync listens to.
 */
fun mpvAudioFilterChain(stabilizer: Boolean, levelProbe: Boolean): String =
    listOfNotNull(
        MpvAudioLevelFilter.takeIf { levelProbe },
        MpvVolumeStabilizerFilter.takeIf { stabilizer },
    ).joinToString(",")

/** The loudness [MpvAudioLevelProperty] reports, with silence as a very low level. */
fun parseMpvAudioLevelDb(value: String?): Float? {
    val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (text.contains("inf", ignoreCase = true)) return MpvSilenceDb
    return text.toFloatOrNull()?.coerceAtLeast(MpvSilenceDb)
}

private const val MpvSilenceDb = -100f
