package com.example.fastfilesearch.licenseadmin.engine

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Shared binary V2 codecs for:
 * 1) Compressed R2 activation request (~24-28 Base62 chars)
 * 2) Short 16-char device+install bound license code (XXXX-XXXX-XXXX-XXXX)
 *
 * Must remain bit-for-bit identical between :app and :license_admin.
 */
object V2LicenseCodec {

    const val R2_PREFIX = "R2"
    const val R2_VERSION = 2
    const val LICENSE_VERSION_2 = 2

    const val PLATFORM_ANDROID = 0
    const val PLATFORM_WINDOWS = 1
    const val PLATFORM_BOTH = 2

    const val FP_TYPE_WV = 0
    const val FP_TYPE_AD = 1
    const val FP_TYPE_WIN = 2

    const val ABI_ARM64 = 0
    const val ABI_ARMV7 = 1
    const val ABI_X64 = 2
    const val ABI_OTHER = 3

    const val PLAN_TRIAL = 0
    const val PLAN_MONTHLY = 1
    const val PLAN_QUARTERLY = 2
    const val PLAN_YEARLY = 3
    const val PLAN_LIFETIME = 4
    const val PLAN_CUSTOM = 5

    // 2025-01-01 UTC epoch day = 20089 (14 bits covers until ~2069)
    val EPOCH_2025_DAY: Long = LocalDate.of(2025, 1, 1).toEpochDay()
    const val MAX_EXPIRY_DAYS = 16383
    const val MAX_MODEL_CHARS = 10

    private const val BASE62_ALPHABET =
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private const val UNAMBIGUOUS_BASE32_ALPHABET =
        "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val HEX_DIGITS = "0123456789abcdef"

    // 6-bit ASCII character table: index 0 = terminator/padding, 1..63 = printable subset
    private val SIX_BIT_CHARS: CharArray = charArrayOf(
        '\u0000', // 0: terminator
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', // 1..13
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', // 14..26
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',                // 27..36
        ' ', '-', '_', '.', '/', '|', '+', ':', '(', ')'                 // 37..46
    )

    private val CHAR_TO_SIX_BIT: Map<Char, Int> = buildMap {
        for (i in 1 until SIX_BIT_CHARS.size) {
            put(SIX_BIT_CHARS[i], i)
        }
    }

    data class DecodedR2Request(
        val version: Int,
        val platform: Int,
        val fpType: Int,
        val abiCode: Int,
        val deviceHash6: ByteArray,
        val installNonce: Int,
        val osCode: Int,
        val appVersionCodeCompact: Int,
        val manufacturerId: Int,
        val manufacturer: String,
        val model: String,
        val osVersion: String,
        val abi: String,
        val appVersion: String,
        val clientDeviceId: String,
        val installationId: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DecodedR2Request) return false
            return version == other.version &&
                platform == other.platform &&
                fpType == other.fpType &&
                abiCode == other.abiCode &&
                deviceHash6.contentEquals(other.deviceHash6) &&
                installNonce == other.installNonce &&
                osCode == other.osCode &&
                appVersionCodeCompact == other.appVersionCodeCompact &&
                manufacturerId == other.manufacturerId &&
                manufacturer == other.manufacturer &&
                model == other.model &&
                osVersion == other.osVersion &&
                abi == other.abi &&
                appVersion == other.appVersion &&
                clientDeviceId == other.clientDeviceId &&
                installationId == other.installationId
        }

        override fun hashCode(): Int {
            var result = version
            result = 31 * result + platform
            result = 31 * result + fpType
            result = 31 * result + abiCode
            result = 31 * result + deviceHash6.contentHashCode()
            result = 31 * result + installNonce
            result = 31 * result + osCode
            result = 31 * result + appVersionCodeCompact
            result = 31 * result + manufacturerId
            result = 31 * result + manufacturer.hashCode()
            result = 31 * result + model.hashCode()
            result = 31 * result + osVersion.hashCode()
            result = 31 * result + abi.hashCode()
            result = 31 * result + appVersion.hashCode()
            result = 31 * result + clientDeviceId.hashCode()
            result = 31 * result + installationId.hashCode()
            return result
        }
    }

    data class DecodedV2License(
        val version: Int,
        val platform: Int,
        val expiryDaysSince2025: Int,
        val expEpochSeconds: Long,
        val planTypeCode: Int,
        val planTypeName: String,
        val maxFolders: Int,
        val allowExcel: Boolean,
        val allowAppLock: Boolean,
        val allowAccess: Boolean,
        val reservedBit: Boolean,
        val payloadBytes: ByteArray,
        val signatureBytes: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DecodedV2License) return false
            return version == other.version &&
                platform == other.platform &&
                expiryDaysSince2025 == other.expiryDaysSince2025 &&
                expEpochSeconds == other.expEpochSeconds &&
                planTypeCode == other.planTypeCode &&
                planTypeName == other.planTypeName &&
                maxFolders == other.maxFolders &&
                allowExcel == other.allowExcel &&
                allowAppLock == other.allowAppLock &&
                allowAccess == other.allowAccess &&
                reservedBit == other.reservedBit &&
                payloadBytes.contentEquals(other.payloadBytes) &&
                signatureBytes.contentEquals(other.signatureBytes)
        }

        override fun hashCode(): Int {
            var result = version
            result = 31 * result + platform
            result = 31 * result + expiryDaysSince2025
            result = 31 * result + expEpochSeconds.hashCode()
            result = 31 * result + planTypeCode
            result = 31 * result + planTypeName.hashCode()
            result = 31 * result + maxFolders
            result = 31 * result + allowExcel.hashCode()
            result = 31 * result + allowAppLock.hashCode()
            result = 31 * result + allowAccess.hashCode()
            result = 31 * result + reservedBit.hashCode()
            result = 31 * result + payloadBytes.contentHashCode()
            result = 31 * result + signatureBytes.contentHashCode()
            return result
        }
    }

    fun fpTypePrefix(fpType: Int): String = when (fpType) {
        FP_TYPE_WV -> "WV"
        FP_TYPE_AD -> "AD"
        FP_TYPE_WIN -> "WIN"
        else -> "AD"
    }

    fun encodeAbiCode(abiString: String): Int {
        val norm = abiString.trim().lowercase(Locale.ROOT)
        return when {
            norm.contains("arm64") || norm.contains("aarch64") -> ABI_ARM64
            norm.contains("armeabi") || norm.contains("armv7") -> ABI_ARMV7
            norm.contains("x86_64") || norm.contains("x64") || norm.contains("amd64") -> ABI_X64
            else -> ABI_OTHER
        }
    }

    fun decodeAbiCode(abiCode: Int): String = when (abiCode) {
        ABI_ARM64 -> "arm64-v8a"
        ABI_ARMV7 -> "armeabi-v7a"
        ABI_X64 -> "x86_64"
        else -> "other"
    }

    fun encodeAppVersionCompact(appVersion: String, versionCodeFallback: Int = 500): Int {
        val parts = appVersion.trim()
            .substringBefore(" ")
            .substringBefore("-")
            .split(".")
        val major = parts.getOrNull(0)?.toIntOrNull()
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        if (major != null && major in 0..25 && minor in 0..9) {
            return (major * 10 + minor).coerceIn(1, 255)
        }
        return (versionCodeFallback / 10).coerceIn(1, 255)
    }

    fun decodeAppVersionCompact(compactByte: Int): String {
        val v = compactByte and 0xFF
        if (v <= 0) return "unknown"
        return "${v / 10}.${v % 10}.0"
    }

    fun decodeOsVersion(platform: Int, osByte: Int): String {
        val api = osByte and 0xFF
        if (platform == PLATFORM_WINDOWS) {
            return if (api > 0) "Windows $api" else "Windows"
        }
        val release = when (api) {
            26 -> "8.0"
            27 -> "8.1"
            28 -> "9"
            29 -> "10"
            30 -> "11"
            31 -> "12"
            32 -> "12L"
            33 -> "13"
            34 -> "14"
            35 -> "15"
            36 -> "16"
            37 -> "17"
            else -> null
        }
        return if (release != null) {
            "Android $release (API $api)"
        } else if (api > 0) {
            "Android (API $api)"
        } else {
            "Android"
        }
    }

    fun encodePlanType(planType: String): Int = when (planType.trim().lowercase(Locale.ROOT)) {
        "trial" -> PLAN_TRIAL
        "monthly" -> PLAN_MONTHLY
        "quarterly" -> PLAN_QUARTERLY
        "yearly" -> PLAN_YEARLY
        "lifetime" -> PLAN_LIFETIME
        else -> PLAN_CUSTOM
    }

    fun decodePlanType(code: Int): String = when (code and 0x07) {
        PLAN_TRIAL -> "trial"
        PLAN_MONTHLY -> "monthly"
        PLAN_QUARTERLY -> "quarterly"
        PLAN_YEARLY -> "yearly"
        PLAN_LIFETIME -> "lifetime"
        else -> "custom"
    }

    fun daysSince2025ToExpSeconds(expiryDaysSince2025: Int): Long {
        if (expiryDaysSince2025 <= 0) return 0L
        return ((EPOCH_2025_DAY + expiryDaysSince2025.toLong() + 1L) * 86_400L) - 1L
    }

    fun calculateExpiryDaysSince2025(
        durationDays: Long,
        nowSeconds: Long = Instant.now().epochSecond
    ): Int {
        if (durationDays <= 0L) return 0
        val todayUtcDay = Math.floorDiv(nowSeconds, 86_400L)
        val targetDay = todayUtcDay + durationDays
        return (targetDay - EPOCH_2025_DAY).toInt().coerceIn(1, MAX_EXPIRY_DAYS)
    }

    fun bytesToHexLower(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(HEX_DIGITS[v ushr 4])
            sb.append(HEX_DIGITS[v and 0x0F])
        }
        return sb.toString()
    }

    fun formatClientDeviceIdShort(fpType: Int, deviceHash6: ByteArray): String {
        require(deviceHash6.size == 6) { "deviceHash6 must be exactly 6 bytes" }
        return "${fpTypePrefix(fpType)}-${bytesToHexLower(deviceHash6)}"
    }

    fun deriveInstallationId(fpType: Int, deviceHash6: ByteArray, installNonce: Int): String {
        require(deviceHash6.size == 6) { "deviceHash6 must be exactly 6 bytes" }
        val md = MessageDigest.getInstance("SHA-256")
        md.update("R2ID".toByteArray(StandardCharsets.UTF_8))
        md.update((fpType and 0xFF).toByte())
        md.update(deviceHash6)
        md.update(((installNonce ushr 8) and 0xFF).toByte())
        md.update((installNonce and 0xFF).toByte())
        val digest = md.digest()

        val sb = StringBuilder(17)
        sb.append("BA-")
        for (i in 0 until 12) {
            if (i == 4 || i == 8) sb.append('-')
            val idx = (digest[i].toInt() and 0xFF) % UNAMBIGUOUS_BASE32_ALPHABET.length
            sb.append(UNAMBIGUOUS_BASE32_ALPHABET[idx])
        }
        return sb.toString()
    }

    internal fun prepareModelTextForR2(
        manufacturerId: Int,
        manufacturer: String,
        model: String
    ): String {
        fun cleanChunk(s: String): String {
            val sb = StringBuilder(s.length)
            var lastWasSpace = false
            for (ch in s.trim().uppercase(Locale.ROOT)) {
                val mapped = when {
                    CHAR_TO_SIX_BIT.containsKey(ch) && ch != '|' -> ch
                    ch.isWhitespace() -> ' '
                    else -> ' '
                }
                if (mapped == ' ') {
                    if (!lastWasSpace && sb.isNotEmpty()) {
                        sb.append(' ')
                        lastWasSpace = true
                    }
                } else {
                    sb.append(mapped)
                    lastWasSpace = false
                }
            }
            return sb.toString().trim()
        }

        val cleanBrand = cleanChunk(manufacturer)
        var cleanModel = cleanChunk(model)

        if (cleanBrand.isNotEmpty() && cleanModel.startsWith(cleanBrand)) {
            cleanModel = cleanModel.removePrefix(cleanBrand).trimStart(' ', '-', '_', '/', ':')
        }

        if (manufacturerId > 0) {
            return cleanModel.take(MAX_MODEL_CHARS).trim()
        }

        return if (cleanBrand.isNotEmpty() && cleanModel.isNotEmpty()) {
            val brandPart = cleanBrand.take(4).trim()
            val remaining = (MAX_MODEL_CHARS - brandPart.length - 1).coerceAtLeast(1)
            "$brandPart|${cleanModel.take(remaining)}".take(MAX_MODEL_CHARS)
        } else {
            (cleanModel.ifEmpty { cleanBrand }).take(MAX_MODEL_CHARS).trim()
        }
    }

    internal fun pack6BitAscii(text: String): ByteArray {
        val capped = text.take(MAX_MODEL_CHARS)
        if (capped.isEmpty()) return ByteArray(0)
        val totalBits = capped.length * 6
        val byteLen = (totalBits + 7) / 8
        val out = ByteArray(byteLen)
        var bitPos = 0
        for (ch in capped) {
            val code = CHAR_TO_SIX_BIT[ch.uppercaseChar()] ?: 37 // fallback space
            for (b in 5 downTo 0) {
                val bit = (code ushr b) and 1
                if (bit == 1) {
                    val byteIdx = bitPos ushr 3
                    val bitInByte = 7 - (bitPos and 7)
                    out[byteIdx] = (out[byteIdx].toInt() or (1 shl bitInByte)).toByte()
                }
                bitPos++
            }
        }
        return out
    }

    internal fun unpack6BitAscii(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val totalBits = bytes.size * 8
        val count = (totalBits / 6).coerceAtMost(MAX_MODEL_CHARS)
        val sb = StringBuilder(count)
        var bitPos = 0
        for (i in 0 until count) {
            var code = 0
            for (b in 0 until 6) {
                val byteIdx = bitPos ushr 3
                val bitInByte = 7 - (bitPos and 7)
                val bit = (bytes[byteIdx].toInt() ushr bitInByte) and 1
                code = (code shl 1) or bit
                bitPos++
            }
            if (code == 0) break
            val ch = SIX_BIT_CHARS.getOrElse(code) { ' ' }
            sb.append(ch)
        }
        return sb.toString().trim()
    }

    fun encodeBase62(bytes: ByteArray): String {
        if (bytes.isEmpty()) return "0"
        var value = BigInteger(1, bytes)
        if (value == BigInteger.ZERO) return "0"
        val base = BigInteger.valueOf(62L)
        val sb = StringBuilder()
        while (value > BigInteger.ZERO) {
            val divRem = value.divideAndRemainder(base)
            sb.append(BASE62_ALPHABET[divRem[1].toInt()])
            value = divRem[0]
        }
        return sb.reverse().toString()
    }

    fun decodeBase62(encoded: String): ByteArray {
        require(encoded.isNotEmpty()) { "Empty Base62 string" }
        val base = BigInteger.valueOf(62L)
        var value = BigInteger.ZERO
        for (ch in encoded) {
            val digit = BASE62_ALPHABET.indexOf(ch)
            require(digit >= 0) { "Invalid Base62 character: $ch" }
            value = value.multiply(base).add(BigInteger.valueOf(digit.toLong()))
        }
        val raw = value.toByteArray()
        return if (raw.size > 1 && raw[0] == 0.toByte()) {
            raw.copyOfRange(1, raw.size)
        } else {
            raw
        }
    }

    fun encodeR2Request(
        platform: Int = PLATFORM_ANDROID,
        fpType: Int,
        abiCode: Int,
        deviceHash6: ByteArray,
        installNonce: Int,
        osVersionInt: Int,
        appVersionCompact: Int,
        manufacturer: String,
        model: String
    ): String {
        require(deviceHash6.size == 6) { "deviceHash6 must be 6 bytes" }
        val mfgId = ManufacturerDictionary.findId(manufacturer)
        val cleanModel = prepareModelTextForR2(mfgId, manufacturer, model)
        val modelBytes = pack6BitAscii(cleanModel)

        val payload = ByteArray(12 + modelBytes.size)
        val headerByte = ((R2_VERSION and 0x07) shl 5) or
            ((platform and 0x01) shl 4) or
            ((fpType and 0x03) shl 2) or
            (abiCode and 0x03)
        payload[0] = headerByte.toByte()
        System.arraycopy(deviceHash6, 0, payload, 1, 6)
        payload[7] = ((installNonce ushr 8) and 0xFF).toByte()
        payload[8] = (installNonce and 0xFF).toByte()
        payload[9] = (osVersionInt.coerceIn(0, 255)).toByte()
        payload[10] = (appVersionCompact.coerceIn(0, 255)).toByte()
        payload[11] = (mfgId and 0xFF).toByte()
        if (modelBytes.isNotEmpty()) {
            System.arraycopy(modelBytes, 0, payload, 12, modelBytes.size)
        }

        return R2_PREFIX + encodeBase62(payload)
    }

    fun extractR2Token(rawInput: String): String? {
        val regex = "\\bR2[0-9A-Za-z]{16,42}\\b".toRegex()
        val directClean = rawInput.trim()
        if (directClean.matches("^R2[0-9A-Za-z]{16,42}$".toRegex())) {
            return directClean
        }
        return regex.find(rawInput)?.value
    }

    fun decodeR2Request(rawInput: String): DecodedR2Request? {
        val token = extractR2Token(rawInput) ?: return null
        return try {
            val bytes = decodeBase62(token.removePrefix(R2_PREFIX))
            if (bytes.size < 12) return null

            val header = bytes[0].toInt() and 0xFF
            val version = (header ushr 5) and 0x07
            if (version != R2_VERSION) return null

            val platform = (header ushr 4) and 0x01
            val fpType = (header ushr 2) and 0x03
            val abiCode = header and 0x03

            val deviceHash6 = bytes.copyOfRange(1, 7)
            val installNonce = ((bytes[7].toInt() and 0xFF) shl 8) or (bytes[8].toInt() and 0xFF)
            val osCode = bytes[9].toInt() and 0xFF
            val appVersionCompact = bytes[10].toInt() and 0xFF
            val manufacturerId = bytes[11].toInt() and 0xFF

            val modelPacked = if (bytes.size > 12) bytes.copyOfRange(12, bytes.size) else ByteArray(0)
            val rawDecodedModel = unpack6BitAscii(modelPacked)

            val resolvedManufacturer: String
            val resolvedModel: String
            if (manufacturerId > 0) {
                resolvedManufacturer = ManufacturerDictionary.getName(manufacturerId)
                resolvedModel = rawDecodedModel
            } else if (rawDecodedModel.contains('|')) {
                val parts = rawDecodedModel.split('|', limit = 2)
                resolvedManufacturer = parts[0].trim()
                resolvedModel = parts.getOrElse(1) { "" }.trim()
            } else {
                resolvedManufacturer = "Unknown"
                resolvedModel = rawDecodedModel
            }

            val clientDeviceId = formatClientDeviceIdShort(fpType, deviceHash6)
            val installationId = deriveInstallationId(fpType, deviceHash6, installNonce)

            DecodedR2Request(
                version = version,
                platform = platform,
                fpType = fpType,
                abiCode = abiCode,
                deviceHash6 = deviceHash6,
                installNonce = installNonce,
                osCode = osCode,
                appVersionCodeCompact = appVersionCompact,
                manufacturerId = manufacturerId,
                manufacturer = resolvedManufacturer,
                model = resolvedModel,
                osVersion = decodeOsVersion(platform, osCode),
                abi = decodeAbiCode(abiCode),
                appVersion = decodeAppVersionCompact(appVersionCompact),
                clientDeviceId = clientDeviceId,
                installationId = installationId
            )
        } catch (_: Exception) {
            null
        }
    }

    fun encodeBase32Unambiguous(bytes: ByteArray): String {
        require(bytes.size == 10) { "V2 license binary must be exactly 10 bytes (80 bits)" }
        val sb = StringBuilder(16)
        var bitPos = 0
        for (i in 0 until 16) {
            var idx = 0
            for (b in 0 until 5) {
                val byteIdx = bitPos ushr 3
                val bitInByte = 7 - (bitPos and 7)
                val bit = (bytes[byteIdx].toInt() ushr bitInByte) and 1
                idx = (idx shl 1) or bit
                bitPos++
            }
            sb.append(UNAMBIGUOUS_BASE32_ALPHABET[idx])
        }
        return sb.toString()
    }

    fun decodeBase32Unambiguous(raw: String): ByteArray {
        val cleaned = cleanV2LicenseInput(raw)
        require(cleaned.length == 16) { "V2 license code must be exactly 16 Base32 characters" }
        val out = ByteArray(10)
        var bitPos = 0
        for (ch in cleaned) {
            val idx = UNAMBIGUOUS_BASE32_ALPHABET.indexOf(ch)
            require(idx >= 0) { "Invalid Base32 character: $ch" }
            for (b in 4 downTo 0) {
                val bit = (idx ushr b) and 1
                if (bit == 1) {
                    val byteIdx = bitPos ushr 3
                    val bitInByte = 7 - (bitPos and 7)
                    out[byteIdx] = (out[byteIdx].toInt() or (1 shl bitInByte)).toByte()
                }
                bitPos++
            }
        }
        return out
    }

    fun cleanV2LicenseInput(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw.uppercase(Locale.ROOT)) {
            if (ch.isWhitespace() || ch == '-' ||
                ch == '\u200E' || ch == '\u200F' || ch == '\u061C' ||
                ch in '\u202A'..'\u202E'
            ) {
                continue
            }
            sb.append(ch)
        }
        return sb.toString()
    }

    fun looksLikeV2LicenseCode(raw: String): Boolean {
        val cleaned = cleanV2LicenseInput(raw)
        if (cleaned.length != 16) return false
        return cleaned.all { UNAMBIGUOUS_BASE32_ALPHABET.indexOf(it) >= 0 }
    }

    fun formatGroupedCode16(rawOrFormatted: String): String {
        val cleaned = cleanV2LicenseInput(rawOrFormatted)
        if (cleaned.length != 16) return rawOrFormatted
        return "${cleaned.substring(0, 4)}-${cleaned.substring(4, 8)}-" +
            "${cleaned.substring(8, 12)}-${cleaned.substring(12, 16)}"
    }

    // --- Split & masked shared secret key assembly (no plaintext single string) ---
    private fun keySegment0(): IntArray = intArrayOf(
        0xD4 xor 0x91, 0x2F xor 0x7A, 0xC1 xor 0x83, 0x5E xor 0x37,
        0xA8 xor 0x42, 0x19 xor 0x6F, 0xE7 xor 0xB5, 0x33 xor 0x5D
    )

    private fun keySegment1(): IntArray = intArrayOf(
        0x72 + 0x19, 0xD3 - 0x4E, 0x44 xor 0x2B, 0x9E xor 0xE1,
        0x61 + 0x24, 0xC5 xor 0x98, 0x1A xor 0x4F, 0xB8 - 0x39
    )

    private fun keySegment2(): IntArray = intArrayOf(
        (0x3C shl 1) xor 0x11, 0x8F xor 0x26, 0x51 + 0x47, 0xA9 xor 0x3F,
        0x22 xor 0x79, 0xE4 xor 0x52, 0x7B - 0x1D, 0x90 xor 0xC7
    )

    private fun keySegment3(): IntArray = intArrayOf(
        0x4D xor 0xA3, 0x66 + 0x3B, 0xF1 xor 0x6C, 0x15 xor 0x88,
        0xC9 xor 0x7E, 0x3A + 0x55, 0x82 xor 0x29, 0xD7 xor 0x41
    )

    private fun assembleSigningKey(): ByteArray {
        val out = ByteArray(32)
        val segs = arrayOf(keySegment0(), keySegment1(), keySegment2(), keySegment3())
        var idx = 0
        for (sIdx in segs.indices) {
            val seg = segs[sIdx]
            for (j in seg.indices) {
                val mixer = ((idx * 17) + (sIdx * 31) + 0x5A) and 0xFF
                out[idx] = (seg[j] xor mixer).toByte()
                idx++
            }
        }
        return out
    }

    internal fun computeHmacTruncated6(
        payload4: ByteArray,
        fpType: Int,
        deviceHash6: ByteArray,
        installNonce: Int
    ): ByteArray {
        require(payload4.size == 4) { "payload4 must be 4 bytes" }
        require(deviceHash6.size == 6) { "deviceHash6 must be 6 bytes" }

        val input = ByteArray(4 + 1 + 6 + 2)
        System.arraycopy(payload4, 0, input, 0, 4)
        input[4] = (fpType and 0xFF).toByte()
        System.arraycopy(deviceHash6, 0, input, 5, 6)
        input[11] = ((installNonce ushr 8) and 0xFF).toByte()
        input[12] = (installNonce and 0xFF).toByte()

        val keyBytes = assembleSigningKey()
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
            val full = mac.doFinal(input)
            full.copyOfRange(0, 6)
        } finally {
            keyBytes.fill(0)
        }
    }

    fun packPayload4Bytes(
        platform: Int,
        expiryDaysSince2025: Int,
        planTypeCode: Int,
        maxFolders: Int,
        allowExcel: Boolean,
        allowAppLock: Boolean,
        allowAccess: Boolean = true,
        reservedBit: Boolean = false
    ): ByteArray {
        val verBits = (LICENSE_VERSION_2 and 0x03) shl 30
        val platBits = (platform and 0x03) shl 28
        val expBits = (expiryDaysSince2025.coerceIn(0, MAX_EXPIRY_DAYS) and 0x3FFF) shl 14
        val planBits = (planTypeCode and 0x07) shl 11
        val folderBits = (maxFolders.coerceIn(0, 127) and 0x7F) shl 4
        val flagBits =
            ((if (allowExcel) 1 else 0) shl 3) or
                ((if (allowAppLock) 1 else 0) shl 2) or
                ((if (allowAccess) 1 else 0) shl 1) or
                (if (reservedBit) 1 else 0)

        val word = verBits or platBits or expBits or planBits or folderBits or flagBits
        return byteArrayOf(
            ((word ushr 24) and 0xFF).toByte(),
            ((word ushr 16) and 0xFF).toByte(),
            ((word ushr 8) and 0xFF).toByte(),
            (word and 0xFF).toByte()
        )
    }

    fun generateShortLicenseCode(
        platform: Int = PLATFORM_ANDROID,
        expiryDaysSince2025: Int,
        planTypeCode: Int,
        maxFolders: Int,
        allowExcel: Boolean,
        allowAppLock: Boolean,
        allowAccess: Boolean = true,
        fpType: Int,
        deviceHash6: ByteArray,
        installNonce: Int
    ): String {
        val payload4 = packPayload4Bytes(
            platform = platform,
            expiryDaysSince2025 = expiryDaysSince2025,
            planTypeCode = planTypeCode,
            maxFolders = maxFolders,
            allowExcel = allowExcel,
            allowAppLock = allowAppLock,
            allowAccess = allowAccess
        )
        val sig6 = computeHmacTruncated6(payload4, fpType, deviceHash6, installNonce)
        val combined = ByteArray(10)
        System.arraycopy(payload4, 0, combined, 0, 4)
        System.arraycopy(sig6, 0, combined, 4, 6)
        return formatGroupedCode16(encodeBase32Unambiguous(combined))
    }

    fun decodeAndVerifyShortLicenseCode(
        rawCode: String,
        expectedFpType: Int,
        expectedDeviceHash6: ByteArray,
        expectedInstallNonce: Int
    ): DecodedV2License? {
        return try {
            val combined = decodeBase32Unambiguous(rawCode)
            val payload4 = combined.copyOfRange(0, 4)
            val sig6 = combined.copyOfRange(4, 10)

            val word = ((payload4[0].toInt() and 0xFF) shl 24) or
                ((payload4[1].toInt() and 0xFF) shl 16) or
                ((payload4[2].toInt() and 0xFF) shl 8) or
                (payload4[3].toInt() and 0xFF)

            val version = (word ushr 30) and 0x03
            if (version != LICENSE_VERSION_2) return null

            val expectedSig6 = computeHmacTruncated6(
                payload4 = payload4,
                fpType = expectedFpType,
                deviceHash6 = expectedDeviceHash6,
                installNonce = expectedInstallNonce
            )
            if (!MessageDigest.isEqual(sig6, expectedSig6)) return null

            decodePayloadOnly(payload4, sig6)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeShortLicensePayloadWithoutVerify(rawCode: String): DecodedV2License? {
        return try {
            val combined = decodeBase32Unambiguous(rawCode)
            val payload4 = combined.copyOfRange(0, 4)
            val sig6 = combined.copyOfRange(4, 10)
            val word = ((payload4[0].toInt() and 0xFF) shl 24) or
                ((payload4[1].toInt() and 0xFF) shl 16) or
                ((payload4[2].toInt() and 0xFF) shl 8) or
                (payload4[3].toInt() and 0xFF)
            val version = (word ushr 30) and 0x03
            if (version != LICENSE_VERSION_2) return null
            decodePayloadOnly(payload4, sig6)
        } catch (_: Exception) {
            null
        }
    }

    private fun decodePayloadOnly(payload4: ByteArray, sig6: ByteArray): DecodedV2License {
        val word = ((payload4[0].toInt() and 0xFF) shl 24) or
            ((payload4[1].toInt() and 0xFF) shl 16) or
            ((payload4[2].toInt() and 0xFF) shl 8) or
            (payload4[3].toInt() and 0xFF)

        val version = (word ushr 30) and 0x03
        val platform = (word ushr 28) and 0x03
        val expiryDays = (word ushr 14) and 0x3FFF
        val planCode = (word ushr 11) and 0x07
        val maxFolders = (word ushr 4) and 0x7F
        val allowExcel = ((word ushr 3) and 0x01) == 1
        val allowAppLock = ((word ushr 2) and 0x01) == 1
        val allowAccess = ((word ushr 1) and 0x01) == 1
        val reservedBit = (word and 0x01) == 1

        return DecodedV2License(
            version = version,
            platform = platform,
            expiryDaysSince2025 = expiryDays,
            expEpochSeconds = daysSince2025ToExpSeconds(expiryDays),
            planTypeCode = planCode,
            planTypeName = decodePlanType(planCode),
            maxFolders = maxFolders,
            allowExcel = allowExcel,
            allowAppLock = allowAppLock,
            allowAccess = allowAccess,
            reservedBit = reservedBit,
            payloadBytes = payload4,
            signatureBytes = sig6
        )
    }
}
