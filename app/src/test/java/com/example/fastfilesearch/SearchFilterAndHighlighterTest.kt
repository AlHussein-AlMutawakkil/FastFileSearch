package com.example.fastfilesearch

import com.example.fastfilesearch.model.SearchFilter
import com.example.fastfilesearch.model.SearchMethod
import com.example.fastfilesearch.model.SupportedFileTypeGroups
import com.example.fastfilesearch.ui.TextHighlighter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchFilterAndHighlighterTest {

    @Test
    fun testDefaultSearchFilterIsNotActive() {
        val filter = SearchFilter()
        assertEquals(SearchMethod.CONTAINS, filter.method)
        assertTrue(filter.ignoreDiacritics)
        assertTrue(filter.selectedExtensions.isEmpty())
        assertTrue(filter.selectedFileUris.isEmpty())
        assertFalse(filter.isFilterActive)
    }

    @Test
    fun testFilterBecomesActiveOnCustomizations() {
        assertFalse(SearchFilter().isFilterActive)
        assertTrue(SearchFilter(method = SearchMethod.EXACT_PHRASE).isFilterActive)
        assertTrue(SearchFilter(ignoreDiacritics = false).isFilterActive)
        assertTrue(SearchFilter(selectedExtensions = setOf("xlsx")).isFilterActive)
        assertTrue(SearchFilter(selectedFileUris = setOf("content://uri/1")).isFilterActive)
    }

    @Test
    fun testDefaultSearchMethodParsingAndFilterActiveCheck() {
        assertEquals(SearchMethod.CONTAINS, SearchMethod.fromStoredName(null))
        assertEquals(SearchMethod.CONTAINS, SearchMethod.fromStoredName(""))
        assertEquals(SearchMethod.CONTAINS, SearchMethod.fromStoredName("UNKNOWN"))
        assertEquals(SearchMethod.ALL_WORDS, SearchMethod.fromStoredName("ALL_WORDS"))
        assertEquals(SearchMethod.EXACT_PHRASE, SearchMethod.fromStoredName("EXACT_PHRASE"))
        assertEquals(SearchMethod.WHOLE_WORD, SearchMethod.fromStoredName("WHOLE_WORD"))

        val filterUsingUserDefault = SearchFilter(method = SearchMethod.WHOLE_WORD)
        assertFalse(filterUsingUserDefault.isFilterActive(SearchMethod.WHOLE_WORD))
        assertTrue(filterUsingUserDefault.isFilterActive(SearchMethod.CONTAINS))
        assertTrue(SearchFilter(method = SearchMethod.EXACT_PHRASE).isFilterActive(SearchMethod.WHOLE_WORD))
    }

    @Test
    fun testSupportedFileTypeGroups() {
        val excelGroup = SupportedFileTypeGroups.EXCEL
        assertTrue(excelGroup.extensions.contains("xlsx"))
        assertTrue(excelGroup.extensions.contains("xls"))

        val wordGroup = SupportedFileTypeGroups.WORD
        assertTrue(wordGroup.extensions.contains("docx"))
        assertTrue(wordGroup.extensions.contains("doc"))

        val pdfGroup = SupportedFileTypeGroups.PDF
        assertTrue(pdfGroup.extensions.contains("pdf"))
    }

    @Test
    fun testHighlighterContainsMatchesMiddleOfWord() {
        val text = "قام المهندس محمد بمراجعة المخطط"
        val query = "حمد"

        val annotated = TextHighlighter.highlight(
            text = text,
            query = query,
            method = SearchMethod.CONTAINS,
            ignoreDiacritics = true
        )

        // span styles must be applied because "حمد" is inside "محمد"
        val spans = annotated.spanStyles
        assertTrue("Should highlight 'حمد' inside 'محمد' with CONTAINS", spans.isNotEmpty())
    }

    @Test
    fun testHighlighterExactPhrase() {
        val text = "ذهب محمد بن علي إلى السوق"

        val matchAnnotated = TextHighlighter.highlight(
            text = text,
            query = "محمد بن علي",
            method = SearchMethod.EXACT_PHRASE,
            ignoreDiacritics = true
        )
        assertTrue("Should highlight exact phrase 'محمد بن علي'", matchAnnotated.spanStyles.isNotEmpty())

        val nonMatchAnnotated = TextHighlighter.highlight(
            text = text,
            query = "علي بن محمد",
            method = SearchMethod.EXACT_PHRASE,
            ignoreDiacritics = true
        )
        assertTrue("Should not highlight reversed phrase 'علي بن محمد'", nonMatchAnnotated.spanStyles.isEmpty())
    }

    @Test
    fun testHighlighterWholeWord() {
        val text = "المعلمون في المدرسة ثم المعلم في الصف"

        val annotated = TextHighlighter.highlight(
            text = text,
            query = "المعلم",
            method = SearchMethod.WHOLE_WORD,
            ignoreDiacritics = true
        )

        val spans = annotated.spanStyles
        assertEquals("Should match only standalone 'المعلم', not 'المعلمون'", 1, spans.size)
    }

    @Test
    fun testHighlighterDiacriticsStrictMatching() {
        val text = "قام إبراهيم بالعمل"

        // When ignoreDiacritics is false, "ابراهيم" (without kasra hamza) should NOT match "إبراهيم"
        val strictNonMatch = TextHighlighter.highlight(
            text = text,
            query = "ابراهيم",
            method = SearchMethod.CONTAINS,
            ignoreDiacritics = false
        )
        assertTrue("Should not match when hamza differs and ignoreDiacritics is false", strictNonMatch.spanStyles.isEmpty())

        // Exact hamza match
        val strictMatch = TextHighlighter.highlight(
            text = text,
            query = "إبراهيم",
            method = SearchMethod.CONTAINS,
            ignoreDiacritics = false
        )
        assertTrue("Should match when exact hamza matches", strictMatch.spanStyles.isNotEmpty())
    }
}
