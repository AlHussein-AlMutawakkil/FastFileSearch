package com.example.fastfilesearch.desktop.licensing

import com.example.fastfilesearch.desktop.storage.DesktopAppStorage
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Manages Windows local license storage, install_nonce, R2 request generation,
 * V2 activation verification, and anti-clock-tamper enforcement under %LOCALAPPDATA%\SafeSearch\.
 */
class DesktopLicenseManager(
    private val baseDir: File = DesktopAppStorage.defaultAppDataDir(),
    private val deviceInfoProvider: () -> WindowsDeviceInfo = { WindowsDeviceIdProvider.getDeviceInfo() },
    private val nowWallMillisProvider: () -> Long = { System.currentTimeMillis() }
) {

    companion object {
        private const val KEY_INSTALL_NONCE = "install_nonce_v2"
        private const val KEY_LICENSE_CODE = "license_code"
        private const val KEY_LAST_WALL_MS = "last_wall_ms"
        private const val KEY_LAST_UPTIME_MS = "last_uptime_ms"
        private const val KEY_CLOCK_HMAC = "clock_hmac"

        private const val FIVE_MINUTES_MS = 5 * 60 * 1000L
        private val random = SecureRandom()
    }

    private val licenseFile: File = DesktopAppStorage.getLicenseFile(baseDir)

    val deviceInfo: WindowsDeviceInfo by lazy { deviceInfoProvider() }

    @Synchronized
    fun getOrCreateInstallNonce(): Int {
        val props = DesktopAppStorage.loadProperties(licenseFile)
        val raw = props.getProperty(KEY_INSTALL_NONCE)?.toIntOrNull()
        if (raw != null && raw in 0..0xFFFF) {
            return raw
        }
        val generated = random.nextInt(0x10000)
        props.setProperty(KEY_INSTALL_NONCE, generated.toString())
        DesktopAppStorage.saveProperties(licenseFile, props)
        return generated
    }

    fun getInstallationId(): String {
        val nonce = getOrCreateInstallNonce()
        return V2LicenseCodec.deriveInstallationId(
            fpType = V2LicenseCodec.FP_TYPE_WIN,
            deviceHash6 = deviceInfo.digestPrefix6,
            installNonce = nonce
        )
    }

    fun buildR2ActivationRequest(): String {
        val info = deviceInfo
        val nonce = getOrCreateInstallNonce()
        val compactVer = V2LicenseCodec.encodeAppVersionCompact(
            appVersion = WindowsDeviceIdProvider.DESKTOP_APP_VERSION,
            versionCodeFallback = 500
        )
        return V2LicenseCodec.encodeR2Request(
            platform = V2LicenseCodec.PLATFORM_WINDOWS,
            fpType = V2LicenseCodec.FP_TYPE_WIN,
            abiCode = info.abiCode,
            deviceHash6 = info.digestPrefix6,
            installNonce = nonce,
            osVersionInt = info.osVersionNumber,
            appVersionCompact = compactVer,
            manufacturer = info.manufacturer,
            model = info.model
        )
    }

    @Synchronized
    fun activateLicense(rawCode: String): LicenseVerificationResult {
        val nonce = getOrCreateInstallNonce()
        val verification = DesktopLicenseVerifier.verify(
            rawCode = rawCode,
            deviceFpType = V2LicenseCodec.FP_TYPE_WIN,
            deviceHash6 = deviceInfo.digestPrefix6,
            installNonce = nonce
        )

        if (verification.status == LicenseStatus.ACTIVE && verification.payload != null) {
            val cleaned = DesktopLicenseVerifier.cleanLicenseCode(rawCode)
            val props = DesktopAppStorage.loadProperties(licenseFile)
            props.setProperty(KEY_LICENSE_CODE, cleaned)
            DesktopAppStorage.saveProperties(licenseFile, props)
            recordActivityTime()
            return checkLicenseStatus(verification.payload)
        }

        return verification
    }

    @Synchronized
    fun getSavedLicenseCode(): String? {
        val props = DesktopAppStorage.loadProperties(licenseFile)
        return props.getProperty(KEY_LICENSE_CODE)?.trim()?.takeIf { it.isNotBlank() }
    }

    @Synchronized
    fun clearLicense() {
        val props = DesktopAppStorage.loadProperties(licenseFile)
        props.remove(KEY_LICENSE_CODE)
        DesktopAppStorage.saveProperties(licenseFile, props)
    }

    @Synchronized
    fun checkCurrentLicense(): LicenseVerificationResult {
        val savedCode = getSavedLicenseCode() ?: return LicenseVerificationResult(
            status = LicenseStatus.NOT_ACTIVATED,
            errorMessage = "لم يتم تفعيل التطبيق بعد"
        )

        val nonce = getOrCreateInstallNonce()
        val verification = DesktopLicenseVerifier.verify(
            rawCode = savedCode,
            deviceFpType = V2LicenseCodec.FP_TYPE_WIN,
            deviceHash6 = deviceInfo.digestPrefix6,
            installNonce = nonce
        )

        if (verification.status != LicenseStatus.ACTIVE || verification.payload == null) {
            return verification
        }

        return checkLicenseStatus(verification.payload)
    }

    fun getLicenseStatus(): LicenseStatus = checkCurrentLicense().status

    fun getActivePayload(): LicensePayload? {
        val res = checkCurrentLicense()
        return if (res.status == LicenseStatus.ACTIVE) res.payload else null
    }

    fun isLicenseActive(): Boolean = getLicenseStatus() == LicenseStatus.ACTIVE

    fun canAddWorkspace(currentCount: Int): Boolean {
        val payload = getActivePayload() ?: return false
        if (payload.maxWorkspaces <= 0) return true
        return currentCount < payload.maxWorkspaces
    }

    fun canExportExcel(): Boolean = getActivePayload()?.allowExcelExport == true

    fun canUseAppLock(): Boolean = getActivePayload()?.allowAppLock == true

    fun canUseBackgroundSync(): Boolean = getActivePayload()?.allowAccess == true

    private fun checkLicenseStatus(payload: LicensePayload): LicenseVerificationResult {
        val nowWall = nowWallMillisProvider()
        val nowUptime = try {
            ManagementFactory.getRuntimeMXBean().uptime
        } catch (_: Throwable) {
            0L
        }

        val props = DesktopAppStorage.loadProperties(licenseFile)
        val lastWall = props.getProperty(KEY_LAST_WALL_MS)?.toLongOrNull() ?: 0L
        val lastUptime = props.getProperty(KEY_LAST_UPTIME_MS)?.toLongOrNull() ?: 0L
        val savedHmac = props.getProperty(KEY_CLOCK_HMAC)

        val isHmacValid = if (lastWall > 0L && savedHmac != null) {
            savedHmac == calculateClockHmac(lastWall, lastUptime)
        } else {
            false
        }

        if (isHmacValid && lastWall > 0L) {
            if (nowWall < lastWall - FIVE_MINUTES_MS) {
                return LicenseVerificationResult(
                    status = LicenseStatus.CLOCK_TAMPERED,
                    payload = payload,
                    errorMessage = "تم اكتشاف تراجع في تاريخ أو وقت النظام. يرجى ضبط الساعة بشكل صحيح."
                )
            }
            if (nowUptime > lastUptime) {
                val elapsedDelta = nowUptime - lastUptime
                val wallDelta = nowWall - lastWall
                if (elapsedDelta > 600_000L && wallDelta < -60_000L) {
                    return LicenseVerificationResult(
                        status = LicenseStatus.CLOCK_TAMPERED,
                        payload = payload,
                        errorMessage = "تم اكتشاف عدم تطابق في وقت تشغيل الجهاز مع الساعة."
                    )
                }
            }
        }

        if (payload.iat > 0L) {
            val iatMs = payload.iat * 1000L
            if (nowWall < iatMs - FIVE_MINUTES_MS) {
                return LicenseVerificationResult(
                    status = LicenseStatus.CLOCK_TAMPERED,
                    payload = payload,
                    errorMessage = "وقت الجهاز الحالي يسبق تاريخ إصدار الترخيص. يرجى ضبط الساعة."
                )
            }
        }

        val minV2EpochMs = V2LicenseCodec.EPOCH_2025_DAY * 86_400L * 1000L
        if (nowWall < minV2EpochMs) {
            return LicenseVerificationResult(
                status = LicenseStatus.CLOCK_TAMPERED,
                payload = payload,
                errorMessage = "تاريخ الجهاز غير صحيح. يرجى ضبط ساعة النظام."
            )
        }

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

        recordActivityTime()
        return LicenseVerificationResult(status = LicenseStatus.ACTIVE, payload = payload)
    }

    private fun recordActivityTime() {
        val nowWall = nowWallMillisProvider()
        val nowUptime = try {
            ManagementFactory.getRuntimeMXBean().uptime
        } catch (_: Throwable) {
            0L
        }

        val props = DesktopAppStorage.loadProperties(licenseFile)
        val prevWall = props.getProperty(KEY_LAST_WALL_MS)?.toLongOrNull() ?: 0L
        if (nowWall >= prevWall) {
            val hmac = calculateClockHmac(nowWall, nowUptime)
            props.setProperty(KEY_LAST_WALL_MS, nowWall.toString())
            props.setProperty(KEY_LAST_UPTIME_MS, nowUptime.toString())
            props.setProperty(KEY_CLOCK_HMAC, hmac)
            DesktopAppStorage.saveProperties(licenseFile, props)
        }
    }

    private fun calculateClockHmac(wallMs: Long, uptimeMs: Long): String {
        val instId = getInstallationId()
        val secret = ("$instId|${deviceInfo.clientDeviceId}_win_clock").toByteArray(StandardCharsets.UTF_8)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        val data = "$wallMs:$uptimeMs".toByteArray(StandardCharsets.UTF_8)
        return Base64.getEncoder().encodeToString(mac.doFinal(data))
    }
}
