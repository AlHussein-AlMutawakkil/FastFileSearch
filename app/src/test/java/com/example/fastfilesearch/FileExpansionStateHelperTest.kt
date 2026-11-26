package com.example.fastfilesearch

import com.example.fastfilesearch.ui.FileExpansionStateHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileExpansionStateHelperTest {

    private val visibleFiles = listOf("uri://file1", "uri://file2", "uri://file3")

    @Test
    fun testDefaultStateAllExpandedShowsCollapseAllMode() {
        val collapsed = emptySet<String>()
        assertTrue(FileExpansionStateHelper.hasAnyExpanded(visibleFiles, collapsed))
        visibleFiles.forEach { uri ->
            assertTrue(FileExpansionStateHelper.isExpanded(uri, collapsed))
        }
    }

    @Test
    fun testToggleAllCollapsesAllAndThenExpandsAll() {
        var collapsed = emptySet<String>()

        // 1. First toggle: at least one expanded -> collapses all
        collapsed = FileExpansionStateHelper.toggleAll(visibleFiles, collapsed)
        assertFalse(FileExpansionStateHelper.hasAnyExpanded(visibleFiles, collapsed))
        visibleFiles.forEach { uri ->
            assertFalse(FileExpansionStateHelper.isExpanded(uri, collapsed))
        }

        // 2. Second toggle: all collapsed -> expands all
        collapsed = FileExpansionStateHelper.toggleAll(visibleFiles, collapsed)
        assertTrue(FileExpansionStateHelper.hasAnyExpanded(visibleFiles, collapsed))
        visibleFiles.forEach { uri ->
            assertTrue(FileExpansionStateHelper.isExpanded(uri, collapsed))
        }
    }

    @Test
    fun testIndividualManualToggleSynchronizesMainButtonState() {
        // Collapse all
        var collapsed = FileExpansionStateHelper.toggleAll(visibleFiles, emptySet())
        assertFalse(FileExpansionStateHelper.hasAnyExpanded(visibleFiles, collapsed))

        // Manually expand one file -> main button must flip back to "Collapse All" (hasAnyExpanded == true)
        collapsed = FileExpansionStateHelper.toggleFile("uri://file2", collapsed)
        assertTrue(FileExpansionStateHelper.isExpanded("uri://file2", collapsed))
        assertFalse(FileExpansionStateHelper.isExpanded("uri://file1", collapsed))
        assertTrue(FileExpansionStateHelper.hasAnyExpanded(visibleFiles, collapsed))

        // Global toggle now collapses everything including file2
        collapsed = FileExpansionStateHelper.toggleAll(visibleFiles, collapsed)
        assertFalse(FileExpansionStateHelper.hasAnyExpanded(visibleFiles, collapsed))
        assertFalse(FileExpansionStateHelper.isExpanded("uri://file2", collapsed))

        // Expand all -> manually collapse each individually until none remain expanded
        collapsed = FileExpansionStateHelper.toggleAll(visibleFiles, collapsed)
        assertTrue(FileExpansionStateHelper.hasAnyExpanded(visibleFiles, collapsed))

        collapsed = FileExpansionStateHelper.toggleFile("uri://file1", collapsed)
        assertTrue(FileExpansionStateHelper.hasAnyExpanded(visibleFiles, collapsed))

        collapsed = FileExpansionStateHelper.toggleFile("uri://file2", collapsed)
        assertTrue(FileExpansionStateHelper.hasAnyExpanded(visibleFiles, collapsed))

        collapsed = FileExpansionStateHelper.toggleFile("uri://file3", collapsed)
        assertFalse(FileExpansionStateHelper.hasAnyExpanded(visibleFiles, collapsed))
    }

    @Test
    fun testEmptyVisibleListAndPartialScopePreservation() {
        val initial = setOf("uri://hidden_other_file")
        assertFalse(FileExpansionStateHelper.hasAnyExpanded(emptyList(), initial))
        assertEquals(initial, FileExpansionStateHelper.toggleAll(emptyList(), initial))

        // Collapse visible files without wiping unrelated URIs
        val collapsed = FileExpansionStateHelper.toggleAll(listOf("uri://file1"), initial)
        assertEquals(setOf("uri://hidden_other_file", "uri://file1"), collapsed)
    }
}
