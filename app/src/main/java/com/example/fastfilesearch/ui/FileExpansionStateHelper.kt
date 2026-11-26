package com.example.fastfilesearch.ui

/**
 * Pure state transitions for per-file card collapse/expand synchronized with
 * the toolbar "Collapse All / Expand All" toggle.
 *
 * A file URI is expanded by default whenever it is absent from [collapsedFileUris].
 */
object FileExpansionStateHelper {

    fun isExpanded(fileUri: String, collapsedFileUris: Set<String>): Boolean {
        return !collapsedFileUris.contains(fileUri)
    }

    fun hasAnyExpanded(visibleFileUris: List<String>, collapsedFileUris: Set<String>): Boolean {
        return visibleFileUris.any { it !in collapsedFileUris }
    }

    fun toggleFile(fileUri: String, collapsedFileUris: Set<String>): Set<String> {
        return if (fileUri in collapsedFileUris) {
            collapsedFileUris - fileUri
        } else {
            collapsedFileUris + fileUri
        }
    }

    fun toggleAll(visibleFileUris: List<String>, collapsedFileUris: Set<String>): Set<String> {
        if (visibleFileUris.isEmpty()) return collapsedFileUris
        return if (hasAnyExpanded(visibleFileUris, collapsedFileUris)) {
            collapsedFileUris + visibleFileUris
        } else {
            collapsedFileUris - visibleFileUris.toSet()
        }
    }
}
