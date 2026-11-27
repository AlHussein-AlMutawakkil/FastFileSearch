package com.example.fastfilesearch.desktop.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.fastfilesearch.desktop.model.FileResultGroup
import com.example.fastfilesearch.desktop.model.FilteredFileType
import com.example.fastfilesearch.desktop.model.IndexedFileRecord
import com.example.fastfilesearch.desktop.model.SearchMethod
import com.example.fastfilesearch.desktop.model.SearchResult
import com.example.fastfilesearch.desktop.model.SupportedFileTypeGroups
import com.example.fastfilesearch.desktop.parser.DesktopFileParserDispatcher
import com.example.fastfilesearch.desktop.ui.selection.RowKey
import com.example.fastfilesearch.desktop.ui.selection.SelectedRowData
import com.example.fastfilesearch.desktop.util.ArabicNormalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DesktopFileTypeVisual(
    val icon: ImageVector,
    val color: Color,
    val typeName: String
)

fun LayoutCoordinates.unclippedBoundsInRoot(): Rect? {
    if (!isAttached) return null
    val w = size.width.toFloat()
    val h = size.height.toFloat()
    if (w <= 0f || h <= 0f) return null
    val pos = positionInRoot()
    return Rect(pos.x, pos.y, pos.x + w, pos.y + h)
}

object DesktopFileTypeHelper {

    fun getVisual(fileName: String): DesktopFileTypeVisual {
        val ext = DesktopFileParserDispatcher.getExtension(fileName)
        return when (ext) {
            "xlsx", "xls" -> DesktopFileTypeVisual(
                icon = Icons.Default.TableChart,
                color = Color(0xFF1E7E34),
                typeName = if (ext == "xlsx") "Excel (XLSX)" else "Excel (XLS)"
            )
            "mdb", "accdb" -> DesktopFileTypeVisual(
                icon = Icons.Default.Storage,
                color = Color(0xFFA4373A),
                typeName = if (ext == "accdb") "قاعدة Access (ACCDB)" else "قاعدة Access (MDB)"
            )
            "docx", "doc" -> DesktopFileTypeVisual(
                icon = Icons.Default.Description,
                color = Color(0xFF185ABD),
                typeName = if (ext == "docx") "Word (DOCX)" else "Word (DOC)"
            )
            "pdf" -> DesktopFileTypeVisual(
                icon = Icons.Default.PictureAsPdf,
                color = Color(0xFFD32F2F),
                typeName = "PDF"
            )
            "csv" -> DesktopFileTypeVisual(
                icon = Icons.Default.TableChart,
                color = Color(0xFF00796B),
                typeName = "CSV"
            )
            "json" -> DesktopFileTypeVisual(
                icon = Icons.Default.Code,
                color = Color(0xFFE65100),
                typeName = "JSON"
            )
            "md" -> DesktopFileTypeVisual(
                icon = Icons.AutoMirrored.Filled.Article,
                color = Color(0xFF5E35B1),
                typeName = "Markdown"
            )
            "txt", "log" -> DesktopFileTypeVisual(
                icon = Icons.AutoMirrored.Filled.Article,
                color = Color(0xFF455A64),
                typeName = ext.uppercase()
            )
            else -> DesktopFileTypeVisual(
                icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                color = Color(0xFF757575),
                typeName = ext.uppercase()
            )
        }
    }

    fun getVisualForGroup(group: FilteredFileType): DesktopFileTypeVisual {
        return when (group.id) {
            "excel" -> DesktopFileTypeVisual(Icons.Default.TableChart, Color(0xFF1E7E34), "Excel")
            "access" -> DesktopFileTypeVisual(Icons.Default.Storage, Color(0xFFA4373A), "قواعد Access")
            "word" -> DesktopFileTypeVisual(Icons.Default.Description, Color(0xFF185ABD), "Word")
            "pdf" -> DesktopFileTypeVisual(Icons.Default.PictureAsPdf, Color(0xFFD32F2F), "PDF")
            "text" -> DesktopFileTypeVisual(Icons.AutoMirrored.Filled.Article, Color(0xFF455A64), "نصوص")
            "csv" -> DesktopFileTypeVisual(Icons.Default.TableChart, Color(0xFF00796B), "CSV")
            "json" -> DesktopFileTypeVisual(Icons.Default.Code, Color(0xFFE65100), "JSON")
            "markdown" -> DesktopFileTypeVisual(Icons.AutoMirrored.Filled.Article, Color(0xFF5E35B1), "Markdown")
            else -> DesktopFileTypeVisual(Icons.AutoMirrored.Filled.InsertDriveFile, Color(0xFF757575), group.displayName)
        }
    }
}

data class VisibleFileTypeChip(
    val group: FilteredFileType,
    val count: Int
)

/**
 * Resolves which file-format filter pills are actually present (> 0 count or currently selected)
 * so empty format pills stay hidden.
 */
object DesktopFormatFilterHelper {

    fun resolveVisibleGroups(
        typeCounts: Map<String, Int>,
        selectedExtensions: Set<String> = emptySet()
    ): List<VisibleFileTypeChip> {
        val normalizedCounts = typeCounts.entries.associate { (k, v) -> k.lowercase().trim() to v }
        val normalizedSelected = selectedExtensions.map { it.lowercase().trim() }.toSet()
        return SupportedFileTypeGroups.ALL_GROUPS.mapNotNull { group ->
            val extCount = group.extensions.sumOf { ext -> normalizedCounts[ext.lowercase()] ?: 0 }
            val idCount = normalizedCounts[group.id.lowercase()] ?: 0
            val count = maxOf(extCount, idCount)
            val isSelected = group.extensions.any { it.lowercase() in normalizedSelected }
            if (count > 0 || isSelected) {
                VisibleFileTypeChip(group = group, count = count)
            } else {
                null
            }
        }
    }

    fun deriveTypeCountsFromFiles(files: List<IndexedFileRecord>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        files.forEach { rec ->
            val ext = rec.fileType.ifBlank { rec.fileName.substringAfterLast('.', "") }.lowercase().trim()
            if (ext.isNotEmpty()) {
                map[ext] = (map[ext] ?: 0) + 1
            }
        }
        return map
    }

    fun deriveTypeCountsFromResults(groups: List<FileResultGroup>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        groups.forEach { grp ->
            val ext = grp.fileName.substringAfterLast('.', "").lowercase().trim()
            if (ext.isNotEmpty()) {
                map[ext] = (map[ext] ?: 0) + grp.totalMatchesCount.coerceAtLeast(grp.results.size)
            }
        }
        return map
    }
}

object DesktopTableCellHelper {

    private val WHITESPACE_REGEX = "\\s+".toRegex()

    fun extractSheetAndRow(location: String): Pair<String, String> {
        var sheet = "ورقة 1"
        var row = "1"
        if (location.contains("ورقة:")) {
            val parts = location.split("،")
            sheet = parts.getOrNull(0)?.substringAfter("ورقة:")?.trim() ?: "ورقة 1"
            row = parts.getOrNull(1)?.replace("صف:", "")?.replace("صف", "")?.trim() ?: "1"
        } else if (location.contains("جدول:")) {
            val parts = location.split("،")
            sheet = parts.getOrNull(0)?.substringAfter("جدول:")?.trim() ?: "جدول 1"
            row = parts.getOrNull(1)?.replace("صف:", "")?.replace("صف", "")?.trim() ?: "1"
        } else if (location.contains("سطر")) {
            sheet = "بيانات جدولية"
            row = location.replace("سطر", "").replace(":", "").trim()
        } else {
            row = location.filter { it.isDigit() }.ifEmpty { "1" }
        }
        return Pair(sheet, row)
    }

    fun buildSelectableRowsForFile(
        fileUri: String,
        fileName: String,
        isDocument: Boolean,
        results: List<SearchResult>
    ): List<SelectedRowData> {
        if (results.isEmpty()) return emptyList()
        if (isDocument) {
            if (isDocumentType(fileName)) {
                val mergedText = results
                    .mapNotNull { it.snippet.trim().takeIf { s -> s.isNotEmpty() } }
                    .joinToString(" ")
                    .replace(WHITESPACE_REGEX, " ")
                    .trim()
                return listOf(
                    SelectedRowData(
                        key = RowKey(fileUri = fileUri, sheetName = "مستند", rowIndex = 0),
                        fileName = fileName,
                        sheetName = "مستند",
                        rowNumber = "1",
                        cells = emptyList(),
                        headers = emptyList(),
                        isDocument = true,
                        snippet = mergedText,
                        location = ""
                    )
                )
            }
            return results.mapIndexed { idx, res ->
                SelectedRowData(
                    key = RowKey(fileUri = fileUri, sheetName = "مستند", rowIndex = idx),
                    fileName = fileName,
                    sheetName = "مستند",
                    rowNumber = "${idx + 1}",
                    cells = emptyList(),
                    headers = emptyList(),
                    isDocument = true,
                    snippet = res.snippet,
                    location = res.location
                )
            }
        }

        val sheetGroups = results.groupBy { res ->
            res.sheetName ?: extractSheetAndRow(res.location).first
        }
        val list = mutableListOf<SelectedRowData>()
        sheetGroups.forEach { (sheetName, sheetResults) ->
            val sortedResults = sheetResults.sortedBy { res ->
                res.rowNumber ?: extractSheetAndRow(res.location).second.toIntOrNull() ?: 0
            }
            val sheetHeaders = sortedResults.firstOrNull { it.columnHeaders.isNotEmpty() }?.columnHeaders ?: emptyList()
            sortedResults.forEachIndexed { index, res ->
                val rowNum = res.rowNumber?.toString() ?: extractSheetAndRow(res.location).second
                val cells = splitIntoCells(res.snippet)
                list.add(
                    SelectedRowData(
                        key = RowKey(fileUri = fileUri, sheetName = sheetName, rowIndex = index),
                        fileName = fileName,
                        sheetName = sheetName,
                        rowNumber = rowNum,
                        cells = cells,
                        headers = sheetHeaders,
                        isDocument = false
                    )
                )
            }
        }
        return list
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 0L) return "غير معروف"
        if (bytes < 1024L) return "$bytes بايت"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f كيلوبايت", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(Locale.US, "%.2f ميجابايت", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f جيجابايت", gb)
    }

    fun formatTimestamp(epochMillis: Long): String {
        if (epochMillis <= 0L) return "غير معروف"
        return SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(epochMillis))
    }

    fun isDocumentType(fileName: String): Boolean {
        val ext = DesktopFileParserDispatcher.getExtension(fileName)
        return ext == "docx" || ext == "doc" || ext == "pdf"
    }

    fun isTabular(fileName: String, content: String): Boolean {
        val ext = DesktopFileParserDispatcher.getExtension(fileName)
        if (ext in listOf("xlsx", "xls", "csv", "mdb", "accdb")) return true
        if (content.contains("\t") || content.contains(" | ")) return true
        if (content.count { it == ',' } >= 2) return true
        if (content.count { it == ';' } >= 2) return true
        if (content.count { it == '|' } >= 2) return true
        return false
    }

    fun splitIntoCells(content: String): List<String> {
        if (content.contains("\t")) {
            return content.split("\t").map { it.trim() }
        }
        val rawCells = when {
            content.contains(" | ") -> content.split(" | ")
            content.count { it == ';' } >= 2 -> content.split(";")
            content.count { it == ',' } >= 2 -> content.split(",")
            content.contains("|") -> content.split("|")
            else -> listOf(content)
        }
        val cleaned = rawCells.map { it.trim() }
        return if (cleaned.size > 1) cleaned else listOf(content.trim())
    }

    fun isCellMatching(cell: String, query: String): Boolean {
        if (query.isBlank() || cell.isBlank()) return false
        val normalizedCell = ArabicNormalizer.normalize(cell)
        val normalizedQuery = ArabicNormalizer.normalize(query)
        val tokens = normalizedQuery.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return false
        return tokens.any { normalizedCell.contains(it) }
    }
}

object DesktopTextHighlighter {

    private val WHITESPACE_REGEX = "\\s+".toRegex()
    private val TOKENIZE_REGEX = Regex("(?<=\\s)|(?=\\s)|(?<=[^\\p{L}\\p{Nd}])|(?=[^\\p{L}\\p{Nd}])")

    private data class WordInfo(
        val startOffset: Int,
        val length: Int,
        val text: String,
        val normalized: String
    )

    fun highlight(
        text: String,
        query: String,
        method: SearchMethod = SearchMethod.CONTAINS,
        ignoreDiacritics: Boolean = true,
        highlightColor: Color = Color(0xFFFFECB3),
        highlightTextColor: Color = Color(0xFF332600)
    ): AnnotatedString {
        if (text.isEmpty() || query.isBlank()) return AnnotatedString(text)

        val rawTokens = query.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        if (rawTokens.isEmpty()) return AnnotatedString(text)

        val queryTokens = if (ignoreDiacritics) {
            ArabicNormalizer.normalize(query).split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        } else {
            rawTokens
        }
        if (queryTokens.isEmpty()) return AnnotatedString(text)

        val words = text.split(TOKENIZE_REGEX)
        val wordInfos = mutableListOf<WordInfo>()
        var offset = 0
        for (word in words) {
            val norm = if (ignoreDiacritics) ArabicNormalizer.normalize(word) else word
            wordInfos.add(WordInfo(offset, word.length, word, norm))
            offset += word.length
        }

        val ranges = mutableListOf<IntRange>()
        when (method) {
            SearchMethod.EXACT_PHRASE -> {
                val meaningful = wordInfos.filter { it.normalized.isNotBlank() }
                if (meaningful.size >= queryTokens.size) {
                    for (i in 0..(meaningful.size - queryTokens.size)) {
                        val sub = (0 until queryTokens.size).map { meaningful[i + it].normalized }
                        val matches = if (ignoreDiacritics) {
                            sub == queryTokens
                        } else {
                            sub.map { it.trim() } == queryTokens.map { it.trim() }
                        }
                        if (matches) {
                            val startChar = meaningful[i].startOffset
                            val last = meaningful[i + queryTokens.size - 1]
                            ranges.add(startChar until (last.startOffset + last.length))
                        }
                    }
                }
            }
            SearchMethod.WHOLE_WORD -> {
                for (info in wordInfos) {
                    if (info.normalized.isNotBlank()) {
                        val matches = if (ignoreDiacritics) {
                            queryTokens.any { it == info.normalized }
                        } else {
                            queryTokens.any { it == info.text }
                        }
                        if (matches) ranges.add(info.startOffset until (info.startOffset + info.length))
                    }
                }
            }
            SearchMethod.CONTAINS, SearchMethod.ALL_WORDS -> {
                for (info in wordInfos) {
                    if (info.normalized.isNotBlank()) {
                        val matches = if (ignoreDiacritics) {
                            queryTokens.any { info.normalized.contains(it) }
                        } else {
                            queryTokens.any { info.text.contains(it) }
                        }
                        if (matches) ranges.add(info.startOffset until (info.startOffset + info.length))
                    }
                }
            }
        }

        return buildAnnotatedString {
            append(text)
            for (r in ranges) {
                if (r.first in 0 until text.length && r.last < text.length) {
                    addStyle(
                        style = SpanStyle(
                            background = highlightColor,
                            color = highlightTextColor,
                            fontWeight = FontWeight.Bold
                        ),
                        start = r.first,
                        end = r.last + 1
                    )
                }
            }
        }
    }
}

object DesktopFileExpansionStateHelper {

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
