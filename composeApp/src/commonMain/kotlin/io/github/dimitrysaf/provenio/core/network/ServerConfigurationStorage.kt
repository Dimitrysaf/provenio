package io.github.dimitrysaf.provenio.core.network

internal expect object ServerConfigurationStorage {
    fun loadCustom(): ServerConfiguration?
    fun saveCustom(configuration: ServerConfiguration): Boolean
    fun useOfficial(): Boolean
}
