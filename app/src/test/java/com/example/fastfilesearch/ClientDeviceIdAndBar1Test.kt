package com.example.fastfilesearch

import com.example.fastfilesearch.util.ClientDeviceIdProvider
import com.example.fastfilesearch.util.DeviceInfoHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

class ClientDeviceIdAndBar1Test {

    @Test
    fun testWidevineFormattingProduces32HexCharsWithWvPrefix() {
        val sampleDrmId = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val formatted = ClientDeviceIdProvider.formatWidevineId(sampleDrmId)
        assertTrue(formatted.startsWith("WV-"))
        assertEquals(3 + 32, formatted.length)
        assertTrue(formatted.substring(3).all { it in '0'..'9' || it in 'a'..'f' })

        val expectedDigest = MessageDigest.getInstance("SHA-256").digest(sampleDrmId)
        val expectedHex = expectedDigest.take(16).joinToString("") { "%02x".format(it) }
        assertEquals("WV-$expectedHex", formatted)
    }

    @Test
    fun testAndroidIdFormattingProduces32HexCharsWithAdPrefix() {
        val sampleAndroidId = "9774d56d682e549c"
        val formatted = ClientDeviceIdProvider.formatAndroidId(sampleAndroidId)
        assertTrue(formatted.startsWith("AD-"))
        assertEquals(3 + 32, formatted.length)
        assertTrue(formatted.substring(3).all { it in '0'..'9' || it in 'a'..'f' })

        val expectedDigest = MessageDigest.getInstance("SHA-256")
            .digest(sampleAndroidId.toByteArray(StandardCharsets.UTF_8))
        val expectedHex = expectedDigest.take(16).joinToString("") { "%02x".format(it) }
        assertEquals("AD-$expectedHex", formatted)
    }

    @Test
    fun testBar1EncodingIncludesClientDeviceId() {
        val clientDeviceId = "WV-0123456789abcdef0123456789abcdef"
        val bar1 = DeviceInfoHelper.encodeActivationRequest(
            installationId = "BA-ABCD-2345-WXYZ",
            manufacturer = "Samsung",
            model = "SM-A525F",
            osVersion = "Android 13 (API 33)",
            abi = "arm64-v8a",
            appVersion = "5.0.0",
            clientDeviceId = clientDeviceId
        )

        assertTrue(bar1.startsWith("BAR1."))
        val payloadB64 = bar1.removePrefix("BAR1.")
        val json = String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8)

        assertTrue(json.contains("\"id\":\"BA-ABCD-2345-WXYZ\""))
        assertTrue(json.contains("\"m\":\"Samsung\""))
        assertTrue(json.contains("\"d\":\"SM-A525F\""))
        assertTrue(json.contains("\"client_device_id\":\"$clientDeviceId\""))
    }
}
