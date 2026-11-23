package com.example.fastfilesearch.security

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class AppLockMode(val titleArabic: String) {
    DISABLED("معطّل"),
    BIOMETRIC_ONLY("البصمة فقط"),
    PASSCODE_ONLY("كلمة مرور فقط"),
    BOTH("البصمة وكلمة المرور")
}

enum class AutoLockTimeout(val titleArabic: String, val durationMs: Long) {
    IMMEDIATE("فوراً", 0L),
    ONE_MINUTE("بعد دقيقة", 60_000L),
    FIVE_MINUTES("بعد 5 دقائق", 300_000L)
}

object CryptoHelper {
    private const val ITERATIONS = 10000
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

class AppLockManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _lockMode = MutableStateFlow(loadLockMode())
    val lockModeFlow: StateFlow<AppLockMode> = _lockMode.asStateFlow()

    private val _autoLockTimeout = MutableStateFlow(loadAutoLockTimeout())
    val autoLockTimeoutFlow: StateFlow<AutoLockTimeout> = _autoLockTimeout.asStateFlow()

    private val _isLocked = MutableStateFlow(_lockMode.value != AppLockMode.DISABLED)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    var lastBackgroundTimestamp: Long = 0L
        internal set

    var suppressNextLock: Boolean = false
        internal set

    var failedAttempts: Int = 0
        internal set

    var lockoutUntilTimestamp: Long = 0L
        internal set

    val lockMode: AppLockMode
        get() = _lockMode.value

    val autoLockTimeout: AutoLockTimeout
        get() = _autoLockTimeout.value

    fun hasPasscode(): Boolean {
        return prefs.contains(KEY_PASSCODE_HASH) && prefs.contains(KEY_PASSCODE_SALT)
    }

    fun setPasscode(passcode: String) {
        val salt = CryptoHelper.generateSalt(16)
        val hash = CryptoHelper.hashPasscode(passcode, salt)
        prefs.edit()
            .putString(KEY_PASSCODE_SALT, Base64.getEncoder().encodeToString(salt))
            .putString(KEY_PASSCODE_HASH, Base64.getEncoder().encodeToString(hash))
            .apply()
    }

    fun clearPasscode() {
        prefs.edit()
            .remove(KEY_PASSCODE_SALT)
            .remove(KEY_PASSCODE_HASH)
            .apply()
    }

    fun verifyPasscode(passcode: String, currentTime: Long = System.currentTimeMillis()): Boolean {
        if (getRemainingLockoutSeconds(currentTime) > 0) {
            return false
        }

        val saltStr = prefs.getString(KEY_PASSCODE_SALT, null) ?: return false
        val hashStr = prefs.getString(KEY_PASSCODE_HASH, null) ?: return false

        val salt = Base64.getDecoder().decode(saltStr)
        val expectedHash = Base64.getDecoder().decode(hashStr)

        val success = CryptoHelper.verifyPasscode(passcode, salt, expectedHash)
        if (success) {
            failedAttempts = 0
            lockoutUntilTimestamp = 0L
            unlock()
        } else {
            failedAttempts++
            if (failedAttempts >= 5) {
                lockoutUntilTimestamp = currentTime + 30_000L
            }
        }
        return success
    }

    fun getRemainingLockoutSeconds(currentTime: Long = System.currentTimeMillis()): Int {
        return if (currentTime < lockoutUntilTimestamp) {
            val remainingMs = lockoutUntilTimestamp - currentTime
            maxOf(1, ((remainingMs + 999) / 1000).toInt())
        } else {
            0
        }
    }

    fun setLockMode(mode: AppLockMode) {
        prefs.edit().putString(KEY_LOCK_MODE, mode.name).apply()
        _lockMode.value = mode
        if (mode == AppLockMode.DISABLED) {
            _isLocked.value = false
            failedAttempts = 0
            lockoutUntilTimestamp = 0L
        }
    }

    fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        prefs.edit().putString(KEY_AUTO_LOCK_TIMEOUT, timeout.name).apply()
        _autoLockTimeout.value = timeout
    }

    fun lock() {
        if (_lockMode.value != AppLockMode.DISABLED) {
            _isLocked.value = true
        }
    }

    fun unlock() {
        _isLocked.value = false
        failedAttempts = 0
        lockoutUntilTimestamp = 0L
    }

    fun suppressNextLockOnce() {
        suppressNextLock = true
    }

    fun onAppBackgrounded(timestamp: Long = System.currentTimeMillis()) {
        if (suppressNextLock) {
            suppressNextLock = false
            return
        }
        lastBackgroundTimestamp = timestamp
        if (_autoLockTimeout.value == AutoLockTimeout.IMMEDIATE && _lockMode.value != AppLockMode.DISABLED) {
            _isLocked.value = true
        }
    }

    fun onAppForegrounded(timestamp: Long = System.currentTimeMillis()) {
        if (_lockMode.value == AppLockMode.DISABLED) {
            _isLocked.value = false
            return
        }
        if (_isLocked.value) {
            return
        }
        if (lastBackgroundTimestamp > 0L) {
            val elapsed = timestamp - lastBackgroundTimestamp
            if (elapsed >= _autoLockTimeout.value.durationMs) {
                _isLocked.value = true
            }
        }
        lastBackgroundTimestamp = 0L
    }

    companion object {
        private const val PREFS_NAME = "app_lock_prefs"
        private const val KEY_LOCK_MODE = "lock_mode"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        private const val KEY_PASSCODE_HASH = "passcode_hash"
        private const val KEY_PASSCODE_SALT = "passcode_salt"

        @Volatile
        private var instance: AppLockManager? = null

        fun getInstance(context: Context): AppLockManager {
            return instance ?: synchronized(this) {
                instance ?: AppLockManager(context.applicationContext).also { instance = it }
            }
        }

        fun canAuthenticateBiometric(context: Context): Boolean {
            val biometricManager = BiometricManager.from(context)
            return biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            ) == BiometricManager.BIOMETRIC_SUCCESS
        }

        fun showBiometricPrompt(
            activity: FragmentActivity,
            title: String = "فتح التطبيق",
            subtitle: String = "استخدم بصمة الإصبع للمتابعة",
            negativeButtonText: String = "إلغاء",
            onSuccess: () -> Unit,
            onError: (String) -> Unit = {}
        ) {
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                .build()

            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun loadLockMode(): AppLockMode {
        val name = prefs.getString(KEY_LOCK_MODE, AppLockMode.DISABLED.name)
        return try {
            AppLockMode.valueOf(name ?: AppLockMode.DISABLED.name)
        } catch (_: Exception) {
            AppLockMode.DISABLED
        }
    }

    private fun loadAutoLockTimeout(): AutoLockTimeout {
        val name = prefs.getString(KEY_AUTO_LOCK_TIMEOUT, AutoLockTimeout.IMMEDIATE.name)
        return try {
            AutoLockTimeout.valueOf(name ?: AutoLockTimeout.IMMEDIATE.name)
        } catch (_: Exception) {
            AutoLockTimeout.IMMEDIATE
        }
    }
}
