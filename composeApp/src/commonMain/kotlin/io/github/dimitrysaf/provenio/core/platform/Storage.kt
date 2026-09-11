package io.github.dimitrysaf.provenio.core.platform

/**
 * Absolute path of a directory the app may fill and empty at will.
 *
 * Torrent pieces land here. It is deliberately a cache location: the data is
 * reconstructible from the swarm, so the operating system is welcome to reclaim it.
 */
expect fun cacheDirectoryPath(): String
