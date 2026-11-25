package com.example.fastfilesearch.licensing

import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object LicenseVerifier {
    // Embedded ECDSA P-256 Public Key (X.509 Base64) - NO private key in app
    const val DEFAULT_PUBLIC_KEY_BASE64 =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAERt37fPahuYV+EG8Jr9zMBgYjxJdgjKaKq5CFNjVrFve5EL7N7lB1zDAqtyNniG2NkPGZbzWbLVBx1ZwjCaM5HQ=="

    private const val PREFIX = "BA1"

    fun cleanLicenseCode(rawCode: String): String {
        val compact = rawCode.replace("\\s+".toRegex(), "").trim()
        return if (V2LicenseCodec.looksLikeV2LicenseCode(rawCode)) {
            V2LicenseCodec.formatGroupedCode16(rawCode)
        } else {
            compact
        }
    }

    fun parsePayloadJson(jsonStr: String): LicensePayload {
        fun extractString(key: String): String? {
            val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
            return pattern.find(jsonStr)?.groupValues?.get(1)
        }

        fun extractLong(key: String, default: Long = 0L): Long {
            val pattern = "\"$key\"\\s*:\\s*(-?\\d+)".toRegex()
            return pattern.find(jsonStr)?.groupValues?.get(1)?.toLongOrNull() ?: default
        }

        fun extractInt(key: String, default: Int = 0): Int {
            val pattern = "\"$key\"\\s*:\\s*(-?\\d+)".toRegex()
            return pattern.find(jsonStr)?.groupValues?.get(1)?.toIntOrNull() ?: default
        }

        val id = extractString("id") ?: throw IllegalArgumentException("Missing id in payload")
        val n = extractString("n")
        val v = extractInt("v", 1)
        val iat = extractLong("iat", 0L)
        val exp = extractLong("exp", 0L)
        val mw = extractInt("mw", 0)
        val ex = extractInt("ex", 1)
        val lk = extractInt("lk", 1)
        val bg = extractInt("bg", 1)

        return LicensePayload(
            v = v,
            id = id,
            n = n?.takeIf { it.isNotBlank() },
            iat = iat,
            exp = exp,
            mw = mw,
            ex = ex,
            lk = lk,
            bg = bg
        )
    }

    fun verify(
        rawCode: String,
        expectedInstallationId: String,
        publicKeyBase64: String = DEFAULT_PUBLIC_KEY_BASE64,
        deviceFpType: Int = 0,
        deviceHash6: ByteArray? = null,
        installNonce: Int = -1
    ): LicenseVerificationResult {
        if (rawCode.isBlank()) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                errorMessage = "كود الترخيص فارغ"
            )
        }

        val cleaned = rawCode.replace("\\s+".toRegex(), "").trim()
        if (cleaned.isBlank()) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                errorMessage = "كود الترخيص فارغ"
            )
        }

        // Route 16-char short V2 licenses (with/without hyphens, whitespace, case)
        if (!cleaned.contains(".")) {
            return verifyV2(
                rawCode = rawCode,
                deviceFpType = deviceFpType,
                deviceHash6 = deviceHash6,
                installNonce = installNonce
            )
        }

        val parts = cleaned.split(".")
        if (parts.size != 3) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                errorMessage = "صيغة كود الترخيص غير صحيحة"
            )
        }

        if (parts[0] != PREFIX) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                errorMessage = "إصدار كود الترخيص غير مدعوم"
            )
        }

        val payloadBase64Url = parts[1]
        val signatureBase64Url = parts[2]

        val payloadBytes: ByteArray
        val signatureBytes: ByteArray
        try {
            val urlDecoder = Base64.getUrlDecoder()
            payloadBytes = urlDecoder.decode(payloadBase64Url)
            signatureBytes = urlDecoder.decode(signatureBase64Url)
        } catch (_: Exception) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                errorMessage = "ترميز كود الترخيص تالف"
            )
        }

        val payloadString = String(payloadBytes, StandardCharsets.UTF_8)
        val payload: LicensePayload
        try {
            payload = parsePayloadJson(payloadString)
            if (payload.v != 1) {
                return LicenseVerificationResult(
                    status = LicenseStatus.INVALID,
                    errorMessage = "إصدار بيانات الترخيص غير متوافق"
                )
            }
        } catch (_: Exception) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                errorMessage = "بيانات الترخيص غير صالحة"
            )
        }

        // Verify that ID strictly matches current installation ID
        if (payload.id != expectedInstallationId) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                payload = payload,
                errorMessage = "كود الترخيص غير مخصص لهذا الجهاز"
            )
        }

        // Verify cryptographic signature with SHA256withECDSA
        val signatureValid = try {
            val keyBytes = Base64.getDecoder().decode(publicKeyBase64)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("EC")
            val publicKey: PublicKey = keyFactory.generatePublic(keySpec)

            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(publicKey)
            verifier.update(payloadBytes)
            verifier.verify(signatureBytes)
        } catch (_: Exception) {
            false
        }

        if (!signatureValid) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                errorMessage = "التوقيع الرقمي للترخيص غير صحيح أو تم التلاعب به"
            )
        }

        return LicenseVerificationResult(
            status = LicenseStatus.ACTIVE,
            payload = payload
        )
    }

    fun verifyV2(
        rawCode: String,
        deviceFpType: Int,
        deviceHash6: ByteArray?,
        installNonce: Int
    ): LicenseVerificationResult {
        if (rawCode.isBlank()) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                errorMessage = "كود الترخيص فارغ"
            )
        }

        if (!V2LicenseCodec.looksLikeV2LicenseCode(rawCode)) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                errorMessage = "صيغة كود الترخيص غير صحيحة"
            )
        }

        if (deviceHash6 == null || deviceHash6.size != 6 || installNonce < 0) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                errorMessage = "تعذّر التحقق من بصمة الجهاز لهذا الكود"
            )
        }

        val decoded = V2LicenseCodec.decodeAndVerifyShortLicenseCode(
            rawCode = rawCode,
            expectedFpType = deviceFpType,
            expectedDeviceHash6 = deviceHash6,
            expectedInstallNonce = installNonce
        ) ?: return LicenseVerificationResult(
            status = LicenseStatus.INVALID,
            errorMessage = "كود الترخيص غير صحيح أو غير مخصص لهذا الجهاز"
        )

        if (decoded.platform != V2LicenseCodec.PLATFORM_ANDROID &&
            decoded.platform != V2LicenseCodec.PLATFORM_BOTH
        ) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                errorMessage = "كود الترخيص غير مخصص لنظام أندرويد"
            )
        }

        if (!decoded.allowAccess) {
            return LicenseVerificationResult(
                status = LicenseStatus.INVALID,
                errorMessage = "صلاحية الوصول معطّلة في هذا الترخيص"
            )
        }

        val derivedId = V2LicenseCodec.deriveInstallationId(deviceFpType, deviceHash6, installNonce)
        val payload = LicensePayload(
            v = 2,
            id = derivedId,
            n = null,
            iat = 0L,
            exp = decoded.expEpochSeconds,
            mw = decoded.maxFolders,
            ex = if (decoded.allowExcel) 1 else 0,
            lk = if (decoded.allowAppLock) 1 else 0,
            bg = if (decoded.allowAccess) 1 else 0
        )

        return LicenseVerificationResult(
            status = LicenseStatus.ACTIVE,
            payload = payload
        )
    }
}
