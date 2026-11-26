package com.example.fastfilesearch.desktop.parser

import com.example.fastfilesearch.desktop.model.FileSegment
import com.example.fastfilesearch.desktop.util.ArabicNormalizer
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Parses plain text files (.txt, .csv, .json, .md, .log) with automatic encoding detection
 * supporting UTF-8 (with/without BOM), UTF-16, and Windows-1256 (Arabic ANSI fallback).
 */
object DesktopTextParser {

    private val WINDOWS_1256: Charset = try {
        Charset.forName("windows-1256")
    } catch (_: Throwable) {
        StandardCharsets.ISO_8859_1
    }

    fun detectCharset(file: File): Charset {
        val sampleSize = minOf(file.length(), 65_536L).toInt()
        if (sampleSize <= 0) return StandardCharsets.UTF_8

        val buffer = ByteArray(sampleSize)
        val readCount = FileInputStream(file).use { it.read(buffer) }
        if (readCount <= 0) return StandardCharsets.UTF_8

        // BOM checks
        if (readCount >= 3 &&
            buffer[0] == 0xEF.toByte() &&
            buffer[1] == 0xBB.toByte() &&
            buffer[2] == 0xBF.toByte()
        ) {
            return StandardCharsets.UTF_8
        }
        if (readCount >= 2 && buffer[0] == 0xFF.toByte() && buffer[1] == 0xFE.toByte()) {
            return StandardCharsets.UTF_16LE
        }
        if (readCount >= 2 && buffer[0] == 0xFE.toByte() && buffer[1] == 0xFF.toByte()) {
            return StandardCharsets.UTF_16BE
        }

        // Strict UTF-8 validation on sample
        return try {
            val decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            decoder.decode(ByteBuffer.wrap(buffer, 0, readCount))
            StandardCharsets.UTF_8
        } catch (_: Exception) {
            WINDOWS_1256
        }
    }

    fun parseFile(
        file: File,
        fileUri: String = file.absolutePath,
        fileName: String = file.name,
        onHeaderFound: (sheetName: String, headers: List<String>) -> Unit = { _, _ -> },
        onSegmentFound: (FileSegment) -> Unit
    ) {
        if (!file.exists() || !file.isFile || file.length() == 0L) return

        val ext = file.extension.lowercase(Locale.ROOT)
        val charset = detectCharset(file)

        try {
            BufferedReader(InputStreamReader(FileInputStream(file), charset)).useLines { lines ->
                if (ext == "csv") {
                    var delimiter: Char? = null
                    var headerCaptured = false
                    lines.forEachIndexed { index, rawLine ->
                        val cleanLine = (if (index == 0) rawLine.removePrefix("\uFEFF") else rawLine).trim()
                        if (cleanLine.isEmpty()) return@forEachIndexed

                        if (delimiter == null) {
                            delimiter = detectCsvDelimiter(cleanLine)
                        }
                        val sep = delimiter ?: ','
                        val cells = splitCsvLine(cleanLine, sep)
                        if (!headerCaptured) {
                            headerCaptured = true
                            onHeaderFound("ورقة 1", cells)
                        }

                        val tabJoined = cells.joinToString("\t")
                        val normalized = ArabicNormalizer.normalize(tabJoined)
                        if (normalized.isNotEmpty()) {
                            onSegmentFound(
                                FileSegment(
                                    fileUri = fileUri,
                                    fileName = fileName,
                                    location = "ورقة: ورقة 1، صف: ${index + 1}",
                                    content = tabJoined,
                                    normalizedContent = normalized
                                )
                            )
                        }
                    }
                } else {
                    lines.forEachIndexed { index, rawLine ->
                        val cleanLine = (if (index == 0) rawLine.removePrefix("\uFEFF") else rawLine).trim()
                        if (cleanLine.isNotEmpty()) {
                            val normalized = ArabicNormalizer.normalize(cleanLine)
                            if (normalized.isNotEmpty()) {
                                onSegmentFound(
                                    FileSegment(
                                        fileUri = fileUri,
                                        fileName = fileName,
                                        location = "سطر ${index + 1}",
                                        content = cleanLine,
                                        normalizedContent = normalized
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            // Skip unreadable text files safely
        }
    }

    private fun detectCsvDelimiter(sampleLine: String): Char {
        val commaCount = sampleLine.count { it == ',' }
        val semiCount = sampleLine.count { it == ';' }
        val tabCount = sampleLine.count { it == '\t' }
        return when {
            tabCount >= semiCount && tabCount > commaCount -> '\t'
            semiCount > commaCount -> ';'
            else -> ','
        }
    }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == delimiter && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.setLength(0)
                }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }
}
