package com.example.fastfilesearch

import com.example.fastfilesearch.util.ValueInteractionHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValueInteractionHelperTest {

    @Test
    fun testExtractPhoneNumbers_variousFormats() {
        val text = "اتصل على 0501234567 أو عبر الرقم الدولي +966501234567 أو الخط الأرضي 01-2345678"
        val phones = ValueInteractionHelper.extractPhoneNumbers(text)

        assertTrue(phones.any { it.contains("0501234567") })
        assertTrue(phones.any { it.contains("+966501234567") })
        assertTrue(phones.any { it.contains("01-2345678") })
    }

    @Test
    fun testExtractPhoneNumbers_ignoresShortNumbersAndPrices() {
        val text = "الكمية 50 والسعر 250 ريال، التاريخ 2024"
        val phones = ValueInteractionHelper.extractPhoneNumbers(text)

        assertTrue(phones.isEmpty())
    }

    @Test
    fun testExtractUrls() {
        val text = "يرجى زيارة موقعنا https://example.com/portal ومستند الدليل http://docs.google.com/doc1"
        val urls = ValueInteractionHelper.extractUrls(text)

        assertEquals(2, urls.size)
        assertEquals("https://example.com/portal", urls[0])
        assertEquals("http://docs.google.com/doc1", urls[1])
    }

    @Test
    fun testExtractEmails() {
        val text = "للتواصل ارسل إلى info@company.org أو support@sub.domain.com شكراً"
        val emails = ValueInteractionHelper.extractEmails(text)

        assertEquals(2, emails.size)
        assertEquals("info@company.org", emails[0])
        assertEquals("support@sub.domain.com", emails[1])
    }

    @Test
    fun testFormatRow_verticalFormat() {
        val cells = listOf("محمد أحمد", "0501234567", "الرياض")
        val headers = listOf("الاسم", "الجوال", "المدينة")

        val formatted = ValueInteractionHelper.formatRow(cells, headers, vertical = true)
        val expected = "الاسم: محمد أحمد\nالجوال: 0501234567\nالمدينة: الرياض"
        assertEquals(expected, formatted)
    }

    @Test
    fun testFormatRow_horizontalFormat() {
        val cells = listOf("محمد أحمد", "0501234567", "الرياض")
        val headers = listOf("الاسم", "الجوال", "المدينة")

        val formatted = ValueInteractionHelper.formatRow(cells, headers, vertical = false)
        val expected = "الاسم: محمد أحمد - الجوال: 0501234567 - المدينة: الرياض"
        assertEquals(expected, formatted)
    }

    @Test
    fun testFormatRow_missingHeaderFallback() {
        val cells = listOf("قيمة 1", "قيمة 2")
        val headers = listOf("الرئيسي")

        val formatted = ValueInteractionHelper.formatRow(cells, headers, vertical = true)
        val expected = "الرئيسي: قيمة 1\nعمود 2: قيمة 2"
        assertEquals(expected, formatted)
    }
}
