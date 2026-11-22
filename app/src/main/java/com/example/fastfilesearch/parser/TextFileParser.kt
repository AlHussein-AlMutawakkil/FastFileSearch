package com.example.fastfilesearch.parser

import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.util.ArabicNormalizer
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object TextFileParser {

    /**
     * Extracts text from plain text files (TXT, CSV, JSON, MD, LOG) line-by-line using UTF-8.
     */
    fun parse(
        inputStream: InputStream,
        fileUri: String,
        fileName: String,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).useLines { lines ->
            lines.forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    val normalized = ArabicNormalizer.normalize(trimmed)
                    if (normalized.isNotEmpty()) {
                        onSegmentFound(
                            FileSegment(
                                fileUri = fileUri,
                                fileName = fileName,
                                location = "سطر ${index + 1}",
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
