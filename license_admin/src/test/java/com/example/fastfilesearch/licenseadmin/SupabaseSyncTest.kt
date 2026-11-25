package com.example.fastfilesearch.licenseadmin

import com.example.fastfilesearch.licenseadmin.data.PendingUploadEntity
import com.example.fastfilesearch.licenseadmin.engine.AdminLicenseGenerator
import com.example.fastfilesearch.licenseadmin.history.IssuedLicenseItem
import com.example.fastfilesearch.licenseadmin.sync.SupabaseUploadWorker
import com.example.fastfilesearch.licenseadmin.sync.UploadAttemptOutcome
import com.example.fastfilesearch.licenseadmin.ui.screens.HistoryStatusFilter
import com.example.fastfilesearch.licenseadmin.ui.screens.matchesHistoryFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

class SupabaseSyncTest {

    @Test
    fun testParseActivationRequestExtractsClientDeviceId() {
        val json = """{"id":"BA-1234-ABCD-5678","m":"Xiaomi","d":"Redmi Note 12","os":"Android 14 (API 34)","abi":"arm64-v8a","v":"5.0.0","client_device_id":"WV-00112233445566778899aabbccddeeff"}"""
        val b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray(StandardCharsets.UTF_8))
        val parsed = AdminLicenseGenerator.parseActivationRequest("طلب تفعيل:\nBAR1.$b64")

        assertEquals("BA-1234-ABCD-5678", parsed.installationId)
        assertEquals("WV-00112233445566778899aabbccddeeff", parsed.clientDeviceId)
        assertNotNull(parsed.deviceInfo)
        assertEquals("WV-00112233445566778899aabbccddeeff", parsed.deviceInfo?.clientDeviceId)
    }

    @Test
    fun testParseActivationRequestBackwardCompatibleWithoutClientDeviceId() {
        val legacyJson = """{"id":"BA-1111-2222-3333","m":"Samsung","d":"A52","os":"Android 13 (API 33)","abi":"arm64-v8a","v":"5.0.0"}"""
        val b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(legacyJson.toByteArray(StandardCharsets.UTF_8))
        val parsedLegacyBar1 = AdminLicenseGenerator.parseActivationRequest("BAR1.$b64")

        assertEquals("BA-1111-2222-3333", parsedLegacyBar1.installationId)
        assertNull(parsedLegacyBar1.clientDeviceId)
        assertNotNull(parsedLegacyBar1.deviceInfo)
        assertNull(parsedLegacyBar1.deviceInfo?.clientDeviceId)

        val parsedPlain = AdminLicenseGenerator.parseActivationRequest("BA-AAAA-BBBB-CCCC")
        assertEquals("BA-AAAA-BBBB-CCCC", parsedPlain.installationId)
        assertNull(parsedPlain.clientDeviceId)
    }

    @Test
    fun testEndpointNormalization() {
        assertEquals(
            "https://example.supabase.co/rest/v1/license_logs",
            SupabaseUploadWorker.buildLicenseLogsEndpoint("https://example.supabase.co")
        )
        assertEquals(
            "https://example.supabase.co/rest/v1/license_logs",
            SupabaseUploadWorker.buildLicenseLogsEndpoint("https://example.supabase.co/")
        )
        assertEquals(
            "https://example.supabase.co/rest/v1/license_logs",
            SupabaseUploadWorker.buildLicenseLogsEndpoint("https://example.supabase.co/rest/v1/")
        )
        assertEquals(
            "https://example.supabase.co/rest/v1/license_logs",
            SupabaseUploadWorker.buildLicenseLogsEndpoint("https://example.supabase.co/rest/v1")
        )
    }

    @Test
    fun testRequestHeadersPublishableVsJwtKey() {
        val publishableHeaders = SupabaseUploadWorker.buildRequestHeaders("sb_publishable_abc123")
        assertEquals("sb_publishable_abc123", publishableHeaders["apikey"])
        assertEquals("application/json; charset=utf-8", publishableHeaders["Content-Type"])
        assertEquals("return=minimal", publishableHeaders["Prefer"])
        assertFalse(publishableHeaders.containsKey("Authorization"))

        val jwtHeaders = SupabaseUploadWorker.buildRequestHeaders("eyJhbGciOiJIUzI1NiJ9.payload.sig")
        assertEquals("eyJhbGciOiJIUzI1NiJ9.payload.sig", jwtHeaders["apikey"])
        assertEquals("return=minimal", jwtHeaders["Prefer"])
        assertEquals("Bearer eyJhbGciOiJIUzI1NiJ9.payload.sig", jwtHeaders["Authorization"])
    }

    @Test
    fun testToSupabaseJsonMatchesExactSchemaAndOmitsLocalColumns() {
        val item = PendingUploadEntity(
            localId = 42L,
            issuedAt = "2026-11-24T21:30:00Z",
            issuerId = "abc123issuer",
            issuerDevice = "Samsung SM-S918B",
            customerName = null,
            installationId = "BA-2345-6789-ABCD",
            clientDeviceId = "AD-0123456789abcdef0123456789abcdef",
            clientManufacturer = "Xiaomi",
            clientModel = "Poco X5",
            clientOs = "Android 14 (API 34)",
            clientAbi = "arm64-v8a",
            clientAppVersion = "5.0.0",
            planType = "lifetime",
            durationDays = 0,
            expiresAt = null,
            maxFolders = 0,
            allowExcel = true,
            allowAppLock = false,
            licenseCode = "BA1.payload.sig",
            attempts = 2
        )

        val json = SupabaseUploadWorker.toSupabaseJson(item)

        assertTrue(json.contains("\"issued_at\":\"2026-11-24T21:30:00Z\""))
        assertTrue(json.contains("\"issuer_id\":\"abc123issuer\""))
        assertTrue(json.contains("\"issuer_device\":\"Samsung SM-S918B\""))
        assertTrue(json.contains("\"customer_name\":null"))
        assertTrue(json.contains("\"installation_id\":\"BA-2345-6789-ABCD\""))
        assertTrue(json.contains("\"client_device_id\":\"AD-0123456789abcdef0123456789abcdef\""))
        assertTrue(json.contains("\"client_manufacturer\":\"Xiaomi\""))
        assertTrue(json.contains("\"client_model\":\"Poco X5\""))
        assertTrue(json.contains("\"client_os\":\"Android 14 (API 34)\""))
        assertTrue(json.contains("\"client_abi\":\"arm64-v8a\""))
        assertTrue(json.contains("\"client_app_version\":\"5.0.0\""))
        assertTrue(json.contains("\"plan_type\":\"lifetime\""))
        assertTrue(json.contains("\"duration_days\":0"))
        assertTrue(json.contains("\"expires_at\":null"))
        assertTrue(json.contains("\"max_folders\":0"))
        assertTrue(json.contains("\"allow_excel\":true"))
        assertTrue(json.contains("\"allow_app_lock\":false"))
        assertTrue(json.contains("\"license_code\":\"BA1.payload.sig\""))

        assertFalse(json.contains("localId"))
        assertFalse(json.contains("attempts"))
    }

    @Test
    fun testHttpStatusClassification() {
        assertEquals(UploadAttemptOutcome.Success, SupabaseUploadWorker.classifyHttpStatus(200))
        assertEquals(UploadAttemptOutcome.Success, SupabaseUploadWorker.classifyHttpStatus(201))
        assertEquals(UploadAttemptOutcome.Success, SupabaseUploadWorker.classifyHttpStatus(204))
        assertTrue(SupabaseUploadWorker.classifyHttpStatus(400) is UploadAttemptOutcome.PermanentClientError)
        assertTrue(SupabaseUploadWorker.classifyHttpStatus(401) is UploadAttemptOutcome.PermanentClientError)
        assertTrue(SupabaseUploadWorker.classifyHttpStatus(409) is UploadAttemptOutcome.PermanentClientError)
        assertTrue(SupabaseUploadWorker.classifyHttpStatus(500) is UploadAttemptOutcome.TransientFailure)
        assertTrue(SupabaseUploadWorker.classifyHttpStatus(503) is UploadAttemptOutcome.TransientFailure)
    }

    @Test
    fun testHistoryStatusFiltering() {
        val now = 1_700_000_000L
        val base = IssuedLicenseItem(
            installationId = "BA-1111-2222-3333",
            clientName = "عميل",
            iat = now - 86_400L,
            exp = 0L,
            maxWorkspaces = 0,
            excelExport = true,
            appLock = true,
            bgSync = true,
            licenseCode = "BA1.p.s"
        )

        val lifetime = base.copy(exp = 0L)
        val expiringSoon = base.copy(exp = now + 3L * 86_400L)
        val activeLong = base.copy(exp = now + 30L * 86_400L)
        val expired = base.copy(exp = now - 10L)

        assertTrue(matchesHistoryFilter(lifetime, HistoryStatusFilter.ACTIVE, now))
        assertFalse(matchesHistoryFilter(lifetime, HistoryStatusFilter.EXPIRING_SOON, now))
        assertFalse(matchesHistoryFilter(lifetime, HistoryStatusFilter.EXPIRED, now))

        assertTrue(matchesHistoryFilter(expiringSoon, HistoryStatusFilter.ACTIVE, now))
        assertTrue(matchesHistoryFilter(expiringSoon, HistoryStatusFilter.EXPIRING_SOON, now))
        assertFalse(matchesHistoryFilter(expiringSoon, HistoryStatusFilter.EXPIRED, now))

        assertTrue(matchesHistoryFilter(activeLong, HistoryStatusFilter.ACTIVE, now))
        assertFalse(matchesHistoryFilter(activeLong, HistoryStatusFilter.EXPIRING_SOON, now))

        assertFalse(matchesHistoryFilter(expired, HistoryStatusFilter.ACTIVE, now))
        assertFalse(matchesHistoryFilter(expired, HistoryStatusFilter.EXPIRING_SOON, now))
        assertTrue(matchesHistoryFilter(expired, HistoryStatusFilter.EXPIRED, now))
    }
}
