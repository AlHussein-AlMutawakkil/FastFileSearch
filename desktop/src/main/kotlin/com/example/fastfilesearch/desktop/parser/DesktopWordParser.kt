package com.example.fastfilesearch.desktop.parser

import com.example.fastfilesearch.desktop.model.FileSegment
import com.example.fastfilesearch.desktop.util.ArabicNormalizer
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream
import java.util.Locale

/**
 * Extracts text paragraphs and table rows from Word (.docx and legacy .doc) via Apache POI.
 */
object DesktopWordParser {

    fun parseFile(
        file: File,
        fileUri: String = file.absolutePath,
        fileName: String = file.name,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        if (!file.exists() || !file.isFile || file.length() == 0L) return
        val ext = file.extension.lowercase(Locale.ROOT)
        try {
            if (ext == "doc") {
                parseLegacyDoc(file, fileUri, fileName, onSegmentFound)
            } else {
                parseDocx(file, fileUri, fileName, onSegmentFound)
            }
        } catch (_: Throwable) {
            // Skip corrupt / password-protected Word files safely
        }
    }

    private fun parseDocx(
        file: File,
        fileUri: String,
        fileName: String,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        var idx = 0
        FileInputStream(file).use { fis ->
            XWPFDocument(fis).use { doc ->
                for (paragraph in doc.paragraphs) {
                    val text = paragraph.text?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        idx++
                        emitSegment(text, idx, fileUri, fileName, onSegmentFound)
                    }
                }
                for (table in doc.tables) {
                    for (row in table.rows) {
                        val rowText = row.tableCells
                            .map { it.text?.replace("[\t\r\n]+".toRegex(), " ")?.trim().orEmpty() }
                            .joinToString(" | ")
                            .trim()
                        if (rowText.isNotEmpty()) {
                            idx++
                            emitSegment(rowText, idx, fileUri, fileName, onSegmentFound)
                        }
                    }
                }
            }
        }
    }

    private fun parseLegacyDoc(
        file: File,
        fileUri: String,
        fileName: String,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        var idx = 0
        FileInputStream(file).use { fis ->
            HWPFDocument(fis).use { hwpf ->
                WordExtractor(hwpf).use { extractor ->
                    for (rawPara in extractor.paragraphText) {
                        val cleaned = rawPara
                            .replace("\u0007", " ")
                            .replace("[\r\n]+".toRegex(), " ")
                            .trim()
                        if (cleaned.isNotEmpty()) {
                            idx++
                            emitSegment(cleaned, idx, fileUri, fileName, onSegmentFound)
                        }
                    }
                }
            }
        }
    }

    private fun emitSegment(
        text: String,
        index: Int,
        fileUri: String,
        fileName: String,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        val normalized = ArabicNormalizer.normalize(text)
        if (normalized.isNotEmpty()) {
            onSegmentFound(
                FileSegment(
                    fileUri = fileUri,
                    fileName = fileName,
                    location = "فقرة $index",
                    content = text,
                    normalizedContent = normalized
                )
            )
        }
    }
}
