package io.github.dimitrysaf.provenio.core.tracking.simkl

import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey
import io.github.dimitrysaf.provenio.desktop.Context
import io.github.dimitrysaf.provenio.desktop.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom

internal actual object SimklPlatformClock {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()
}

internal actual object SimklPkceCrypto {
    private val secureRandom = SecureRandom()

    actual fun secureRandomBytes(size: Int): ByteArray =
        ByteArray(size).also(secureRandom::nextBytes)

    actual fun sha256(value: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value)
}

/**
 * Android keeps these tokens encrypted with a Keystore key. Desktop has no equivalent without a
 * Secret Service integration yet, so they stay in the app's private preferences, which Flatpak
 * keeps inside the app's own data directory.
 */
internal actual object SimklAuthStorage {
    private const val PREFERENCES_NAME = "provenio_simkl_auth"
    private const val METADATA_KEY = "simkl_auth_metadata"
    private const val ACCESS_TOKEN_KEY = "simkl_access_token"
    private const val CODE_VERIFIER_KEY = "simkl_code_verifier"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadMetadataPayload(): String? =
        preferences?.getString(ProfileScopedKey.of(METADATA_KEY), null)

    actual fun saveMetadataPayload(payload: String) {
        preferences?.edit()?.putString(ProfileScopedKey.of(METADATA_KEY), payload)?.apply()
    }

    actual fun loadAccessToken(): String? = load(ACCESS_TOKEN_KEY)

    actual fun saveAccessToken(value: String?) = save(ACCESS_TOKEN_KEY, value)

    actual fun loadCodeVerifier(): String? = load(CODE_VERIFIER_KEY)

    actual fun saveCodeVerifier(value: String?) = save(CODE_VERIFIER_KEY, value)

    actual fun removeProfile(profileId: Int) {
        preferences?.edit()
            ?.remove(ProfileScopedKey.of(METADATA_KEY, profileId))
            ?.remove(ProfileScopedKey.of(ACCESS_TOKEN_KEY, profileId))
            ?.remove(ProfileScopedKey.of(CODE_VERIFIER_KEY, profileId))
            ?.apply()
    }

    private fun load(key: String): String? =
        preferences?.getString(ProfileScopedKey.of(key), null)?.takeIf { it.isNotBlank() }

    private fun save(key: String, value: String?) {
        val editor = preferences?.edit() ?: return
        if (value.isNullOrBlank()) {
            editor.remove(ProfileScopedKey.of(key)).apply()
        } else {
            editor.putString(ProfileScopedKey.of(key), value).apply()
        }
    }
}
