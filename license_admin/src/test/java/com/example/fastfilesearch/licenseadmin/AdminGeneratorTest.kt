package com.example.fastfilesearch.licenseadmin

import com.example.fastfilesearch.licenseadmin.engine.AdminLicenseGenerator
import com.example.fastfilesearch.licenseadmin.security.KeyStorageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class AdminGeneratorTest {

    @Test
    fun testGenerateLicenseWithEphemeralKeyPair() {
        // 1. Generate an ephemeral ECDSA P-256 key pair
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        val keyPair = kpg.generateKeyPair()

        val pubBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)

        // 2. Generate license code
        val instId = "BA-1234-5678-ABCD"
        val client = "شركة الفجر"
        val durationDays = 30L
        val maxWs = 5

        val result = AdminLicenseGenerator.generateLicense(
            privateKey = keyPair.private,
            installationId = instId,
            clientName = client,
            durationDays = durationDays,
            maxWorkspaces = maxWs,
            excelExport = true,
            appLock = true,
            bgSync = false,
            publicKeyBase64 = pubBase64
        )

        assertTrue("License generation should succeed", result.isSuccess)
        val code = result.getOrThrow()

        // 3. Verify format: BA1.<payload>.<sig>
        val parts = code.split(".")
        assertEquals("Code must have 3 parts separated by dots", 3, parts.size)
        assertEquals("Prefix must be BA1", "BA1", parts[0])
        assertTrue("Payload should not be empty", parts[1].isNotBlank())
        assertTrue("Signature should not be empty", parts[2].isNotBlank())

        // 4. Parse payload and verify all fields
        val payload = AdminLicenseGenerator.parsePayload(code)
        assertEquals("v must be 1", 1, payload.v)
        assertEquals("id must match uppercase cleaned ID", "BA-1234-5678-ABCD", payload.id)
        assertEquals("Client name must match", client, payload.n)
        assertEquals("Max workspaces must match", 5, payload.mw)
        assertEquals("ex must be 1", 1, payload.ex)
        assertEquals("lk must be 1", 1, payload.lk)
        assertEquals("bg must be 0", 0, payload.bg)
        assertTrue("iat must be positive timestamp", payload.iat > 0)
        assertTrue("exp must be after iat", payload.exp > payload.iat)
    }

    @Test
    fun testPermanentLicenseExpirationIsZero() {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        val keyPair = kpg.generateKeyPair()
        val pubBase64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)

        val result = AdminLicenseGenerator.generateLicense(
            privateKey = keyPair.private,
            installationId = "BA-PERM-TEST-0001",
            clientName = null,
            durationDays = 0L, // Permanent
            publicKeyBase64 = pubBase64
        )

        assertTrue(result.isSuccess)
        val payload = AdminLicenseGenerator.parsePayload(result.getOrThrow())
        assertEquals("Permanent license exp must be 0", 0L, payload.exp)
        assertEquals(null, payload.n)
    }

    @Test
    fun testWithOfficialPrivateKeyIfPresent() {
        // Look for license_tools/private_key.pem in possible relative paths
        val possiblePaths = listOf(
            File("license_tools/private_key.pem"),
            File("../license_tools/private_key.pem"),
            File("../../license_tools/private_key.pem")
        )
        val privFile = possiblePaths.firstOrNull { it.exists() }

        if (privFile != null) {
            val pemContent = privFile.readText(StandardCharsets.UTF_8)
            val privKey = KeyStorageManager.parsePrivateKeyFromPem(pemContent)

            // Test key match verification
            val matches = KeyStorageManager.verifyKeyMatch(privKey, KeyStorageManager.EMBEDDED_PUBLIC_KEY_BASE64)
            assertTrue("Official private key must match embedded public key", matches)

            // Generate license with embedded public key
            val result = AdminLicenseGenerator.generateLicense(
                privateKey = privKey,
                installationId = "BA-OFFICIAL-TEST-9999",
                clientName = "عميل رسمي",
                durationDays = 90L,
                publicKeyBase64 = KeyStorageManager.EMBEDDED_PUBLIC_KEY_BASE64
            )

            assertTrue("License generation with official key should succeed", result.isSuccess)
            val code = result.getOrThrow()
            assertTrue(code.startsWith("BA1."))

            val payload = AdminLicenseGenerator.parsePayload(code)
            assertEquals("BA-OFFICIAL-TEST-9999", payload.id)
            assertEquals("عميل رسمي", payload.n)
        }
    }

    @Test
    fun testCleanInstallationId() {
        val raw = "  ba - a1b2 - c3d4 - e5f6 \n\t "
        val cleaned = AdminLicenseGenerator.cleanInstallationId(raw)
        assertEquals("BA-A1B2-C3D4-E5F6", cleaned)
    }

    @Test
    fun testExtractInstallationIdFromFullWhatsAppMessage() {
        val whatsAppMessage = """
            السلام عليكم ورحمة الله،
            طلب تفعيل تطبيق «بحث آمن»:
            • معرّف التثبيت: BA-7F89-A1B2-C3D4
            • نوع وموديل الهاتف: Samsung SM-A205F
            • إصدار النظام: Android 11 (API 30)
            • إصدار التطبيق: 1.0 (1)
        """.trimIndent()

        val extracted = AdminLicenseGenerator.extractInstallationId(whatsAppMessage)
        assertEquals("BA-7F89-A1B2-C3D4", extracted)

        val cleaned = AdminLicenseGenerator.cleanInstallationId(whatsAppMessage)
        assertEquals("BA-7F89-A1B2-C3D4", cleaned)
    }

    @Test
    fun testExtractInstallationIdDirectInput() {
        val direct = "BA-1234-5678-ABCD"
        assertEquals("BA-1234-5678-ABCD", AdminLicenseGenerator.extractInstallationId(direct))

        val lowercaseDirect = "ba-1234-5678-abcd"
        assertEquals("BA-1234-5678-ABCD", AdminLicenseGenerator.extractInstallationId(lowercaseDirect))
    }
}
