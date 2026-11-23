package com.example.fastfilesearch

import com.example.fastfilesearch.model.IndexedFileRecord
import com.example.fastfilesearch.model.SearchFilter
import com.example.fastfilesearch.model.Workspace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DifferentialIndexingAndWorkspacesTest {

    data class DiscoveredFile(
        val uri: String,
        val name: String,
        val lastModified: Long,
        val size: Long
    )

    enum class SyncDecision {
        SKIP,
        INDEX,
        DELETE
    }

    private fun determineFileAction(
        discovered: DiscoveredFile,
        existing: IndexedFileRecord?
    ): SyncDecision {
        return if (existing != null &&
            existing.lastModified == discovered.lastModified &&
            existing.size == discovered.size
        ) {
            SyncDecision.SKIP
        } else {
            SyncDecision.INDEX
        }
    }

    @Test
    fun testWorkspaceModel() {
        val now = System.currentTimeMillis()
        val ws = Workspace(
            id = "ws_test_1",
            name = "مستندات العمل",
            uri = "content://com.android.externalstorage.documents/tree/primary%3AWork",
            addedAt = now
        )

        assertEquals("ws_test_1", ws.id)
        assertEquals("مستندات العمل", ws.name)
        assertEquals(now, ws.addedAt)
    }

    @Test
    fun testDifferentialIndexingSkipsUnchangedFiles() {
        val existing = IndexedFileRecord(
            uri = "content://file/1",
            workspaceId = "ws_1",
            fileName = "report.xlsx",
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            size = 10240L,
            lastModified = 1700000000000L,
            indexedAt = 1700000005000L
        )

        val discovered = DiscoveredFile(
            uri = "content://file/1",
            name = "report.xlsx",
            lastModified = 1700000000000L,
            size = 10240L
        )

        val action = determineFileAction(discovered, existing)
        assertEquals(SyncDecision.SKIP, action)
    }

    @Test
    fun testDifferentialIndexingIndexesModifiedFile_WhenTimestampChanged() {
        val existing = IndexedFileRecord(
            uri = "content://file/1",
            workspaceId = "ws_1",
            fileName = "report.xlsx",
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            size = 10240L,
            lastModified = 1700000000000L,
            indexedAt = 1700000005000L
        )

        val discoveredModifiedTime = DiscoveredFile(
            uri = "content://file/1",
            name = "report.xlsx",
            lastModified = 1700000500000L, // changed
            size = 10240L
        )

        val action = determineFileAction(discoveredModifiedTime, existing)
        assertEquals(SyncDecision.INDEX, action)
    }

    @Test
    fun testDifferentialIndexingIndexesModifiedFile_WhenSizeChanged() {
        val existing = IndexedFileRecord(
            uri = "content://file/1",
            workspaceId = "ws_1",
            fileName = "report.xlsx",
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            size = 10240L,
            lastModified = 1700000000000L,
            indexedAt = 1700000005000L
        )

        val discoveredModifiedSize = DiscoveredFile(
            uri = "content://file/1",
            name = "report.xlsx",
            lastModified = 1700000000000L,
            size = 12500L // changed
        )

        val action = determineFileAction(discoveredModifiedSize, existing)
        assertEquals(SyncDecision.INDEX, action)
    }

    @Test
    fun testDifferentialIndexingIndexesNewFile() {
        val discoveredNew = DiscoveredFile(
            uri = "content://file/2",
            name = "budget.docx",
            lastModified = 1700000000000L,
            size = 5000L
        )

        val action = determineFileAction(discoveredNew, null)
        assertEquals(SyncDecision.INDEX, action)
    }

    @Test
    fun testDifferentialIndexingDetectsDeletedFiles() {
        val existingFiles = mapOf(
            "content://file/1" to IndexedFileRecord(uri = "content://file/1", workspaceId = "ws_1", fileName = "f1.pdf", mimeType = "application/pdf", size = 50L, lastModified = 100L),
            "content://file/2" to IndexedFileRecord(uri = "content://file/2", workspaceId = "ws_1", fileName = "f2.pdf", mimeType = "application/pdf", size = 50L, lastModified = 100L),
            "content://file/3" to IndexedFileRecord(uri = "content://file/3", workspaceId = "ws_1", fileName = "f3.pdf", mimeType = "application/pdf", size = 50L, lastModified = 100L)
        )

        val discoveredUris = setOf(
            "content://file/1",
            "content://file/3"
        )

        val deletedUris = existingFiles.keys - discoveredUris
        assertEquals(1, deletedUris.size)
        assertTrue(deletedUris.contains("content://file/2"))
    }

    @Test
    fun testSearchFilterWorkspaceIds() {
        val defaultFilter = SearchFilter()
        assertFalse(defaultFilter.isFilterActive)
        assertTrue(defaultFilter.selectedWorkspaceIds.isEmpty())

        val workspaceFiltered = SearchFilter(selectedWorkspaceIds = setOf("ws_finance"))
        assertTrue(workspaceFiltered.isFilterActive)
        assertEquals(setOf("ws_finance"), workspaceFiltered.selectedWorkspaceIds)
    }

    @Test
    fun testWorkspaceFilteringLogic() {
        val files = listOf(
            IndexedFileRecord(uri = "uri_1", workspaceId = "ws_a", fileName = "file1.txt", mimeType = "text/plain", size = 1L, lastModified = 1L),
            IndexedFileRecord(uri = "uri_2", workspaceId = "ws_b", fileName = "file2.txt", mimeType = "text/plain", size = 1L, lastModified = 1L),
            IndexedFileRecord(uri = "uri_3", workspaceId = "ws_a", fileName = "file3.txt", mimeType = "text/plain", size = 1L, lastModified = 1L)
        )

        val selectedWorkspaces = setOf("ws_a")
        val filtered = files.filter { file ->
            selectedWorkspaces.isEmpty() || selectedWorkspaces.contains(file.workspaceId)
        }

        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.workspaceId == "ws_a" })

        // When an active workspace is set, results/files are strictly scoped to it
        val activeWorkspaceId: String? = "ws_a"
        val activeFiltered = files.filter { file ->
            if (activeWorkspaceId != null) {
                file.workspaceId == activeWorkspaceId
            } else {
                selectedWorkspaces.isEmpty() || selectedWorkspaces.contains(file.workspaceId)
            }
        }
        assertEquals(2, activeFiltered.size)
        assertTrue(activeFiltered.all { it.workspaceId == "ws_a" })

        // When no workspace is selected, all files are matched
        val allFiles = files.filter { file ->
            emptySet<String>().isEmpty() || emptySet<String>().contains(file.workspaceId)
        }
        assertEquals(3, allFiles.size)
    }
}
