package io.github.dimitrysaf.provenio.core.playback

/**
 * The volume stabilizer as an mpv audio filter: FFmpeg's dynamic audio normalizer, tuned to react
 * within a fraction of a second, so quiet dialogue is lifted and loud scenes are held back.
 */
private const val MpvVolumeStabilizerFilter = "@provenio_stabilizer:lavfi=[dynaudnorm=f=150:g=15:p=0.9:m=10]"

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
