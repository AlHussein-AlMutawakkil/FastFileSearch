package com.example.fastfilesearch

import com.example.fastfilesearch.licensing.InstallationIdManager
import com.example.fastfilesearch.licensing.LicensePayload
import com.example.fastfilesearch.licensing.LicenseStatus
import com.example.fastfilesearch.licensing.LicenseVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64

class LicenseSystemTest {

    private fun getPrivateKeyFile(): File {
        val f1 = File("license_tools/private_key.pem")
        if (f1.exists()) return f1
        val f2 = File("../license_tools/private_key.pem")
        if (f2.exists()) return f2
        throw IllegalStateException("private_key.pem not found in license_tools/ or ../license_tools/")
    }

    private fun generateTestCode(
        privateKey: PrivateKey,
        installationId: String,
        clientName: String? = "شركة الاختبار",
        iat: Long = Instant.now().epochSecond,
        exp: Long = 0L,
        maxWorkspaces: Int = 0,
        allowExcel: Int = 1,
        allowLock: Int = 1,
        allowSync: Int = 1
    ): String {
        val nameJson = if (clientName != null) "\"n\":\"$clientName\"," else ""
        val jsonPayload = """{"v":1,"id":"$installationId",$nameJson"iat":$iat,"exp":$exp,"mw":$maxWorkspaces,"ex":$allowExcel,"lk":$allowLock,"bg":$allowSync}"""
        val payloadBytes = jsonPayload.toByteArray(StandardCharsets.UTF_8)

        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(privateKey)
        signer.update(payloadBytes)
        val sigBytes = signer.sign()

        val urlEncoder = Base64.getUrlEncoder().withoutPadding()
        val payloadB64 = urlEncoder.encodeToString(payloadBytes)
        val sigB64 = urlEncoder.encodeToString(sigBytes)

        return "BA1.$payloadB64.$sigB64"
    }

    @Test
    fun testInstallationIdValidation() {
        assertTrue(InstallationIdManager.isValidFormat("BA-ABCD-2345-WXYZ"))
        assertTrue(InstallationIdManager.isValidFormat("BA-2222-3333-4444"))
        assertFalse("Must start with BA-", InstallationIdManager.isValidFormat("XX-ABCD-2345-WXYZ"))
        assertFalse("Cannot contain 0 (ambiguous)", InstallationIdManager.isValidFormat("BA-0BCD-2345-WXYZ"))
        assertFalse("Cannot contain O (ambiguous)", InstallationIdManager.isValidFormat("BA-OBCD-2345-WXYZ"))
        assertFalse("Cannot contain 1 (ambiguous)", InstallationIdManager.isValidFormat("BA-1BCD-2345-WXYZ"))
        assertFalse("Cannot contain I (ambiguous)", InstallationIdManager.isValidFormat("BA-IBCD-2345-WXYZ"))
        assertFalse("Must have 4 parts", InstallationIdManager.isValidFormat("BA-ABCD-2345"))
    }

    @Test
    fun testRealEcdsaVerificationSuccess() {
        // Generate ephemeral key pair
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
        val kp = kpg.generateKeyPair()

        val pubBase64 = Base64.getEncoder().encodeToString(kp.public.encoded)
        val testId = "BA-TEST-2345-6789"

        val code = generateTestCode(
            privateKey = kp.private,
            installationId = testId,
            clientName = "أحمد محمد",
            exp = 0L,
            maxWorkspaces = 3
        )

        val res = LicenseVerifier.verify(code, testId, pubBase64)
        assertEquals(LicenseStatus.ACTIVE, res.status)
        assertNotNull(res.payload)
        assertEquals(testId, res.payload?.id)
        assertEquals("أحمد محمد", res.payload?.n)
        assertEquals(3, res.payload?.maxWorkspaces)
        assertTrue(res.payload?.isPermanent == true)
    }

    @Test
    fun testRejectTamperedPayloadOrSignature() {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
        val kp = kpg.generateKeyPair()
        val pubBase64 = Base64.getEncoder().encodeToString(kp.public.encoded)
        val testId = "BA-ABCD-2345-EFGH"

        val validCode = generateTestCode(kp.private, testId)
        val parts = validCode.split(".")

        // 1. Tamper payload by modifying one character
        val tamperedPayload = if (parts[1].endsWith("A")) parts[1].dropLast(1) + "B" else parts[1].dropLast(1) + "A"
        val codeWithTamperedPayload = "${parts[0]}.$tamperedPayload.${parts[2]}"
        val res1 = LicenseVerifier.verify(codeWithTamperedPayload, testId, pubBase64)
        assertEquals(LicenseStatus.INVALID, res1.status)

        // 2. Tamper signature
        val tamperedSig = if (parts[2].endsWith("X")) parts[2].dropLast(1) + "Y" else parts[2].dropLast(1) + "X"
        val codeWithTamperedSig = "${parts[0]}.${parts[1]}.$tamperedSig"
        val res2 = LicenseVerifier.verify(codeWithTamperedSig, testId, pubBase64)
        assertEquals(LicenseStatus.INVALID, res2.status)
    }

    @Test
    fun testRejectDifferentInstallationId() {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
        val kp = kpg.generateKeyPair()
        val pubBase64 = Base64.getEncoder().encodeToString(kp.public.encoded)

        val codeForDeviceA = generateTestCode(kp.private, "BA-AAAA-2222-3333")

        // Verifying for Device B
        val res = LicenseVerifier.verify(codeForDeviceA, "BA-BBBB-4444-5555", pubBase64)
        assertEquals(LicenseStatus.INVALID, res.status)
        assertTrue(res.errorMessage?.contains("غير مخصص") == true)
    }

    @Test
    fun testPermanentAndExpiredPayloadProperties() {
        val now = Instant.now().epochSecond

        // Permanent
        val permPayload = LicensePayload(id = "BA-TEST-2345-6789", iat = now, exp = 0L)
        assertTrue(permPayload.isPermanent)

        // Expired
        val expPayload = LicensePayload(id = "BA-TEST-2345-6789", iat = now - 86400, exp = now - 3600)
        assertFalse(expPayload.isPermanent)
        assertTrue(now > expPayload.exp)
    }

    @Test
    fun testPermissionFlagsExtraction() {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
        val kp = kpg.generateKeyPair()
        val pubBase64 = Base64.getEncoder().encodeToString(kp.public.encoded)
        val installId = "BA-FLAG-TEST-2345"

        // Restricted license: maxWorkspaces=2, allowExcel=0, allowLock=0, allowSync=0
        val restrictedCode = generateTestCode(
            privateKey = kp.private,
            installationId = installId,
            maxWorkspaces = 2,
            allowExcel = 0,
            allowLock = 0,
            allowSync = 0
        )

        val res = LicenseVerifier.verify(restrictedCode, installId, pubBase64)
        assertEquals(LicenseStatus.ACTIVE, res.status)
        val p = res.payload!!
        assertEquals(2, p.maxWorkspaces)
        assertFalse("Excel export should be disabled", p.allowExcelExport)
        assertFalse("App lock should be disabled", p.allowAppLock)
        assertFalse("Background sync should be disabled", p.allowBackgroundSync)

        // Full license: maxWorkspaces=0 (unlimited), allowExcel=1, allowLock=1, allowSync=1
        val fullCode = generateTestCode(
            privateKey = kp.private,
            installationId = installId,
            maxWorkspaces = 0,
            allowExcel = 1,
            allowLock = 1,
            allowSync = 1
        )

        val fullRes = LicenseVerifier.verify(fullCode, installId, pubBase64)
        assertEquals(LicenseStatus.ACTIVE, fullRes.status)
        val pFull = fullRes.payload!!
        assertEquals(0, pFull.maxWorkspaces)
        assertTrue("Excel export should be enabled", pFull.allowExcelExport)
        assertTrue("App lock should be enabled", pFull.allowAppLock)
        assertTrue("Background sync should be enabled", pFull.allowBackgroundSync)
    }

    @Test
    fun testEmbeddedDefaultPublicKeyMatchesGeneratedPrivateKey() {
        val privFile = getPrivateKeyFile()
        assertTrue("private_key.pem must exist", privFile.exists())

        val privPem = String(Files.readAllBytes(privFile.toPath()), StandardCharsets.UTF_8)
        val privBase64 = privPem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s+".toRegex(), "")
        val privBytes = Base64.getDecoder().decode(privBase64)
        val privSpec = PKCS8EncodedKeySpec(privBytes)
        val kf = KeyFactory.getInstance("EC")
        val privateKey = kf.generatePrivate(privSpec)

        val installId = "BA-DEVC-2345-6789"
        val code = generateTestCode(
            privateKey = privateKey,
            installationId = installId,
            clientName = "عميل رسمي",
            exp = 0L,
            maxWorkspaces = 0,
            allowExcel = 1,
            allowLock = 1,
            allowSync = 1
        )

        // Verify using default embedded public key
        val res = LicenseVerifier.verify(
            rawCode = code,
            expectedInstallationId = installId
        )

        assertEquals("Must be ACTIVE with embedded default public key", LicenseStatus.ACTIVE, res.status)
        assertNotNull(res.payload)
        assertEquals("عميل رسمي", res.payload?.n)
        assertEquals(installId, res.payload?.id)
        assertTrue(res.payload?.allowExcelExport == true)
        assertTrue(res.payload?.allowAppLock == true)
        assertTrue(res.payload?.allowBackgroundSync == true)
        assertEquals(0, res.payload?.maxWorkspaces)
    }

    @Test
    fun testWhitespaceAndNewlineSanitization() {
        val privFile = getPrivateKeyFile()
        val privPem = String(Files.readAllBytes(privFile.toPath()), StandardCharsets.UTF_8)
        val privBase64 = privPem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s+".toRegex(), "")
        val privBytes = Base64.getDecoder().decode(privBase64)
        val privSpec = PKCS8EncodedKeySpec(privBytes)
        val kf = KeyFactory.getInstance("EC")
        val privateKey = kf.generatePrivate(privSpec)

        val installId = "BA-CLEA-2345-6789"
        val code = generateTestCode(privateKey, installId)

        // Inject newlines and spaces like from WhatsApp paste
        val messyCode = "  \n  " + code.substring(0, 15) + " \n " + code.substring(15) + "  \r\n "

        val res = LicenseVerifier.verify(messyCode, installId)
        assertEquals(LicenseStatus.ACTIVE, res.status)
        assertEquals(installId, res.payload?.id)
    }
}
