package com.example.fastfilesearch.licenseadmin

import com.example.fastfilesearch.licenseadmin.data.PendingUploadEntity
import com.example.fastfilesearch.licenseadmin.engine.AdminLicenseGenerator
import com.example.fastfilesearch.licenseadmin.engine.ManufacturerDictionary
import com.example.fastfilesearch.licenseadmin.engine.V2LicenseCodec
import com.example.fastfilesearch.licenseadmin.sync.SupabaseUploadWorker
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class V2AdminCodecTest {

    @Test
    fun testManufacturerDictionarySyncCountAndEntries() {
        assertTrue(ManufacturerDictionary.size() >= 60)
        assertEquals(1, ManufacturerDictionary.findId("Samsung"))
        assertEquals(2, ManufacturerDictionary.findId("Xiaomi"))
        assertEquals(19, ManufacturerDictionary.findId("DELL INC."))
        assertEquals(20, ManufacturerDictionary.findId("HP"))
        assertEquals(0, ManufacturerDictionary.findId("UnknownBrandX"))
    }

    @Test
    fun testParseActivationRequestDecodesR2FromWhatsAppMessage() {
        val hash6 = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0x11, 0x22, 0x33)
        val nonce = 0x5678
        val r2 = V2LicenseCodec.encodeR2Request(
            platform = V2LicenseCodec.PLATFORM_ANDROID,
            fpType = V2LicenseCodec.FP_TYPE_AD,
            abiCode = V2LicenseCodec.ABI_ARM64,
            deviceHash6 = hash6,
            installNonce = nonce,
            osVersionInt = 35,
            appVersionCompact = 50,
            manufacturer = "Xiaomi",
            model = "2201117TY"
        )

        val whatsAppMessage = """
            طلب تفعيل تطبيق «بحث آمن»:
            $r2
        """.trimIndent()

        val parsed = AdminLicenseGenerator.parseActivationRequest(whatsAppMessage)
        assertTrue(parsed.isR2Request)
        assertEquals("AD-aabbcc112233", parsed.clientDeviceId)
        assertEquals(V2LicenseCodec.FP_TYPE_AD, parsed.fpType)
        assertArrayEquals(hash6, parsed.deviceHash6)
        assertEquals(nonce, parsed.installNonce)
        assertEquals(V2LicenseCodec.PLATFORM_ANDROID, parsed.platformCode)
        assertEquals(V2LicenseCodec.deriveInstallationId(V2LicenseCodec.FP_TYPE_AD, hash6, nonce), parsed.installationId)

        val dev = parsed.deviceInfo
        assertNotNull(dev)
        dev!!
        assertEquals("Xiaomi", dev.manufacturer)
        assertEquals("2201117TY", dev.model)
        assertEquals("Android 15 (API 35)", dev.osVersion)
        assertEquals("arm64-v8a", dev.abi)
        assertEquals("5.0.0", dev.appVersion)
        assertEquals("android", dev.platform)
        assertEquals("AD-aabbcc112233", dev.clientDeviceId)
    }

    @Test
    fun testGenerateV2ShortCodeFromR2AndSerializeToSupabase() {
        val hash6 = byteArrayOf(0x10, 0x20, 0x30, 0x40, 0x50, 0x60)
        val nonce = 0x1A2B
        val r2 = V2LicenseCodec.encodeR2Request(
            platform = V2LicenseCodec.PLATFORM_ANDROID,
            fpType = V2LicenseCodec.FP_TYPE_WV,
            abiCode = V2LicenseCodec.ABI_ARM64,
            deviceHash6 = hash6,
            installNonce = nonce,
            osVersionInt = 33,
            appVersionCompact = 50,
            manufacturer = "Huawei",
            model = "ANA-NX9"
        )

        val parsed = AdminLicenseGenerator.parseActivationRequest(r2)
        val nowSeconds = 1795651200L
        val genResult = AdminLicenseGenerator.generateV2License(
            parsedRequest = parsed,
            durationDays = 90L,
            maxWorkspaces = 4,
            excelExport = true,
            appLock = false,
            allowAccess = true,
            planType = "quarterly",
            nowSeconds = nowSeconds
        )

        assertTrue(genResult.isSuccess)
        val shortCode = genResult.getOrThrow()
        assertEquals(19, shortCode.length)
        assertEquals(16, V2LicenseCodec.cleanV2LicenseInput(shortCode).length)
        assertTrue(V2LicenseCodec.looksLikeV2LicenseCode(shortCode))

        val verifiedDecoded = V2LicenseCodec.decodeAndVerifyShortLicenseCode(
            rawCode = shortCode,
            expectedFpType = V2LicenseCodec.FP_TYPE_WV,
            expectedDeviceHash6 = hash6,
            expectedInstallNonce = nonce
        )
        assertNotNull(verifiedDecoded)
        verifiedDecoded!!
        assertEquals("quarterly", verifiedDecoded.planTypeName)
        assertEquals(4, verifiedDecoded.maxFolders)
        assertTrue(verifiedDecoded.allowExcel)
        assertFalse(verifiedDecoded.allowAppLock)
        assertTrue(verifiedDecoded.allowAccess)

        val payload = AdminLicenseGenerator.parsePayload(
            licenseCode = shortCode,
            installationId = parsed.installationId,
            clientName = "شركة النور للتجارة",
            issuedAt = nowSeconds
        )
        assertEquals(2, payload.v)
        assertEquals("شركة النور للتجارة", payload.n)
        assertEquals(parsed.installationId, payload.id)
        assertEquals(4, payload.mw)
        assertEquals(1, payload.ex)
        assertEquals(0, payload.lk)
        assertEquals(1, payload.bg)

        val entity = PendingUploadEntity(
            issuedAt = Instant.ofEpochSecond(payload.iat).toString(),
            issuerId = "issuer-01",
            issuerDevice = "Google Pixel",
            customerName = payload.n,
            installationId = payload.id,
            clientDeviceId = parsed.clientDeviceId,
            clientManufacturer = parsed.deviceInfo?.manufacturer,
            clientModel = parsed.deviceInfo?.model,
            clientOs = parsed.deviceInfo?.osVersion,
            clientAbi = parsed.deviceInfo?.abi,
            clientAppVersion = parsed.deviceInfo?.appVersion,
            planType = "quarterly",
            durationDays = 90,
            expiresAt = Instant.ofEpochSecond(payload.exp).toString(),
            maxFolders = payload.mw,
            allowExcel = payload.ex == 1,
            allowAppLock = payload.lk == 1,
            licenseCode = shortCode
        )

        val json = SupabaseUploadWorker.toSupabaseJson(entity)
        assertTrue(json.contains("\"customer_name\":\"شركة النور للتجارة\""))
        assertTrue(json.contains("\"client_device_id\":\"WV-102030405060\""))
        assertTrue(json.contains("\"client_manufacturer\":\"Huawei\""))
        assertTrue(json.contains("\"client_model\":\"ANA-NX9\""))
        assertTrue(json.contains("\"plan_type\":\"quarterly\""))
        assertTrue(json.contains("\"license_code\":\"$shortCode\""))
    }

    @Test
    fun testV2AllPlanTypesAndLifetimeEncoding() {
        val hash6 = byteArrayOf(1, 2, 3, 4, 5, 6)
        val plans = listOf(
            "trial" to 7L,
            "monthly" to 30L,
            "quarterly" to 90L,
            "yearly" to 365L,
            "lifetime" to 0L,
            "custom" to 45L
        )
        for ((planName, days) in plans) {
            val code = AdminLicenseGenerator.generateV2License(
                fpType = V2LicenseCodec.FP_TYPE_WV,
                deviceHash6 = hash6,
                installNonce = 999,
                durationDays = days,
                planType = planName,
                nowSeconds = 1795651200L
            ).getOrThrow()

            val decoded = V2LicenseCodec.decodeAndVerifyShortLicenseCode(
                rawCode = code,
                expectedFpType = V2LicenseCodec.FP_TYPE_WV,
                expectedDeviceHash6 = hash6,
                expectedInstallNonce = 999
            )!!
            assertEquals(planName, decoded.planTypeName)
            if (days == 0L) {
                assertEquals(0, decoded.expiryDaysSince2025)
                assertEquals(0L, decoded.expEpochSeconds)
            } else {
                assertTrue(decoded.expiryDaysSince2025 > 0)
                assertTrue(decoded.expEpochSeconds > 1795651200L)
            }
        }
    }

    @Test
    fun testRejectMissingR2NonceAndVerifyLegacyStillWorks() {
        val plain = AdminLicenseGenerator.parseActivationRequest("BA-1234-5678-ABCD")
        assertFalse(plain.isR2Request)
        assertNull(plain.installNonce)
        assertTrue(AdminLicenseGenerator.generateV2License(plain).isFailure)
    }
}
