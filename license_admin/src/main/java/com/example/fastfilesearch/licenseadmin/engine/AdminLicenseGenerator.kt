package com.example.fastfilesearch.licenseadmin.engine

import com.example.fastfilesearch.licenseadmin.security.KeyStorageManager
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.Locale

data class AdminLicensePayload(
    val v: Int = 1,
    val id: String,
    val n: String? = null,
    val iat: Long,
    val exp: Long,
    val mw: Int = 0,
    val ex: Int = 1,
    val lk: Int = 1,
    val bg: Int = 1
)

data class ClientDeviceInfo(
    val manufacturer: String = "",
    val model: String = "",
    val osVersion: String = "",
    val abi: String = "",
    val appVersion: String = ""
) {
    val isAvailable: Boolean
        get() = manufacturer.isNotBlank() || model.isNotBlank() || osVersion.isNotBlank() || abi.isNotBlank() || appVersion.isNotBlank()
}

data class ParsedActivationRequest(
    val installationId: String,
    val deviceInfo: ClientDeviceInfo? = null
)

object AdminLicenseGenerator {

    private const val PREFIX = "BA1"
    private val INSTALLATION_ID_REGEX = Regex("""BA-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}""", RegexOption.IGNORE_CASE)
    private val BAR1_REGEX = Regex("""BAR1\.([A-Za-z0-9_-]+)""")

    /**
     * Parses activation request input.
     * 1) First checks for BAR1.<base64> pattern, decodes Base64Url, and extracts installationId + ClientDeviceInfo.
     * 2) If not found, falls back to BA-XXXX-XXXX-XXXX pattern (backward compatibility).
     * 3) If neither matches, returns trimmed input with null device info.
     */
    fun parseActivationRequest(rawInput: String): ParsedActivationRequest {
        val bar1Match = BAR1_REGEX.find(rawInput)
        if (bar1Match != null) {
            val base64Payload = bar1Match.groupValues[1]
            try {
                val decodedBytes = Base64.getUrlDecoder().decode(base64Payload)
                val json = String(decodedBytes, StandardCharsets.UTF_8)

                fun extractField(key: String): String {
                    val p = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
                    val m = p.find(json) ?: return ""
                    return m.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\")
                }

                val id = extractField("id")
                val m = extractField("m")
                val d = extractField("d")
                val os = extractField("os")
                val abi = extractField("abi")
                val v = extractField("v")

                val cleanId = if (id.isNotBlank()) cleanInstallationId(id) else ""
                val devInfo = ClientDeviceInfo(
                    manufacturer = m,
                    model = d,
                    osVersion = os,
                    abi = abi,
                    appVersion = v
                )

                if (cleanId.isNotBlank()) {
                    return ParsedActivationRequest(
                        installationId = cleanId,
                        deviceInfo = if (devInfo.isAvailable) devInfo else null
                    )
                }
            } catch (_: Exception) {}
        }

        // Backward compatibility fallback to plain BA-XXXX-XXXX-XXXX
        val idMatch = INSTALLATION_ID_REGEX.find(rawInput)
        if (idMatch != null) {
            return ParsedActivationRequest(
                installationId = idMatch.value.uppercase(Locale.ROOT),
                deviceInfo = null
            )
        }

        return ParsedActivationRequest(
            installationId = rawInput.trim(),
            deviceInfo = null
        )
    }

    /**
     * Extracts installation ID matching BA-XXXX-XXXX-XXXX from text (e.g. from WhatsApp message)
     * If found, returns it in uppercase; otherwise returns the trimmed input text.
     */
    fun extractInstallationId(rawText: String): String {
        return parseActivationRequest(rawText).installationId
    }

    /**
     * Cleans installation ID: removes whitespace and converts to uppercase.
     */
    fun cleanInstallationId(rawId: String): String {
        val idMatch = INSTALLATION_ID_REGEX.find(rawId)
        val candidate = idMatch?.value ?: rawId
        return candidate.replace("\\s+".toRegex(), "").uppercase(Locale.ROOT)
    }

    /**
     * Builds the JSON payload string matching the exact format of LicenseVerifier and LicenseGeneratorGui.
     */
    fun buildPayloadJson(
        installationId: String,
        clientName: String?,
        iat: Long,
        exp: Long,
        maxWorkspaces: Int,
        excelExport: Boolean,
        appLock: Boolean,
        bgSync: Boolean
    ): String {
        val cleanId = cleanInstallationId(installationId)
        val sb = StringBuilder("{")
        sb.append("\"v\":1,")
        sb.append("\"id\":\"").append(cleanId).append("\",")
        if (!clientName.isNullOrBlank()) {
            val safeName = clientName.trim().replace("\\", "\\\\").replace("\"", "\\\"")
            sb.append("\"n\":\"").append(safeName).append("\",")
        }
        sb.append("\"iat\":").append(iat).append(",")
        sb.append("\"exp\":").append(exp).append(",")
        sb.append("\"mw\":").append(maxWorkspaces).append(",")
        sb.append("\"ex\":").append(if (excelExport) 1 else 0).append(",")
        sb.append("\"lk\":").append(if (appLock) 1 else 0).append(",")
        sb.append("\"bg\":").append(if (bgSync) 1 else 0)
        sb.append("}")
        return sb.toString()
    }

    /**
     * Signs the payload and constructs the full license code BA1.<payload>.<sig>.
     * Immediately self-verifies the signature with the public key.
     */
    fun generateLicense(
        privateKey: PrivateKey,
        installationId: String,
        clientName: String? = null,
        durationDays: Long = 0L, // 0 = permanent
        maxWorkspaces: Int = 0, // 0 = unlimited
        excelExport: Boolean = true,
        appLock: Boolean = true,
        bgSync: Boolean = true,
        publicKeyBase64: String = KeyStorageManager.EMBEDDED_PUBLIC_KEY_BASE64
    ): Result<String> {
        val cleanId = cleanInstallationId(installationId)
        if (cleanId.isBlank()) {
            return Result.failure(IllegalArgumentException("معرّف التثبيت لا يمكن أن يكون فارغاً"))
        }

        return try {
            val iat = System.currentTimeMillis() / 1000L
            val exp = if (durationDays <= 0L) 0L else iat + (durationDays * 24L * 3600L)

            val jsonPayload = buildPayloadJson(
                installationId = cleanId,
                clientName = clientName,
                iat = iat,
                exp = exp,
                maxWorkspaces = maxWorkspaces,
                excelExport = excelExport,
                appLock = appLock,
                bgSync = bgSync
            )

            val payloadBytes = jsonPayload.toByteArray(StandardCharsets.UTF_8)

            // Sign using SHA256withECDSA
            val signer = Signature.getInstance("SHA256withECDSA")
            signer.initSign(privateKey)
            signer.update(payloadBytes)
            val signatureBytes = signer.sign()

            // Self-verify using public key
            val pubBytes = Base64.getDecoder().decode(publicKeyBase64)
            val pubSpec = X509EncodedKeySpec(pubBytes)
            val kf = KeyFactory.getInstance("EC")
            val publicKey: PublicKey = kf.generatePublic(pubSpec)

            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(publicKey)
            verifier.update(payloadBytes)
            if (!verifier.verify(signatureBytes)) {
                return Result.failure(IllegalStateException("فشل التحقق الذاتي من التوقيع الرقمي للترخيص"))
            }

            // Encode to Base64Url without padding
            val urlEncoder = Base64.getUrlEncoder().withoutPadding()
            val payloadB64 = urlEncoder.encodeToString(payloadBytes)
            val sigB64 = urlEncoder.encodeToString(signatureBytes)

            val fullLicenseCode = "$PREFIX.$payloadB64.$sigB64"
            Result.success(fullLicenseCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses the payload from a generated license code.
     */
    fun parsePayload(licenseCode: String): AdminLicensePayload {
        val parts = licenseCode.trim().split(".")
        require(parts.size == 3 && parts[0] == PREFIX) { "صيغة الترخيص غير صحيحة" }

        val payloadBytes = Base64.getUrlDecoder().decode(parts[1])
        val jsonStr = String(payloadBytes, StandardCharsets.UTF_8)

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

        val id = extractString("id") ?: throw IllegalArgumentException("Missing id")
        val n = extractString("n")
        val v = extractInt("v", 1)
        val iat = extractLong("iat", 0L)
        val exp = extractLong("exp", 0L)
        val mw = extractInt("mw", 0)
        val ex = extractInt("ex", 1)
        val lk = extractInt("lk", 1)
        val bg = extractInt("bg", 1)

        return AdminLicensePayload(
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
}
