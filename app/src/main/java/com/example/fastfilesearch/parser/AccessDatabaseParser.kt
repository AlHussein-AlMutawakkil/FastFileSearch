package com.example.fastfilesearch.parser

import android.content.Context
import android.util.Log
import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.util.ArabicNormalizer
import com.healthmarketscience.jackcess.Column
import com.healthmarketscience.jackcess.DatabaseBuilder
import com.healthmarketscience.jackcess.Row
import com.healthmarketscience.jackcess.complex.ComplexValueForeignKey
import java.io.File
import java.io.InputStream
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Streams rows from Microsoft Access databases (.mdb / .accdb) in strict read-only mode
 * using Jackcess without loading full tables into memory.
 */
object AccessDatabaseParser {

    private const val TAG = "AccessDatabaseParser"
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)

    fun parse(
        context: Context,
        inputStream: InputStream,
        fileUri: String,
        fileName: String,
        onHeaderFound: (sheetName: String, headers: List<String>) -> Unit = { _, _ -> },
        onSegmentFound: (FileSegment) -> Unit
    ) {
        val ext = FileParserDispatcher.getExtension(fileName)
        val suffix = if (ext == "accdb" || ext == "mdb") ".$ext" else ".mdb"
        val tempFile = File.createTempFile("access_", suffix, context.cacheDir)
        try {
            tempFile.outputStream().use { out ->
                inputStream.copyTo(out)
            }
            parseFile(
                dbFile = tempFile,
                fileUri = fileUri,
                fileName = fileName,
                onHeaderFound = onHeaderFound,
                onSegmentFound = onSegmentFound
            )
        } catch (t: Throwable) {
            safeLogWarn("Failed staging Access file: $fileName", t)
        } finally {
            try {
                if (tempFile.exists()) tempFile.delete()
            } catch (_: Throwable) {
            }
        }
    }

    /**
     * Opens an Access database file read-only and streams all non-system tables row-by-row.
     * Password-protected, corrupted, or unsupported databases are skipped safely without crashing.
     */
    fun parseFile(
        dbFile: File,
        fileUri: String,
        fileName: String,
        onHeaderFound: (sheetName: String, headers: List<String>) -> Unit = { _, _ -> },
        onSegmentFound: (FileSegment) -> Unit
    ) {
        if (!dbFile.exists() || !dbFile.isFile || dbFile.length() == 0L) return

        try {
            DatabaseBuilder(dbFile)
                .setReadOnly(true)
                .open()
                .use { database ->
                    val tableNames = database.tableNames
                    for (tableName in tableNames) {
                        if (tableName.startsWith("MSys", ignoreCase = true) ||
                            tableName.startsWith("~TMP", ignoreCase = true)
                        ) {
                            continue
                        }

                        try {
                            val table = database.getTable(tableName) ?: continue
                            val columns = table.columns
                            if (columns.isEmpty()) continue

                            val headers = columns.map { col ->
                                col.name.trim().ifEmpty { "عمود" }
                            }
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
                        } catch (tableErr: Throwable) {
                            safeLogWarn("Skipping unreadable Access table '$tableName' in $fileName", tableErr)
                        }
                    }
                }
        } catch (dbErr: Throwable) {
            // Encrypted / password-protected / corrupt / unsupported Access formats are skipped safely
            safeLogWarn("Skipping unsupported or corrupt Access file: $fileName", dbErr)
        }
    }

    private fun extractRowCells(row: Row, columns: List<Column>): List<String> {
        val cells = ArrayList<String>(columns.size)
        for (column in columns) {
            val rawValue = try {
                row[column.name]
            } catch (_: Throwable) {
                null
            }
            cells.add(formatCellValue(rawValue))
        }
        return cells
    }

    internal fun formatCellValue(rawValue: Any?): String {
        if (rawValue == null) return ""
        val rawString = when (rawValue) {
            is ByteArray -> "[بيانات ثنائية]"
            is LocalDateTime -> {
                if (rawValue.hour == 0 && rawValue.minute == 0 && rawValue.second == 0) {
                    rawValue.toLocalDate().format(DATE_FORMATTER)
                } else {
                    rawValue.format(DATE_TIME_FORMATTER)
                }
            }
            is LocalDate -> rawValue.format(DATE_FORMATTER)
            is Date -> formatLegacyDate(rawValue)
            is BigDecimal -> {
                if (rawValue.compareTo(BigDecimal.ZERO) == 0) {
                    "0"
                } else {
                    rawValue.stripTrailingZeros().toPlainString()
                }
            }
            is Double -> formatFloatingNumber(rawValue)
            is Float -> formatFloatingNumber(rawValue.toDouble())
            is ComplexValueForeignKey -> {
                try {
                    rawValue.values?.joinToString(", ") { it.toString() }.orEmpty()
                } catch (_: Throwable) {
                    ""
                }
            }
            else -> rawValue.toString()
        }
        return rawString.replace("[\t\r\n]+".toRegex(), " ").trim()
    }

    private fun formatLegacyDate(date: Date): String {
        val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val formatted = timeFmt.format(date)
        return if (formatted.endsWith(" 00:00")) {
            formatted.substring(0, 10)
        } else {
            formatted
        }
    }

    private fun formatFloatingNumber(value: Double): String {
        if (!value.isFinite()) return ""
        if (value % 1.0 == 0.0 && abs(value) < 1e15) {
            return value.toLong().toString()
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    }

    private fun safeLogWarn(message: String, error: Throwable) {
        try {
            Log.w(TAG, "$message (${error.javaClass.simpleName}: ${error.message})")
        } catch (_: Throwable) {
            // android.util.Log is unmocked in pure JVM unit tests
        }
    }
}
