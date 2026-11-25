package com.example.fastfilesearch.licensing

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.example.fastfilesearch.util.ClientDeviceIdProvider
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class LicenseManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "license_prefs"
        private const val KEY_LICENSE_CODE = "license_code"
        private const val KEY_LAST_WALL_TIME = "last_known_wall_time_ms"
        private const val KEY_LAST_ELAPSED_TIME = "last_elapsed_realtime_ms"
        private const val KEY_CLOCK_HMAC = "clock_hmac"

        private const val FIVE_MINUTES_MS = 5 * 60 * 1000L // 300,000 ms

        @Volatile
        private var instance: LicenseManager? = null

        fun getInstance(context: Context): LicenseManager {
            return instance ?: synchronized(this) {
                instance ?: LicenseManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val installationId: String
        get() = InstallationIdManager.getInstallationId(context)

    @Synchronized
    fun activateLicense(rawCode: String): LicenseVerificationResult {
        val fp = ClientDeviceIdProvider.getFingerprintInfo(context)
        val nonce = InstallationIdManager.getOrCreateInstallNonce(context)
        val verification = LicenseVerifier.verify(
            rawCode = rawCode,
            expectedInstallationId = installationId,
            deviceFpType = fp.fpType,
            deviceHash6 = fp.digestPrefix6,
            installNonce = nonce
        )

        if (verification.status == LicenseStatus.ACTIVE && verification.payload != null) {
            val cleaned = LicenseVerifier.cleanLicenseCode(rawCode)
            prefs.edit().putString(KEY_LICENSE_CODE, cleaned).apply()
            recordActivityTime()
            return checkLicenseStatus(verification.payload)
        }

        return verification
    }

    @Synchronized
    fun getSavedLicenseCode(): String? {
        return prefs.getString(KEY_LICENSE_CODE, null)?.takeIf { it.isNotBlank() }
    }

    @Synchronized
    fun clearLicense() {
        prefs.edit().remove(KEY_LICENSE_CODE).apply()
    }

    @Synchronized
    fun checkCurrentLicense(): LicenseVerificationResult {
        val savedCode = getSavedLicenseCode()
        if (savedCode.isNullOrBlank()) {
            return LicenseVerificationResult(
                status = LicenseStatus.NOT_ACTIVATED,
                errorMessage = "لم يتم تفعيل التطخيص بعد"
            )
        }

        val fp = ClientDeviceIdProvider.getFingerprintInfo(context)
        val nonce = InstallationIdManager.getOrCreateInstallNonce(context)
        val verification = LicenseVerifier.verify(
            rawCode = savedCode,
            expectedInstallationId = installationId,
            deviceFpType = fp.fpType,
            deviceHash6 = fp.digestPrefix6,
            installNonce = nonce
        )

        if (verification.status != LicenseStatus.ACTIVE || verification.payload == null) {
            return verification
        }

        return checkLicenseStatus(verification.payload)
    }

    @Synchronized
    fun getLicenseStatus(): LicenseStatus {
        return checkCurrentLicense().status
    }

    @Synchronized
    fun getActivePayload(): LicensePayload? {
        val res = checkCurrentLicense()
        return if (res.status == LicenseStatus.ACTIVE) res.payload else null
    }

    fun isLicenseActive(): Boolean {
        return getLicenseStatus() == LicenseStatus.ACTIVE
    }

    // Permission checks
    fun canAddWorkspace(currentWorkspaceCount: Int): Boolean {
        val payload = getActivePayload() ?: return false
        if (payload.maxWorkspaces <= 0) return true
        return currentWorkspaceCount < payload.maxWorkspaces
    }

    fun canExportExcel(): Boolean {
        val payload = getActivePayload() ?: return false
        return payload.allowExcelExport
    }

    fun canUseAppLock(): Boolean {
        val payload = getActivePayload() ?: return false
        return payload.allowAppLock
    }

    fun canUseBackgroundSync(): Boolean {
        val payload = getActivePayload() ?: return false
        return payload.allowBackgroundSync
    }

    /**
     * Checks clock tampering and expiry against payload.
     */
    private fun checkLicenseStatus(payload: LicensePayload): LicenseVerificationResult {
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()

        val lastSavedWall = prefs.getLong(KEY_LAST_WALL_TIME, 0L)
        val lastSavedElapsed = prefs.getLong(KEY_LAST_ELAPSED_TIME, 0L)
        val savedHmac = prefs.getString(KEY_CLOCK_HMAC, null)

        val isHmacValid = if (lastSavedWall > 0L && savedHmac != null) {
            savedHmac == calculateClockHmac(lastSavedWall, lastSavedElapsed)
        } else {
            false
        }

        // 1) Clock Rollback Check: Wall time reversed by > 5 minutes
        if (isHmacValid && lastSavedWall > 0L) {
            if (nowWall < lastSavedWall - FIVE_MINUTES_MS) {
                return LicenseVerificationResult(
                    status = LicenseStatus.CLOCK_TAMPERED,
                    payload = payload,
                    errorMessage = "تم اكتشاف تراجع في تاريخ أو وقت الجهاز. يرجى ضبط الساعة بشكل صحيح."
                )
            }

            // In same boot cycle (nowElapsed > lastSavedElapsed), check if elapsed time moved forward while wall moved backward
            if (nowElapsed > lastSavedElapsed) {
                val elapsedDelta = nowElapsed - lastSavedElapsed
                val wallDelta = nowWall - lastSavedWall
                if (elapsedDelta > 600_000L && wallDelta < -60_000L) {
                    return LicenseVerificationResult(
                        status = LicenseStatus.CLOCK_TAMPERED,
                        payload = payload,
                        errorMessage = "تم اكتشاف عدم تطابق في وقت تشغيل الجهاز مع الساعة."
                    )
                }
            }
        }

        // 2) Check if current wall time is before license issue date by > 5 minutes (or pre-2025 for V2)
        if (payload.iat > 0L) {
            val iatMs = payload.iat * 1000L
            if (nowWall < iatMs - FIVE_MINUTES_MS) {
                return LicenseVerificationResult(
                    status = LicenseStatus.CLOCK_TAMPERED,
                    payload = payload,
                    errorMessage = "وقت الجهاز الحالي يسبق تاريخ إصدار الترخيص. يرجى ضبط الساعة."
                )
            }
        } else if (payload.v >= 2) {
            val minV2EpochMs = V2LicenseCodec.EPOCH_2025_DAY * 86_400L * 1000L
            if (nowWall < minV2EpochMs) {
                return LicenseVerificationResult(
                    status = LicenseStatus.CLOCK_TAMPERED,
                    payload = payload,
                    errorMessage = "تاريخ الجهاز الحالي غير صحيح. يرجى ضبط الساعة."
                )
            }
        }

        // 3) Check expiration (if not permanent)
        if (!payload.isPermanent) {
            val expMs = payload.exp * 1000L
            if (nowWall > expMs) {
                return LicenseVerificationResult(
                    status = LicenseStatus.EXPIRED,
                    payload = payload,
                    errorMessage = "انتهت صلاحية كود الترخيص. يرجى تجديد الترخيص لمواصلة الاستخدام."
                )
            }
        }

        // Update trusted wall time if valid and advanced
        recordActivityTime()

        return LicenseVerificationResult(
            status = LicenseStatus.ACTIVE,
            payload = payload
        )
    }

    /**
     * Records current wall clock time securely, protected by HMAC-SHA256.
     * Only updates if current time is ahead of previously recorded time.
     */
    @Synchronized
    fun recordActivityTime() {
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        val lastSavedWall = prefs.getLong(KEY_LAST_WALL_TIME, 0L)

        if (nowWall >= lastSavedWall) {
            val hmac = calculateClockHmac(nowWall, nowElapsed)
            prefs.edit()
                .putLong(KEY_LAST_WALL_TIME, nowWall)
                .putLong(KEY_LAST_ELAPSED_TIME, nowElapsed)
                .putString(KEY_CLOCK_HMAC, hmac)
                .apply()
        }
    }

    private fun calculateClockHmac(wallTimeMs: Long, elapsedRealtimeMs: Long): String {
        return try {
            val secret = (installationId + "_ba_clock_integrity").toByteArray(StandardCharsets.UTF_8)
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret, "HmacSHA256"))
            val data = "$wallTimeMs:$elapsedRealtimeMs".toByteArray(StandardCharsets.UTF_8)
            Base64.getEncoder().encodeToString(mac.doFinal(data))
        } catch (_: Exception) {
            ""
        }
    }

    internal fun setClockForTesting(wallTimeMs: Long, elapsedRealtimeMs: Long) {
        val hmac = calculateClockHmac(wallTimeMs, elapsedRealtimeMs)
        prefs.edit()
            .putLong(KEY_LAST_WALL_TIME, wallTimeMs)
            .putLong(KEY_LAST_ELAPSED_TIME, elapsedRealtimeMs)
            .putString(KEY_CLOCK_HMAC, hmac)
            .apply()
    }

    internal fun resetForTesting() {
        prefs.edit().clear().apply()
    }
}
