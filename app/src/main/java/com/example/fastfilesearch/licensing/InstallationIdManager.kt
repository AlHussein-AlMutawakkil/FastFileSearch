package com.example.fastfilesearch.licensing

import android.content.Context
import android.content.SharedPreferences
import com.example.fastfilesearch.util.ClientDeviceIdProvider
import java.security.SecureRandom

object InstallationIdManager {
    private const val PREFS_NAME = "installation_prefs"
    private const val KEY_INSTALLATION_ID = "installation_id"
    private const val KEY_INSTALL_NONCE = "install_nonce_v2"

    // Characters excluding ambiguous characters: 0, O, 1, I
    private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val random = SecureRandom()

    @Volatile
    private var cachedId: String? = null

    @Volatile
    private var cachedNonce: Int? = null

    @Synchronized
    fun getOrCreateInstallNonce(context: Context): Int {
        cachedNonce?.let { return it }

        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getInt(KEY_INSTALL_NONCE, -1)
        if (existing in 0..0xFFFF) {
            cachedNonce = existing
            return existing
        }

        val generated = random.nextInt(0x10000)
        prefs.edit().putInt(KEY_INSTALL_NONCE, generated).apply()
        cachedNonce = generated
        return generated
    }

    fun getR2InstallationId(context: Context): String {
        val fp = ClientDeviceIdProvider.getFingerprintInfo(context)
        val nonce = getOrCreateInstallNonce(context)
        return V2LicenseCodec.deriveInstallationId(fp.fpType, fp.digestPrefix6, nonce)
    }

    @Synchronized
    fun getInstallationId(context: Context): String {
        cachedId?.let { return it }

        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_INSTALLATION_ID, null)

        if (id.isNullOrBlank() || !isValidFormat(id)) {
            id = try {
                getR2InstallationId(context)
            } catch (_: Throwable) {
                generateNewId()
            }
            prefs.edit().putString(KEY_INSTALLATION_ID, id).apply()
        }

        cachedId = id
        return id
    }

    private fun generateNewId(): String {
        return buildString {
            append("BA-")
            append(randomBlock(4))
            append("-")
            append(randomBlock(4))
            append("-")
            append(randomBlock(4))
        }
    }

    private fun randomBlock(length: Int): String {
        val chars = CharArray(length)
        for (i in 0 until length) {
            chars[i] = ALPHABET[random.nextInt(ALPHABET.length)]
        }
        return String(chars)
    }

    fun isValidFormat(id: String): Boolean {
        // Must match BA-XXXX-XXXX-XXXX where X in ALPHABET
        val parts = id.split("-")
        if (parts.size != 4) return false
        if (parts[0] != "BA") return false
        for (i in 1..3) {
            val part = parts[i]
            if (part.length != 4) return false
            if (part.any { it !in ALPHABET }) return false
        }
        return true
    }

    // For testing / reset purposes only
    internal fun resetForTesting(context: Context) {
        cachedId = null
        cachedNonce = null
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
