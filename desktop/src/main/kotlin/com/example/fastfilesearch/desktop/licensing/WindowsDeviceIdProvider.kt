package com.example.fastfilesearch.desktop.licensing

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class WindowsDeviceInfo(
    val digestPrefix6: ByteArray,
    val clientDeviceId: String,
    val manufacturer: String,
    val model: String,
    val osVersionNumber: Int,
    val osDisplayName: String,
    val abiCode: Int,
    val abiDisplay: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WindowsDeviceInfo) return false
        return digestPrefix6.contentEquals(other.digestPrefix6) &&
            clientDeviceId == other.clientDeviceId &&
            manufacturer == other.manufacturer &&
            model == other.model &&
            osVersionNumber == other.osVersionNumber &&
            osDisplayName == other.osDisplayName &&
            abiCode == other.abiCode &&
            abiDisplay == other.abiDisplay
    }

    override fun hashCode(): Int {
        var result = digestPrefix6.contentHashCode()
        result = 31 * result + clientDeviceId.hashCode()
        result = 31 * result + manufacturer.hashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + osVersionNumber
        result = 31 * result + osDisplayName.hashCode()
        result = 31 * result + abiCode
        result = 31 * result + abiDisplay.hashCode()
        return result
    }
}

/**
 * Computes a persistent offline Windows hardware fingerprint and device metadata
 * combining Motherboard/Product UUID (Win32_ComputerSystemProduct) and Windows Cryptography MachineGuid.
 */
object WindowsDeviceIdProvider {

    const val DESKTOP_APP_VERSION = "5.0.0"

    @Volatile
    private var cachedDeviceInfo: WindowsDeviceInfo? = null

    fun getDeviceInfo(): WindowsDeviceInfo {
        cachedDeviceInfo?.let { return it }
        val computed = collectDeviceInfo()
        cachedDeviceInfo = computed
        return computed
    }

    private fun collectDeviceInfo(): WindowsDeviceInfo {
        val cimFields = queryCimFields()
        val rawUuid = cimFields?.getOrNull(0)?.takeIf { isValidHardwareToken(it) }
        val rawMfg = cimFields?.getOrNull(1)?.takeIf { isValidHardwareToken(it) }
        val rawModel = cimFields?.getOrNull(2)?.takeIf { isValidHardwareToken(it) }
        val rawOsCaption = cimFields?.getOrNull(3).orEmpty()
        val rawBuildNumber = cimFields?.getOrNull(4).orEmpty()

        val machineGuid = queryRegistryMachineGuid()?.takeIf { isValidHardwareToken(it) }

        val sources = listOfNotNull(rawUuid, machineGuid)
        val combinedSeed = if (sources.isNotEmpty()) {
            sources.joinToString("|")
        } else {
            buildFallbackSeed()
        }

        val digest16 = sha256First16Bytes(combinedSeed.toByteArray(StandardCharsets.UTF_8))
        val prefix6 = digest16.copyOfRange(0, 6)
        val clientDeviceId = "WIN-${V2LicenseCodec.bytesToHexLower(prefix6)}"

        val osNumber = resolveWindowsMajorNumber(
            caption = rawOsCaption,
            buildNumberStr = rawBuildNumber,
            sysOsName = System.getProperty("os.name").orEmpty(),
            sysOsVersion = System.getProperty("os.version").orEmpty()
        )
        val arch = System.getProperty("os.arch") ?: "x86_64"
        val abiCode = V2LicenseCodec.encodeAbiCode(arch)

        return WindowsDeviceInfo(
            digestPrefix6 = prefix6,
            clientDeviceId = clientDeviceId,
            manufacturer = rawMfg ?: "Microsoft",
            model = rawModel ?: (System.getenv("COMPUTERNAME")?.takeIf { it.isNotBlank() } ?: "Windows PC"),
            osVersionNumber = osNumber,
            osDisplayName = "Windows $osNumber",
            abiCode = abiCode,
            abiDisplay = V2LicenseCodec.decodeAbiCode(abiCode)
        )
    }

    internal fun formatWindowsFingerprintPrefix6(seed: String): ByteArray {
        return sha256First16Bytes(seed.toByteArray(StandardCharsets.UTF_8)).copyOfRange(0, 6)
    }

    internal fun sha256First16Bytes(input: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256").digest(input)
        return digest.copyOfRange(0, 16)
    }

    internal fun resolveWindowsMajorNumber(
        caption: String,
        buildNumberStr: String,
        sysOsName: String,
        sysOsVersion: String
    ): Int {
        val buildNum = buildNumberStr.trim().toIntOrNull() ?: 0
        if (caption.contains("11") || sysOsName.contains("11") || buildNum >= 22000) {
            return 11
        }
        if (caption.contains("10") || sysOsName.contains("10") || buildNum >= 10240) {
            return 10
        }
        if (caption.contains("Server", ignoreCase = true)) {
            return if (buildNum >= 20348) 22 else 19
        }
        val sysMajor = sysOsVersion.substringBefore(".").toIntOrNull()
        return sysMajor?.coerceIn(7, 99) ?: 10
    }

    private fun isValidHardwareToken(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val cleaned = value.trim()
        val lower = cleaned.lowercase()
        return lower != "null" &&
            lower != "none" &&
            lower != "default string" &&
            lower != "to be filled by o.e.m." &&
            lower != "system product name" &&
            lower != "system manufacturer" &&
            cleaned != "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF" &&
            cleaned != "00000000-0000-0000-0000-000000000000"
    }

    private fun buildFallbackSeed(): String {
        val comp = System.getenv("COMPUTERNAME") ?: ""
        val osName = System.getProperty("os.name") ?: ""
        val osArch = System.getProperty("os.arch") ?: ""
        val userName = System.getProperty("user.name") ?: ""
        return "WIN-FALLBACK|$comp|$osName|$osArch|$userName"
    }

    private fun queryCimFields(): List<String>? {
        val script = $$"""
            $csp = Get-CimInstance -ClassName Win32_ComputerSystemProduct -ErrorAction SilentlyContinue
            $cs = Get-CimInstance -ClassName Win32_ComputerSystem -ErrorAction SilentlyContinue
            $os = Get-CimInstance -ClassName Win32_OperatingSystem -ErrorAction SilentlyContinue
            "$($csp.UUID);;$($cs.Manufacturer);;$($cs.Model);;$($os.Caption);;$($os.BuildNumber)"
        """.trimIndent().replace("\r\n", "; ")

        val output = runCommandQuietly(
            listOf("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script)
        ) ?: return null

        val lastNonEmpty = output.lineSequence()
            .map { it.trim() }
            .lastOrNull { it.contains(";;") }
            ?: return null

        return lastNonEmpty.split(";;")
    }

    private fun queryRegistryMachineGuid(): String? {
        val output = runCommandQuietly(
            listOf("reg.exe", "query", "HKLM\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid")
        ) ?: return null

        for (line in output.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.contains("MachineGuid", ignoreCase = true) && trimmed.contains("REG_SZ")) {
                val parts = trimmed.split("\\s+".toRegex())
                return parts.lastOrNull()?.trim()
            }
        }
        return null
    }

    private fun runCommandQuietly(command: List<String>, timeoutSeconds: Long = 4L): String? {
        var process: Process? = null
        return try {
            process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return null
            }
            process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (_: Throwable) {
            null
        } finally {
            try {
                process?.destroy()
            } catch (_: Throwable) {
            }
        }
    }
}
