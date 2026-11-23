package com.example.fastfilesearch.licensing

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom

object InstallationIdManager {
    private const val PREFS_NAME = "installation_prefs"
    private const val KEY_INSTALLATION_ID = "installation_id"

    // Characters excluding ambiguous characters: 0, O, 1, I
    private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val random = SecureRandom()

    @Volatile
    private var cachedId: String? = null

    @Synchronized
    fun getInstallationId(context: Context): String {
        cachedId?.let { return it }

        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_INSTALLATION_ID, null)

        if (id.isNullOrBlank() || !isValidFormat(id)) {
            id = generateNewId()
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
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
