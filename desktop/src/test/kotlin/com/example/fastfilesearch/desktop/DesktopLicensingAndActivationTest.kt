package com.example.fastfilesearch.desktop

import com.example.fastfilesearch.desktop.licensing.DesktopLicenseManager
import com.example.fastfilesearch.desktop.licensing.LicenseStatus
import com.example.fastfilesearch.desktop.licensing.ManufacturerDictionary
import com.example.fastfilesearch.desktop.licensing.V2LicenseCodec
import com.example.fastfilesearch.desktop.licensing.WindowsDeviceIdProvider
import com.example.fastfilesearch.desktop.licensing.WindowsDeviceInfo
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DesktopLicensingAndActivationTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    private fun sampleDeviceInfo(): WindowsDeviceInfo {
        val hash6 = byteArrayOf(0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte())
        return WindowsDeviceInfo(
            digestPrefix6 = hash6,
            clientDeviceId = "WIN-123456789abc",
            manufacturer = "Dell Inc.",
            model = "Dell Latitude 5430",
            osVersionNumber = 11,
            osDisplayName = "Windows 11",
            abiCode = V2LicenseCodec.ABI_X64,
            abiDisplay = "x86_64"
        )
    }

    @Test
    fun testManufacturerDictionaryIncludesMajorPcVendors() {
        assertTrue(ManufacturerDictionary.size() >= 60)
        assertEquals(19, ManufacturerDictionary.findId("DELL INC."))
        assertEquals(20, ManufacturerDictionary.findId("HP"))
        assertEquals(18, ManufacturerDictionary.findId("Lenovo"))
        assertEquals(21, ManufacturerDictionary.findId("ASUS"))
        assertEquals(22, ManufacturerDictionary.findId("Acer"))
    }

    @Test
    fun testWindowsVersionDetectionAndFingerprintHashing() {
        assertEquals(11, WindowsDeviceIdProvider.resolveWindowsMajorNumber("Microsoft Windows 11 Pro", "22631", "Windows 11", "10.0"))
        assertEquals(10, WindowsDeviceIdProvider.resolveWindowsMajorNumber("Microsoft Windows 10 Pro", "19045", "Windows 10", "10.0"))
        val p1 = WindowsDeviceIdProvider.formatWindowsFingerprintPrefix6("UUID-1|GUID-1")
        val p2 = WindowsDeviceIdProvider.formatWindowsFingerprintPrefix6("UUID-1|GUID-1")
        assertEquals(6, p1.size)
        assertArrayEquals(p1, p2)
    }

    @Test
    fun testFullWindowsR2GenerationAnd16CharLicenseRoundtrip() {
        val baseDir = tempFolder.newFolder("license_roundtrip")
        var currentNowMs = 1_795_651_200_000L // 2026-11-26 approx
        val deviceInfo = sampleDeviceInfo()
        val manager = DesktopLicenseManager(
            baseDir = baseDir,
            deviceInfoProvider = { deviceInfo },
            nowWallMillisProvider = { currentNowMs }
        )

        assertEquals(LicenseStatus.NOT_ACTIVATED, manager.checkCurrentLicense().status)

        val r2 = manager.buildR2ActivationRequest()
        assertTrue(r2.startsWith("R2"))
        assertTrue(r2.length in 22..32)

        val decodedR2 = V2LicenseCodec.decodeR2Request(r2)
        assertNotNull(decodedR2)
        decodedR2!!
        assertEquals(V2LicenseCodec.PLATFORM_WINDOWS, decodedR2.platform)
        assertEquals(V2LicenseCodec.FP_TYPE_WIN, decodedR2.fpType)
        assertEquals(V2LicenseCodec.ABI_X64, decodedR2.abiCode)
        assertEquals("Dell", decodedR2.manufacturer)
        assertEquals("LATITUDE 5", decodedR2.model)
        assertEquals("Windows 11", decodedR2.osVersion)
        assertEquals("x86_64", decodedR2.abi)
        assertEquals("WIN-123456789abc", decodedR2.clientDeviceId)
        assertEquals(manager.getInstallationId(), decodedR2.installationId)
        assertEquals(manager.getOrCreateInstallNonce(), decodedR2.installNonce)

        val expiryDays = V2LicenseCodec.calculateExpiryDaysSince2025(
            durationDays = 90L,
            nowSeconds = currentNowMs / 1000L
        )
        val shortCode = V2LicenseCodec.generateShortLicenseCode(
            platform = V2LicenseCodec.PLATFORM_WINDOWS,
            expiryDaysSince2025 = expiryDays,
            planTypeCode = V2LicenseCodec.PLAN_QUARTERLY,
            maxFolders = 3,
            allowExcel = true,
            allowAppLock = false,
            allowAccess = true,
            fpType = decodedR2.fpType,
            deviceHash6 = decodedR2.deviceHash6,
            installNonce = decodedR2.installNonce
        )
        assertEquals(19, shortCode.length)

        val activationResult = manager.activateLicense(shortCode)
        assertEquals(LicenseStatus.ACTIVE, activationResult.status)
        val payload = activationResult.payload!!
        assertEquals(3, payload.maxWorkspaces)
        assertTrue(payload.allowExcelExport)
        assertFalse(payload.allowAppLock)
        assertTrue(payload.allowAccess)
        assertEquals("quarterly", payload.planType)

        assertTrue(manager.canAddWorkspace(0))
        assertTrue(manager.canAddWorkspace(2))
        assertFalse(manager.canAddWorkspace(3))

        // Clock rollback tampering check
        currentNowMs -= 2 * 60 * 60 * 1000L
        assertEquals(LicenseStatus.CLOCK_TAMPERED, manager.checkCurrentLicense().status)

        // Restore clock and test expiry
        currentNowMs += 2 * 60 * 60 * 1000L
        assertEquals(LicenseStatus.ACTIVE, manager.checkCurrentLicense().status)

        currentNowMs = (payload.exp + 120L) * 1000L
        assertEquals(LicenseStatus.EXPIRED, manager.checkCurrentLicense().status)
    }

    @Test
    fun testRejectsAndroidOnlyWrongDeviceWrongNonceAndTamperedCode() {
        val baseDir = tempFolder.newFolder("license_rejections")
        val nowMs = 1_795_651_200_000L
        val deviceInfo = sampleDeviceInfo()
        val manager = DesktopLicenseManager(
            baseDir = baseDir,
            deviceInfoProvider = { deviceInfo },
            nowWallMillisProvider = { nowMs }
        )

        val nonce = manager.getOrCreateInstallNonce()
        val expiryDays = V2LicenseCodec.calculateExpiryDaysSince2025(30L, nowMs / 1000L)

        // 1. Android-only license must be rejected on Windows
        val androidOnly = V2LicenseCodec.generateShortLicenseCode(
            platform = V2LicenseCodec.PLATFORM_ANDROID,
            expiryDaysSince2025 = expiryDays,
            planTypeCode = V2LicenseCodec.PLAN_MONTHLY,
            maxFolders = 2,
            allowExcel = true,
            allowAppLock = true,
            allowAccess = true,
            fpType = V2LicenseCodec.FP_TYPE_WIN,
            deviceHash6 = deviceInfo.digestPrefix6,
            installNonce = nonce
        )
        assertEquals(LicenseStatus.INVALID, manager.activateLicense(androidOnly).status)

        // 2. Both-platforms license must be accepted on Windows
        val bothPlatforms = V2LicenseCodec.generateShortLicenseCode(
            platform = V2LicenseCodec.PLATFORM_BOTH,
            expiryDaysSince2025 = expiryDays,
            planTypeCode = V2LicenseCodec.PLAN_MONTHLY,
            maxFolders = 2,
            allowExcel = true,
            allowAppLock = true,
            allowAccess = true,
            fpType = V2LicenseCodec.FP_TYPE_WIN,
            deviceHash6 = deviceInfo.digestPrefix6,
            installNonce = nonce
        )
        assertEquals(LicenseStatus.ACTIVE, manager.activateLicense(bothPlatforms).status)

        // 3. Different device hash must be rejected
        val otherDeviceHash = byteArrayOf(1, 2, 3, 4, 5, 6)
        val wrongDevice = V2LicenseCodec.generateShortLicenseCode(
            platform = V2LicenseCodec.PLATFORM_WINDOWS,
            expiryDaysSince2025 = expiryDays,
            planTypeCode = V2LicenseCodec.PLAN_MONTHLY,
            maxFolders = 2,
            allowExcel = true,
            allowAppLock = true,
            allowAccess = true,
            fpType = V2LicenseCodec.FP_TYPE_WIN,
            deviceHash6 = otherDeviceHash,
            installNonce = nonce
        )
        assertEquals(LicenseStatus.INVALID, manager.activateLicense(wrongDevice).status)

        // 4. Wrong install_nonce must be rejected
        val wrongNonce = V2LicenseCodec.generateShortLicenseCode(
            platform = V2LicenseCodec.PLATFORM_WINDOWS,
            expiryDaysSince2025 = expiryDays,
            planTypeCode = V2LicenseCodec.PLAN_MONTHLY,
            maxFolders = 2,
            allowExcel = true,
            allowAppLock = true,
            allowAccess = true,
            fpType = V2LicenseCodec.FP_TYPE_WIN,
            deviceHash6 = deviceInfo.digestPrefix6,
            installNonce = (nonce + 1) and 0xFFFF
        )
        assertEquals(LicenseStatus.INVALID, manager.activateLicense(wrongNonce).status)

        // 5. Tampered character must be rejected
        val chars = bothPlatforms.toCharArray()
        chars[0] = if (chars[0] == '2') '3' else '2'
        assertEquals(LicenseStatus.INVALID, manager.activateLicense(String(chars)).status)
    }
}
