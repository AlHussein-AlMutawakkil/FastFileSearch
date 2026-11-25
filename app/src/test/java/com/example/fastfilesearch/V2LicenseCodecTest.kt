package com.example.fastfilesearch

import com.example.fastfilesearch.licensing.InstallationIdManager
import com.example.fastfilesearch.licensing.LicenseStatus
import com.example.fastfilesearch.licensing.LicenseVerifier
import com.example.fastfilesearch.licensing.ManufacturerDictionary
import com.example.fastfilesearch.licensing.V2LicenseCodec
import com.example.fastfilesearch.util.DeviceInfoHelper
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V2LicenseCodecTest {

    @Test
    fun testManufacturerDictionaryHas60PlusEntriesAndMapsAliases() {
        assertTrue(ManufacturerDictionary.size() >= 60)
        assertEquals(1, ManufacturerDictionary.findId("Samsung"))
        assertEquals(1, ManufacturerDictionary.findId("  samsung electronics "))
        assertEquals("Samsung", ManufacturerDictionary.getName(1))
        assertEquals(19, ManufacturerDictionary.findId("Dell Inc."))
        assertEquals("Dell", ManufacturerDictionary.getName(19))
        assertEquals(20, ManufacturerDictionary.findId("Hewlett-Packard"))
        assertEquals(21, ManufacturerDictionary.findId("ASUSTeK COMPUTER INC."))
        assertEquals(0, ManufacturerDictionary.findId("SomeRareBrand"))
        assertEquals("", ManufacturerDictionary.getName(0))
    }

    @Test
    fun testR2EncodingLengthAndFullRoundtrip() {
        val hash6 = byteArrayOf(0x1A, 0x2B, 0x3C, 0x4D, 0x5E, 0x6F.toByte())
        val nonce = 0x9182

        val r2 = DeviceInfoHelper.encodeR2ActivationRequest(
            platform = V2LicenseCodec.PLATFORM_ANDROID,
            fpType = V2LicenseCodec.FP_TYPE_WV,
            abiCode = V2LicenseCodec.ABI_ARM64,
            deviceHash6 = hash6,
            installNonce = nonce,
            osVersionInt = 34,
            appVersionCompact = V2LicenseCodec.encodeAppVersionCompact("5.0.0", 500),
            manufacturer = "Samsung",
            model = "Samsung SM-S918B"
        )

        assertTrue(r2.startsWith("R2"))
        // Base62 only after prefix
        assertTrue(r2.substring(2).all { it in '0'..'9' || it in 'A'..'Z' || it in 'a'..'z' })
        // Realistic phone models land in ~24-28 chars
        assertTrue("Expected compact R2 length in 23..29, was ${r2.length} ($r2)", r2.length in 23..29)

        val decoded = V2LicenseCodec.decodeR2Request(r2)
        assertNotNull(decoded)
        decoded!!
        assertEquals(V2LicenseCodec.R2_VERSION, decoded.version)
        assertEquals(V2LicenseCodec.PLATFORM_ANDROID, decoded.platform)
        assertEquals(V2LicenseCodec.FP_TYPE_WV, decoded.fpType)
        assertEquals(V2LicenseCodec.ABI_ARM64, decoded.abiCode)
        assertArrayEquals(hash6, decoded.deviceHash6)
        assertEquals(nonce, decoded.installNonce)
        assertEquals(34, decoded.osCode)
        assertEquals("Samsung", decoded.manufacturer)
        assertEquals("SM-S918B", decoded.model) // Brand prefix stripped cleanly
        assertEquals("Android 14 (API 34)", decoded.osVersion)
        assertEquals("arm64-v8a", decoded.abi)
        assertEquals("5.0.0", decoded.appVersion)
        assertEquals("WV-1a2b3c4d5e6f", decoded.clientDeviceId)
        assertTrue(InstallationIdManager.isValidFormat(decoded.installationId))
    }

    @Test
    fun testR2UnlistedBrandFallsBackToBrandPipeModel() {
        val hash6 = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06)
        val r2 = DeviceInfoHelper.encodeR2ActivationRequest(
            platform = V2LicenseCodec.PLATFORM_WINDOWS,
            fpType = V2LicenseCodec.FP_TYPE_WIN,
            abiCode = V2LicenseCodec.ABI_X64,
            deviceHash6 = hash6,
            installNonce = 42,
            osVersionInt = 11,
            appVersionCompact = 51,
            manufacturer = "Kuu",
            model = "G5"
        )

        assertTrue(r2.length in 20..28)
        val decoded = V2LicenseCodec.decodeR2Request(r2)!!
        assertEquals(0, decoded.manufacturerId)
        assertEquals("KUU", decoded.manufacturer)
        assertEquals("G5", decoded.model)
        assertEquals("WIN-010203040506", decoded.clientDeviceId)
        assertEquals("Windows 11", decoded.osVersion)
        assertEquals("x86_64", decoded.abi)
        assertEquals("5.1.0", decoded.appVersion)
    }

    @Test
    fun testV2ShortCodeIs16CharsAndVerifiesCleanlyWithNormalizedInput() {
        val hash6 = byteArrayOf(0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte())
        val nonce = 0x4321
        val expiryDays = V2LicenseCodec.calculateExpiryDaysSince2025(3650L, nowSeconds = 1795651200L) // ~2036

        val code = V2LicenseCodec.generateShortLicenseCode(
            platform = V2LicenseCodec.PLATFORM_ANDROID,
            expiryDaysSince2025 = expiryDays,
            planTypeCode = V2LicenseCodec.PLAN_YEARLY,
            maxFolders = 12,
            allowExcel = true,
            allowAppLock = false,
            allowAccess = true,
            fpType = V2LicenseCodec.FP_TYPE_AD,
            deviceHash6 = hash6,
            installNonce = nonce
        )

        // XXXX-XXXX-XXXX-XXXX -> 19 formatted chars, 16 Base32 chars
        assertEquals(19, code.length)
        assertTrue(code.matches(Regex("^[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}$")))
        assertEquals(16, V2LicenseCodec.cleanV2LicenseInput(code).length)

        // Verify with messy lowercase / spaces / no dashes / RTL mark
        val messy = " \u200F ${code.lowercase().replace("-", "  ")} \n "
        val result = LicenseVerifier.verify(
            rawCode = messy,
            expectedInstallationId = "BA-ANY0-0000-0000",
            deviceFpType = V2LicenseCodec.FP_TYPE_AD,
            deviceHash6 = hash6,
            installNonce = nonce
        )

        assertEquals(LicenseStatus.ACTIVE, result.status)
        val payload = result.payload!!
        assertEquals(2, payload.v)
        assertNull(payload.n) // Licensee name never stored in V2 short code
        assertEquals(12, payload.maxWorkspaces)
        assertTrue(payload.allowExcelExport)
        assertFalse(payload.allowAppLock)
        assertTrue(payload.allowBackgroundSync)
        assertFalse(payload.isPermanent)
        assertEquals(V2LicenseCodec.daysSince2025ToExpSeconds(expiryDays), payload.exp)
    }

    @Test
    fun testV2RejectsTamperingWrongDeviceWrongNonceWrongPlatformAndDisabledAccess() {
        val hash6 = byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55, 0x66)
        val nonce = 12345

        val code = V2LicenseCodec.generateShortLicenseCode(
            platform = V2LicenseCodec.PLATFORM_ANDROID,
            expiryDaysSince2025 = 0, // permanent
            planTypeCode = V2LicenseCodec.PLAN_LIFETIME,
            maxFolders = 0, // unlimited
            allowExcel = true,
            allowAppLock = true,
            allowAccess = true,
            fpType = V2LicenseCodec.FP_TYPE_WV,
            deviceHash6 = hash6,
            installNonce = nonce
        )

        // 1. Valid verification
        val ok = LicenseVerifier.verifyV2(code, V2LicenseCodec.FP_TYPE_WV, hash6, nonce)
        assertEquals(LicenseStatus.ACTIVE, ok.status)
        assertTrue(ok.payload?.isPermanent == true)
        assertEquals(0, ok.payload?.maxWorkspaces)

        // 2. Single character tamper anywhere in the 16 chars
        val raw16 = V2LicenseCodec.cleanV2LicenseInput(code)
        val replacementChar = if (raw16[5] == '2') '3' else '2'
        val tampered = raw16.substring(0, 5) + replacementChar + raw16.substring(6)
        assertEquals(
            LicenseStatus.INVALID,
            LicenseVerifier.verifyV2(tampered, V2LicenseCodec.FP_TYPE_WV, hash6, nonce).status
        )

        // 3. Wrong device hash
        val wrongHash = byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55, 0x67)
        assertEquals(
            LicenseStatus.INVALID,
            LicenseVerifier.verifyV2(code, V2LicenseCodec.FP_TYPE_WV, wrongHash, nonce).status
        )

        // 4. Wrong fingerprint type (WV vs AD)
        assertEquals(
            LicenseStatus.INVALID,
            LicenseVerifier.verifyV2(code, V2LicenseCodec.FP_TYPE_AD, hash6, nonce).status
        )

        // 5. Wrong install_nonce (app uninstalled and reinstalled)
        assertEquals(
            LicenseStatus.INVALID,
            LicenseVerifier.verifyV2(code, V2LicenseCodec.FP_TYPE_WV, hash6, nonce + 1).status
        )

        // 6. Windows-only license rejected by Android verifier
        val windowsOnlyCode = V2LicenseCodec.generateShortLicenseCode(
            platform = V2LicenseCodec.PLATFORM_WINDOWS,
            expiryDaysSince2025 = 0,
            planTypeCode = V2LicenseCodec.PLAN_LIFETIME,
            maxFolders = 0,
            allowExcel = true,
            allowAppLock = true,
            allowAccess = true,
            fpType = V2LicenseCodec.FP_TYPE_WV,
            deviceHash6 = hash6,
            installNonce = nonce
        )
        assertEquals(
            LicenseStatus.INVALID,
            LicenseVerifier.verifyV2(windowsOnlyCode, V2LicenseCodec.FP_TYPE_WV, hash6, nonce).status
        )

        // 7. Disabled allow_access bit rejected
        val noAccessCode = V2LicenseCodec.generateShortLicenseCode(
            platform = V2LicenseCodec.PLATFORM_BOTH,
            expiryDaysSince2025 = 0,
            planTypeCode = V2LicenseCodec.PLAN_CUSTOM,
            maxFolders = 1,
            allowExcel = true,
            allowAppLock = true,
            allowAccess = false,
            fpType = V2LicenseCodec.FP_TYPE_WV,
            deviceHash6 = hash6,
            installNonce = nonce
        )
        assertEquals(
            LicenseStatus.INVALID,
            LicenseVerifier.verifyV2(noAccessCode, V2LicenseCodec.FP_TYPE_WV, hash6, nonce).status
        )
    }

    @Test
    fun testDeterministicCrossModuleHmacVector() {
        val hash6 = byteArrayOf(0x10, 0x20, 0x30, 0x40, 0x50, 0x60)
        val nonce = 0x1A2B
        val code1 = V2LicenseCodec.generateShortLicenseCode(
            platform = V2LicenseCodec.PLATFORM_ANDROID,
            expiryDaysSince2025 = 730,
            planTypeCode = V2LicenseCodec.PLAN_YEARLY,
            maxFolders = 5,
            allowExcel = true,
            allowAppLock = true,
            allowAccess = true,
            fpType = V2LicenseCodec.FP_TYPE_WV,
            deviceHash6 = hash6,
            installNonce = nonce
        )
        val code2 = V2LicenseCodec.generateShortLicenseCode(
            platform = V2LicenseCodec.PLATFORM_ANDROID,
            expiryDaysSince2025 = 730,
            planTypeCode = V2LicenseCodec.PLAN_YEARLY,
            maxFolders = 5,
            allowExcel = true,
            allowAppLock = true,
            allowAccess = true,
            fpType = V2LicenseCodec.FP_TYPE_WV,
            deviceHash6 = hash6,
            installNonce = nonce
        )
        assertEquals(code1, code2)
        val verified = LicenseVerifier.verifyV2(code1, V2LicenseCodec.FP_TYPE_WV, hash6, nonce)
        assertEquals(LicenseStatus.ACTIVE, verified.status)
    }
}
