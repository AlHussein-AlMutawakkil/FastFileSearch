package com.example.fastfilesearch.desktop.parser

import com.example.fastfilesearch.desktop.model.FileSegment
import com.example.fastfilesearch.desktop.util.ArabicNormalizer
import com.healthmarketscience.jackcess.Column
import com.healthmarketscience.jackcess.DatabaseBuilder
import com.healthmarketscience.jackcess.Row
import com.healthmarketscience.jackcess.complex.ComplexValueForeignKey
import java.io.File
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Streams rows from Microsoft Access (.mdb / .accdb) files in Read-Only mode via Jackcess.
 */
object DesktopAccessParser {

    private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val DATE_TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)

    fun parseFile(
        dbFile: File,
        fileUri: String = dbFile.absolutePath,
        fileName: String = dbFile.name,
        onHeaderFound: (sheetName: String, headers: List<String>) -> Unit = { _, _ -> },
        onSegmentFound: (FileSegment) -> Unit
    ) {
        if (!dbFile.exists() || !dbFile.isFile || dbFile.length() == 0L) return

        try {
            DatabaseBuilder(dbFile)
                .setReadOnly(true)
                .open()
                .use { db ->
                    for (tableName in db.tableNames) {
                        if (tableName.startsWith("MSys", ignoreCase = true) ||
                            tableName.startsWith("~TMP", ignoreCase = true)
                        ) {
                            continue
                        }

                        try {
                            val table = db.getTable(tableName) ?: continue
                            val columns = table.columns
                            if (columns.isEmpty()) continue

                            val headers = columns.map { col -> col.name.trim().ifEmpty { "عمود" } }
                            onHeaderFound(tableName, headers)

                            var rowNumber = 0
                            for (row in table) {
                                rowNumber++
                                val cells = extractRowCells(row, columns)
                                if (cells.all { it.isEmpty() }) continue

                                val rowText = cells.joinToString("\t")
                                val normalized = ArabicNormalizer.normalize(rowText)
                                if (normalized.isNotEmpty()) {
                                    onSegmentFound(
                                        FileSegment(
                                            fileUri = fileUri,
                                            fileName = fileName,
                                            location = "جدول: $tableName، صف: $rowNumber",
                                            content = rowText,
                                            normalizedContent = normalized
                                        )
                                    )
                                }
                            }
                        } catch (_: Throwable) {
                            // Skip problematic table without aborting the database
                        }
                    }
                }
        } catch (_: Throwable) {
            // Password-protected / corrupt / unsupported Access files skipped safely
        }
    }

    private fun extractRowCells(row: Row, columns: List<Column>): List<String> {
        val cells = ArrayList<String>(columns.size)
        for (column in columns) {
            val raw = try {
                row[column.name]
            } catch (_: Throwable) {
                null
            }
            cells.add(formatValue(raw))
        }
        return cells
    }

    internal fun formatValue(rawValue: Any?): String {
        if (rawValue == null) return ""
        val rawStr = when (rawValue) {
            is ByteArray -> "[بيانات ثنائية]"
            is LocalDateTime -> {
                if (rawValue.hour == 0 && rawValue.minute == 0 && rawValue.second == 0) {
                    rawValue.toLocalDate().format(DATE_FMT)
                } else {
                    rawValue.format(DATE_TIME_FMT)
                }
            }
            is LocalDate -> rawValue.format(DATE_FMT)
            is Date -> {
                val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(rawValue)
                if (fmt.endsWith(" 00:00")) fmt.substring(0, 10) else fmt
            }
            is BigDecimal -> if (rawValue.compareTo(BigDecimal.ZERO) == 0) "0" else rawValue.stripTrailingZeros().toPlainString()
            is Double -> formatFloating(rawValue)
            is Float -> formatFloating(rawValue.toDouble())
            is ComplexValueForeignKey -> try {
                rawValue.values?.joinToString(", ") { it.toString() }.orEmpty()
            } catch (_: Throwable) {
                ""
            }
            else -> rawValue.toString()
        }
        return rawStr.replace("[\t\r\n]+".toRegex(), " ").trim()
    }

    private fun formatFloating(value: Double): String {
        if (!value.isFinite()) return ""
        if (value % 1.0 == 0.0 && abs(value) < 1e15) {
            return value.toLong().toString()
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    }
}
