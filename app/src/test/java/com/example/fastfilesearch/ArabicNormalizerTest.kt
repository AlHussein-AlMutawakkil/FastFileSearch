package com.example.fastfilesearch

import com.example.fastfilesearch.util.ArabicNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicNormalizerTest {

    @Test
    fun testAlifNormalization() {
        assertEquals("احمد", ArabicNormalizer.normalize("أحمد"))
        assertEquals("احمد", ArabicNormalizer.normalize("إحمد"))
        assertEquals("اثار", ArabicNormalizer.normalize("آثار"))
        assertEquals("اسم", ArabicNormalizer.normalize("ٱسم"))
    }

    @Test
    fun testAlefMaksuraAndTaMarbuta() {
        assertEquals("علي", ArabicNormalizer.normalize("على"))
        assertEquals("شركه", ArabicNormalizer.normalize("شركة"))
        assertEquals("مدينه", ArabicNormalizer.normalize("مدينة"))
    }

    @Test
    fun testDiacriticsAndTatweelRemoval() {
        val withTashkeel = "مُحَمَّدٌ"
        assertEquals("محمد", ArabicNormalizer.normalize(withTashkeel))

        val withTatweel = "تـــــقــــريـــــر"
        assertEquals("تقرير", ArabicNormalizer.normalize(withTatweel))

        val mixed = "شَــــرِكَةُ أَعْمَالٍ"
        assertEquals("شركه اعمال", ArabicNormalizer.normalize(mixed))
    }

    @Test
    fun testPersianVariants() {
        assertEquals("بانك", ArabicNormalizer.normalize("بانک"))
        assertEquals("علي", ArabicNormalizer.normalize("على"))
    }

    @Test
    fun testNumeralNormalization() {
        assertEquals("0123456789", ArabicNormalizer.normalize("٠١٢٣٤٥٦٧٨٩"))
        assertEquals("0123456789", ArabicNormalizer.normalize("۰۱۲۳۴۵۶۷۸۹"))
        assertEquals("فاتوره رقم 2024", ArabicNormalizer.normalize("فاتورة رقم ٢٠٢٤"))
        assertEquals("حساب 12345", ArabicNormalizer.normalize("حساب ۱۲۳۴۵"))
    }
}
