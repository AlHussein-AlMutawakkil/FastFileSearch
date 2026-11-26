package com.example.fastfilesearch.desktop.licensing

import com.example.fastfilesearch.desktop.storage.DesktopAppStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class DesktopAppLockMode(val titleArabic: String) {
    DISABLED("معطّل"),
    PASSCODE_ONLY("كلمة مرور")
}

enum class DesktopAutoLockTimeout(val titleArabic: String, val durationMs: Long) {
    ON_STARTUP("عند تشغيل التطبيق فقط", -1L),
    ONE_MINUTE("بعد دقيقة من عدم النشاط", 60_000L),
    FIVE_MINUTES("بعد 5 دقائق من عدم النشاط", 300_000L),
    FIFTEEN_MINUTES("بعد 15 دقيقة من عدم النشاط", 900_000L)
}

object DesktopCryptoHelper {
    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    fun generateSalt(size: Int = 16): ByteArray {
        val salt = ByteArray(size)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun hashPasscode(passcode: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(passcode.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    fun verifyPasscode(passcode: String, salt: ByteArray, expectedHash: ByteArray): Boolean {
        val computed = hashPasscode(passcode, salt)
        return MessageDigest.isEqual(computed, expectedHash)
    }
}

/**
 * Desktop password lock manager with PBKDF2-HMAC-SHA256 storage,
 * brute-force lockout protection, and idle inactivity auto-lock.
 */
class DesktopAppLockManager(
    baseDir: File = DesktopAppStorage.defaultAppDataDir(),
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() }
) {
    companion object {
        private const val KEY_LOCK_MODE = "lock_mode"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        private const val KEY_PASSCODE_SALT = "passcode_salt"
        private const val KEY_PASSCODE_HASH = "passcode_hash"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30_000L
    }

    private val securityFile: File = DesktopAppStorage.getSecurityFile(baseDir)

    private val _lockMode = MutableStateFlow(loadLockMode())
    val lockModeFlow: StateFlow<DesktopAppLockMode> = _lockMode.asStateFlow()

    private val _autoLockTimeout = MutableStateFlow(loadAutoLockTimeout())
    val autoLockTimeoutFlow: StateFlow<DesktopAutoLockTimeout> = _autoLockTimeout.asStateFlow()

    private val _isLocked = MutableStateFlow(_lockMode.value != DesktopAppLockMode.DISABLED && hasPasscode())
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    var lastActivityTimestamp: Long = nowMillisProvider()
        internal set

    var failedAttempts: Int = 0
        internal set

    var lockoutUntilTimestamp: Long = 0L
        internal set

    val lockMode: DesktopAppLockMode
        get() = _lockMode.value

    val autoLockTimeout: DesktopAutoLockTimeout
        get() = _autoLockTimeout.value

    @Synchronized
    fun hasPasscode(): Boolean {
        val props = DesktopAppStorage.loadProperties(securityFile)
        return !props.getProperty(KEY_PASSCODE_HASH).isNullOrBlank() &&
            !props.getProperty(KEY_PASSCODE_SALT).isNullOrBlank()
    }

    @Synchronized
    fun setPasscode(passcode: String) {
        val salt = DesktopCryptoHelper.generateSalt(16)
        val hash = DesktopCryptoHelper.hashPasscode(passcode, salt)
        val props = DesktopAppStorage.loadProperties(securityFile)
        props.setProperty(KEY_PASSCODE_SALT, Base64.getEncoder().encodeToString(salt))
        props.setProperty(KEY_PASSCODE_HASH, Base64.getEncoder().encodeToString(hash))
        DesktopAppStorage.saveProperties(securityFile, props)
    }

    @Synchronized
    fun clearPasscode() {
        val props = DesktopAppStorage.loadProperties(securityFile)
        props.remove(KEY_PASSCODE_SALT)
        props.remove(KEY_PASSCODE_HASH)
        DesktopAppStorage.saveProperties(securityFile, props)
    }

    @Synchronized
    fun verifyPasscode(passcode: String, currentTime: Long = nowMillisProvider()): Boolean {
        if (getRemainingLockoutSeconds(currentTime) > 0) {
            return false
        }

        val props = DesktopAppStorage.loadProperties(securityFile)
        val saltStr = props.getProperty(KEY_PASSCODE_SALT) ?: return false
        val hashStr = props.getProperty(KEY_PASSCODE_HASH) ?: return false

        val salt = Base64.getDecoder().decode(saltStr)
        val expectedHash = Base64.getDecoder().decode(hashStr)

        val success = DesktopCryptoHelper.verifyPasscode(passcode, salt, expectedHash)
        if (success) {
            failedAttempts = 0
            lockoutUntilTimestamp = 0L
            lastActivityTimestamp = currentTime
            _isLocked.value = false
        } else {
            failedAttempts++
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                lockoutUntilTimestamp = currentTime + LOCKOUT_DURATION_MS
            }
        }
        return success
    }

    fun getRemainingLockoutSeconds(currentTime: Long = nowMillisProvider()): Int {
        return if (currentTime < lockoutUntilTimestamp) {
            val remainingMs = lockoutUntilTimestamp - currentTime
            maxOf(1, ((remainingMs + 999) / 1000).toInt())
        } else {
            0
        }
    }

    @Synchronized
    fun setLockMode(mode: DesktopAppLockMode) {
        val props = DesktopAppStorage.loadProperties(securityFile)
        props.setProperty(KEY_LOCK_MODE, mode.name)
        DesktopAppStorage.saveProperties(securityFile, props)
        _lockMode.value = mode
        if (mode == DesktopAppLockMode.DISABLED) {
            _isLocked.value = false
            failedAttempts = 0
            lockoutUntilTimestamp = 0L
        } else {
            lastActivityTimestamp = nowMillisProvider()
        }
    }

    @Synchronized
    fun setAutoLockTimeout(timeout: DesktopAutoLockTimeout) {
        val props = DesktopAppStorage.loadProperties(securityFile)
        props.setProperty(KEY_AUTO_LOCK_TIMEOUT, timeout.name)
        DesktopAppStorage.saveProperties(securityFile, props)
        _autoLockTimeout.value = timeout
        lastActivityTimestamp = nowMillisProvider()
    }

    fun lockNow() {
        if (_lockMode.value != DesktopAppLockMode.DISABLED && hasPasscode()) {
            _isLocked.value = true
        }
    }

    fun recordUserActivity(currentTime: Long = nowMillisProvider()) {
        lastActivityTimestamp = currentTime
    }

    fun checkIdleTimeout(currentTime: Long = nowMillisProvider()): Boolean {
        if (_lockMode.value == DesktopAppLockMode.DISABLED || !hasPasscode()) return false
        if (_isLocked.value) return true
        val timeoutMs = _autoLockTimeout.value.durationMs
        if (timeoutMs <= 0L) return false
        if (currentTime - lastActivityTimestamp >= timeoutMs) {
            _isLocked.value = true
            return true
        }
        return false
    }

    private fun loadLockMode(): DesktopAppLockMode {
        val raw = DesktopAppStorage.loadProperties(securityFile).getProperty(KEY_LOCK_MODE)
        return try {
            DesktopAppLockMode.valueOf(raw ?: DesktopAppLockMode.DISABLED.name)
        } catch (_: Exception) {
            DesktopAppLockMode.DISABLED
        }
    }

    private fun loadAutoLockTimeout(): DesktopAutoLockTimeout {
        val raw = DesktopAppStorage.loadProperties(securityFile).getProperty(KEY_AUTO_LOCK_TIMEOUT)
        return try {
            DesktopAutoLockTimeout.valueOf(raw ?: DesktopAutoLockTimeout.FIVE_MINUTES.name)
        } catch (_: Exception) {
            DesktopAutoLockTimeout.FIVE_MINUTES
        }
    }
}
