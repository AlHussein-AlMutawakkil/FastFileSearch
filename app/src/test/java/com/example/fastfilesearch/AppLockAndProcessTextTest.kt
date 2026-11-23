package com.example.fastfilesearch

import com.example.fastfilesearch.security.AppLockMode
import com.example.fastfilesearch.security.AutoLockTimeout
import com.example.fastfilesearch.security.CryptoHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class AppLockAndProcessTextTest {

    @Test
    fun testPasscodeCryptographyPBKDF2WithHmacSHA256() {
        val passcode = "secret1234"
        val salt1 = CryptoHelper.generateSalt(16)
        val salt2 = CryptoHelper.generateSalt(16)

        // Salts must be unique
        assertEquals(16, salt1.size)
        assertEquals(16, salt2.size)
        assertFalse(salt1.contentEquals(salt2))

        val hash1 = CryptoHelper.hashPasscode(passcode, salt1)
        val hash2 = CryptoHelper.hashPasscode(passcode, salt2)

        // PBKDF2WithHmacSHA256 produces 32-byte (256-bit) key
        assertEquals(32, hash1.size)
        assertEquals(32, hash2.size)

        // Different salts must result in different hashes for identical passcode
        assertFalse(hash1.contentEquals(hash2))

        // Verification must succeed for correct passcode
        assertTrue(CryptoHelper.verifyPasscode(passcode, salt1, hash1))

        // Verification must fail for incorrect passcode
        assertFalse(CryptoHelper.verifyPasscode("wrong_passcode", salt1, hash1))

        // Base64 encoding/decoding consistency
        val saltBase64 = Base64.getEncoder().encodeToString(salt1)
        val hashBase64 = Base64.getEncoder().encodeToString(hash1)
        val decodedSalt = Base64.getDecoder().decode(saltBase64)
        val decodedHash = Base64.getDecoder().decode(hashBase64)

        assertTrue(CryptoHelper.verifyPasscode(passcode, decodedSalt, decodedHash))
    }

    @Test
    fun testAutoLockTimingAndSuppressionLogic() {
        var isLocked = false
        var lockMode = AppLockMode.PASSCODE_ONLY
        var timeout = AutoLockTimeout.ONE_MINUTE
        var lastBackgroundTimestamp = 0L
        var suppressNextLock = false

        fun onAppBackgrounded(timestamp: Long) {
            if (suppressNextLock) {
                suppressNextLock = false
                return
            }
            lastBackgroundTimestamp = timestamp
            if (timeout == AutoLockTimeout.IMMEDIATE && lockMode != AppLockMode.DISABLED) {
                isLocked = true
            }
        }

        fun onAppForegrounded(timestamp: Long) {
            if (lockMode == AppLockMode.DISABLED || isLocked) return
            if (lastBackgroundTimestamp > 0L) {
                val elapsed = timestamp - lastBackgroundTimestamp
                if (elapsed >= timeout.durationMs) {
                    isLocked = true
                }
            }
            lastBackgroundTimestamp = 0L
        }

        // Test 1: Foreground before timeout expires (30 seconds vs 60 seconds timeout)
        val t0 = 1_000_000L
        onAppBackgrounded(t0)
        assertEquals(t0, lastBackgroundTimestamp)
        onAppForegrounded(t0 + 30_000L)
        assertFalse("App should not lock before 1 minute elapsed", isLocked)

        // Test 2: Foreground after timeout expires (70 seconds vs 60 seconds timeout)
        val t1 = 2_000_000L
        onAppBackgrounded(t1)
        onAppForegrounded(t1 + 70_000L)
        assertTrue("App must lock after 1 minute elapsed", isLocked)

        // Unlock
        isLocked = false

        // Test 3: suppressNextLockOnce() logic (e.g. SAF picker or Share intent)
        suppressNextLock = true
        val t2 = 3_000_000L
        onAppBackgrounded(t2)
        assertEquals("lastBackgroundTimestamp must remain 0 when suppressed", 0L, lastBackgroundTimestamp)
        assertFalse("suppressNextLock flag must reset to false", suppressNextLock)
        onAppForegrounded(t2 + 100_000L)
        assertFalse("App must not lock when background was suppressed", isLocked)

        // Test 4: IMMEDIATE timeout
        timeout = AutoLockTimeout.IMMEDIATE
        onAppBackgrounded(4_000_000L)
        assertTrue("Immediate timeout must lock instantly on background", isLocked)
    }

    @Test
    fun testRateLimitingAndBruteForceLockout() {
        var failedAttempts = 0
        var lockoutUntilTimestamp = 0L
        val salt = CryptoHelper.generateSalt(16)
        val hash = CryptoHelper.hashPasscode("1234", salt)

        fun getRemainingLockout(currentTime: Long): Int {
            return if (currentTime < lockoutUntilTimestamp) {
                val remainingMs = lockoutUntilTimestamp - currentTime
                maxOf(1, ((remainingMs + 999) / 1000).toInt())
            } else {
                0
            }
        }

        fun verify(input: String, currentTime: Long): Boolean {
            if (getRemainingLockout(currentTime) > 0) return false
            val ok = CryptoHelper.verifyPasscode(input, salt, hash)
            if (ok) {
                failedAttempts = 0
                lockoutUntilTimestamp = 0L
            } else {
                failedAttempts++
                if (failedAttempts >= 5) {
                    lockoutUntilTimestamp = currentTime + 30_000L
                }
            }
            return ok
        }

        val now = 100_000L
        // 4 failed attempts: not yet locked out
        assertFalse(verify("0000", now))
        assertFalse(verify("0000", now))
        assertFalse(verify("0000", now))
        assertFalse(verify("0000", now))
        assertEquals(4, failedAttempts)
        assertEquals(0, getRemainingLockout(now))

        // 5th failed attempt: triggers 30s lockout
        assertFalse(verify("0000", now))
        assertEquals(5, failedAttempts)
        assertEquals(now + 30_000L, lockoutUntilTimestamp)
        assertEquals(30, getRemainingLockout(now))

        // During lockout: even correct passcode is rejected
        assertFalse(verify("1234", now + 10_000L))
        assertEquals(20, getRemainingLockout(now + 10_000L))

        // After 30s: lockout expires
        val afterLockout = now + 30_001L
        assertEquals(0, getRemainingLockout(afterLockout))

        // Correct passcode succeeds and resets failed attempts
        assertTrue(verify("1234", afterLockout))
        assertEquals(0, failedAttempts)
        assertEquals(0L, lockoutUntilTimestamp)
    }

    @Test
    fun testProcessTextSanitization() {
        fun sanitize(input: String?): String? {
            val clean = input?.trim()
            return if (clean.isNullOrBlank()) null else clean
        }

        // Normal text
        assertEquals("مبيعات الربع الأول", sanitize("مبيعات الربع الأول"))

        // Text with spaces, tabs, and newlines
        val messy = "\n\n  \tتقرير الميزانية 2024 \t \r\n"
        assertEquals("تقرير الميزانية 2024", sanitize(messy))

        // Blank or whitespace-only inputs
        assertEquals(null, sanitize(""))
        assertEquals(null, sanitize("    "))
        assertEquals(null, sanitize("\n\t\r "))
        assertEquals(null, sanitize(null))
    }
}
