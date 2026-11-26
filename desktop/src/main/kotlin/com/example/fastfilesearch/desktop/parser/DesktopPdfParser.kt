package com.example.fastfilesearch.desktop.parser

import com.example.fastfilesearch.desktop.model.FileSegment
import com.example.fastfilesearch.desktop.util.ArabicNormalizer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

/**
 * Extracts text line-by-line per page from PDF files via Apache PDFBox.
 */
object DesktopPdfParser {

    fun parseFile(
        file: File,
        fileUri: String = file.absolutePath,
        fileName: String = file.name,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        if (!file.exists() || !file.isFile || file.length() == 0L) return

        try {
            PDDocument.load(file).use { document ->
                if (document.isEncrypted) return

                val totalPages = document.numberOfPages
                val stripper = PDFTextStripper()
                for (page in 1..totalPages) {
                    stripper.startPage = page
                    stripper.endPage = page
                    val pageText = try {
                        stripper.getText(document)
                    } catch (_: Throwable) {
                        ""
                    }
                    if (pageText.isBlank()) continue

                    pageText.lineSequence().forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) {
                            val normalized = ArabicNormalizer.normalize(trimmed)
                            if (normalized.isNotEmpty()) {
                                onSegmentFound(
                                    FileSegment(
                                        fileUri = fileUri,
                                        fileName = fileName,
                                        location = "صفحة $page",
                                        content = trimmed,
                                        normalizedContent = normalized
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            // Skip encrypted / corrupt PDF files safely
        }
    }
}
