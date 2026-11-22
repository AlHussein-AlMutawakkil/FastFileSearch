package com.example.fastfilesearch.parser

import android.content.Context
import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.util.ArabicNormalizer
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

object PdfParser {

    private val isInitialized = AtomicBoolean(false)

    fun initialize(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            PDFBoxResourceLoader.init(context.applicationContext)
        }
    }

    /**
     * Extracts text from PDF files page-by-page using pdfbox-android.
     */
    fun parse(
        context: Context,
        inputStream: InputStream,
        fileUri: String,
        fileName: String,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        initialize(context)

        PDDocument.load(inputStream).use { document ->
            val stripper = PDFTextStripper()
            val totalPages = document.numberOfPages

            for (page in 1..totalPages) {
                stripper.startPage = page
                stripper.endPage = page
                val pageText = stripper.getText(document)

                if (pageText.isNotBlank()) {
                    for (line in pageText.lines()) {
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
        }
    }
}
