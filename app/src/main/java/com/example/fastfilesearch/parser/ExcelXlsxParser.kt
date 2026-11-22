package com.example.fastfilesearch.parser

import android.content.Context
import android.util.Log
import android.util.Xml
import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.util.ArabicNormalizer
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object ExcelXlsxParser {

    private const val TAG = "ExcelXlsxParser"

    /**
     * Extracts text from modern Excel (.xlsx) files using a streaming, high-performance
     * OpenXML Zip + XmlPullParser implementation that does not depend on StAX (javax.xml.stream).
     *
     * Correctly aligns columns by parsing cell references (e.g. r="C5" -> column index 2),
     * pads missing cells with "", and joins row data with "\t".
     *
     * Inspects the first 5 non-empty rows to determine true column headers for each sheet.
     */
    fun parse(
        context: Context,
        inputStream: InputStream,
        fileUri: String,
        fileName: String,
        onHeaderFound: (sheetName: String, headers: List<String>) -> Unit = { _, _ -> },
        onSegmentFound: (FileSegment) -> Unit
    ) {
        val tempFile = File.createTempFile("xlsx_", ".tmp", context.cacheDir)
        try {
            tempFile.outputStream().use { out ->
                inputStream.copyTo(out)
            }

            ZipFile(tempFile).use { zip ->
                // 1. Parse sheet names from xl/workbook.xml
                val sheetNames = parseSheetNames(zip)

                // 2. Parse shared strings from xl/sharedStrings.xml
                val sharedStrings = parseSharedStrings(zip)

                // 3. Find and sort sheet entries: xl/worksheets/sheet1.xml, sheet2.xml, ...
                val sheetEntries = mutableListOf<ZipEntry>()
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name
                    if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                        sheetEntries.add(entry)
                    }
                }

                sheetEntries.sortBy { entry ->
                    val num = entry.name.removePrefix("xl/worksheets/sheet").removeSuffix(".xml").toIntOrNull() ?: 0
                    num
                }

                // 4. Parse each sheet
                sheetEntries.forEachIndexed { index, sheetEntry ->
                    val sheetName = sheetNames.getOrNull(index) ?: "ورقة ${index + 1}"
                    parseSheetData(
                        zip = zip,
                        sheetEntry = sheetEntry,
                        sheetName = sheetName,
                        sharedStrings = sharedStrings,
                        fileUri = fileUri,
                        fileName = fileName,
                        onHeaderFound = onHeaderFound,
                        onSegmentFound = onSegmentFound
                    )
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error parsing xlsx file: $fileName", t)
            throw t
        } finally {
            try {
                if (tempFile.exists()) tempFile.delete()
            } catch (ignored: Exception) {}
        }
    }

    private fun parseSheetNames(zip: ZipFile): List<String> {
        val sheetNames = mutableListOf<String>()
        val workbookEntry = zip.getEntry("xl/workbook.xml") ?: return sheetNames

        try {
            zip.getInputStream(workbookEntry).use { stream ->
                val parser: XmlPullParser = Xml.newPullParser()
                parser.setInput(stream, "UTF-8")
                var event = parser.eventType

                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        val name = parser.name
                        if (name.equals("sheet", ignoreCase = true) || name.endsWith(":sheet")) {
                            val sheetName = parser.getAttributeValue(null, "name")
                            if (!sheetName.isNullOrBlank()) {
                                sheetNames.add(sheetName.trim())
                            }
                        }
                    }
                    event = parser.next()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse sheet names from xl/workbook.xml", e)
        }

        return sheetNames
    }

    private fun parseSharedStrings(zip: ZipFile): List<String> {
        val sharedStrings = ArrayList<String>()
        val sstEntry = zip.getEntry("xl/sharedStrings.xml") ?: return sharedStrings

        try {
            zip.getInputStream(sstEntry).use { stream ->
                val parser: XmlPullParser = Xml.newPullParser()
                parser.setInput(stream, "UTF-8")
                var event = parser.eventType
                var insideSi = false
                val currentString = StringBuilder()

                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            val name = parser.name
                            if (name.equals("si", ignoreCase = true) || name.endsWith(":si")) {
                                insideSi = true
                                currentString.setLength(0)
                            } else if (insideSi && (name.equals("t", ignoreCase = true) || name.endsWith(":t"))) {
                                val text = parser.nextText()
                                currentString.append(text)
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            val name = parser.name
                            if (name.equals("si", ignoreCase = true) || name.endsWith(":si")) {
                                insideSi = false
                                sharedStrings.add(currentString.toString().trim())
                                currentString.setLength(0)
                            }
                        }
                    }
                    event = parser.next()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing shared strings", e)
        }

        return sharedStrings
    }

    /**
     * Converts an Excel column reference (e.g. "C", "AA") into a 0-based index.
     * "A" -> 0, "B" -> 1, "Z" -> 25, "AA" -> 26, etc.
     */
    private fun extractColumnIndexFromRef(cellRef: String?): Int? {
        if (cellRef.isNullOrEmpty()) return null
        var i = 0
        while (i < cellRef.length && cellRef[i].isLetter()) {
            i++
        }
        if (i == 0) return null
        val colLetters = cellRef.substring(0, i)
        var col = 0
        for (ch in colLetters.uppercase()) {
            if (ch in 'A'..'Z') {
                col = col * 26 + (ch - 'A' + 1)
            }
        }
        return col - 1
    }

    private fun selectSheetHeaders(candidateRows: List<List<String>>): List<String> {
        if (candidateRows.isEmpty()) return emptyList()
        val multiCellRow = candidateRows.firstOrNull { row ->
            row.count { it.isNotBlank() } > 1
        }
        if (multiCellRow != null) {
            return multiCellRow
        }
        val mostFilledRow = candidateRows.maxByOrNull { row ->
            row.count { it.isNotBlank() }
        }
        return mostFilledRow ?: emptyList()
    }

    private fun parseSheetData(
        zip: ZipFile,
        sheetEntry: ZipEntry,
        sheetName: String,
        sharedStrings: List<String>,
        fileUri: String,
        fileName: String,
        onHeaderFound: (sheetName: String, headers: List<String>) -> Unit,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        val candidateHeaderRows = mutableListOf<List<String>>()

        zip.getInputStream(sheetEntry).use { stream ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(stream, "UTF-8")
            var event = parser.eventType

            var currentRowNum = 0
            val rowCells = mutableListOf<String>()
            var currentTargetColIndex: Int? = null
            var currentCellType: String? = null
            val cellTextBuffer = StringBuilder()
            var insideCell = false
            var insideIs = false

            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name
                        when {
                            name.equals("row", ignoreCase = true) || name.endsWith(":row") -> {
                                rowCells.clear()
                                val rAttr = parser.getAttributeValue(null, "r")
                                currentRowNum = rAttr?.toIntOrNull() ?: (currentRowNum + 1)
                            }
                            name.equals("c", ignoreCase = true) || name.endsWith(":c") -> {
                                insideCell = true
                                val rAttr = parser.getAttributeValue(null, "r")
                                currentTargetColIndex = extractColumnIndexFromRef(rAttr)
                                currentCellType = parser.getAttributeValue(null, "t")
                                cellTextBuffer.setLength(0)
                            }
                            insideCell && (name.equals("is", ignoreCase = true) || name.endsWith(":is")) -> {
                                insideIs = true
                            }
                            insideCell && (name.equals("t", ignoreCase = true) || name.endsWith(":t")) -> {
                                val text = parser.nextText()
                                cellTextBuffer.append(text)
                            }
                            insideCell && !insideIs && (name.equals("v", ignoreCase = true) || name.endsWith(":v")) -> {
                                val text = parser.nextText()
                                cellTextBuffer.append(text)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name
                        when {
                            name.equals("c", ignoreCase = true) || name.endsWith(":c") -> {
                                insideCell = false
                                insideIs = false
                                val rawVal = cellTextBuffer.toString()
                                val resolvedVal = if (currentCellType == "s") {
                                    val idx = rawVal.trim().toIntOrNull()
                                    if (idx != null && idx in 0 until sharedStrings.size) {
                                        sharedStrings[idx]
                                    } else {
                                        rawVal
                                    }
                                } else {
                                    rawVal
                                }

                                // Clean any internal newlines or tabs to protect table formatting
                                val cleanedVal = resolvedVal.replace("[\t\r\n]+".toRegex(), " ").trim()

                                val colIndex = currentTargetColIndex ?: rowCells.size
                                while (rowCells.size < colIndex) {
                                    rowCells.add("")
                                }
                                if (rowCells.size == colIndex) {
                                    rowCells.add(cleanedVal)
                                } else {
                                    rowCells[colIndex] = cleanedVal
                                }

                                cellTextBuffer.setLength(0)
                                currentCellType = null
                                currentTargetColIndex = null
                            }
                            name.equals("row", ignoreCase = true) || name.endsWith(":row") -> {
                                // Strip trailing empty cells only
                                while (rowCells.isNotEmpty() && rowCells.last().isEmpty()) {
                                    rowCells.removeAt(rowCells.size - 1)
                                }

                                if (rowCells.any { it.isNotEmpty() }) {
                                    if (candidateHeaderRows.size < 5) {
                                        candidateHeaderRows.add(rowCells.toList())
                                    }

                                    val rowText = rowCells.joinToString("\t")
                                    val normalized = ArabicNormalizer.normalize(rowText)
                                    if (normalized.isNotEmpty()) {
                                        onSegmentFound(
                                            FileSegment(
                                                fileUri = fileUri,
                                                fileName = fileName,
                                                location = "ورقة: $sheetName، صف: $currentRowNum",
                                                content = rowText,
                                                normalizedContent = normalized
                                            )
                                        )
                                    }
                                }
                                rowCells.clear()
                            }
                        }
                    }
                }
                event = parser.next()
            }
        }

        // Determine true column headers from the first candidate rows
        val headers = selectSheetHeaders(candidateHeaderRows)
        if (headers.isNotEmpty()) {
            onHeaderFound(sheetName, headers)
        }
    }
}
