package com.example.fastfilesearch.model

data class IndexedFileRecord(
    val uri: String,
    val fileName: String,
    val mimeType: String?,
    val size: Long,
    val lastModified: Long,
    val indexedAt: Long = System.currentTimeMillis()
)

data class FileSegment(
    val fileUri: String,
    val fileName: String,
    val location: String,
    val content: String,
    val normalizedContent: String
)

data class SearchResult(
    val id: Long = 0,
    val fileUri: String,
    val fileName: String,
    val location: String,
    val snippet: String,
    val query: String
)
