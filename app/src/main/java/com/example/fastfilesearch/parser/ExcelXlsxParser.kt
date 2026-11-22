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
     * OpenXML Zip + XmlPullParser implementation that does not depend on StAX (javax.xml.stream)
     * which is absent from Android's runtime.
     */
    fun parse(
        context: Context,
        inputStream: InputStream,
        fileUri: String,
        fileName: String,
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
                        onSegmentFound = onSegmentFound
                    )
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error parsing xlsx file: $fileName", t)
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

    private fun parseSheetData(
        zip: ZipFile,
        sheetEntry: ZipEntry,
        sheetName: String,
        sharedStrings: List<String>,
        fileUri: String,
        fileName: String,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        try {
            zip.getInputStream(sheetEntry).use { stream ->
                val parser: XmlPullParser = Xml.newPullParser()
                parser.setInput(stream, "UTF-8")
                var event = parser.eventType

                var currentRowNum = 0
                val rowCells = mutableListOf<String>()
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
                                    val rawVal = cellTextBuffer.toString().trim()
                                    if (rawVal.isNotEmpty()) {
                                        val resolvedVal = if (currentCellType == "s") {
                                            val idx = rawVal.toIntOrNull()
                                            if (idx != null && idx in 0 until sharedStrings.size) {
                                                sharedStrings[idx]
                                            } else {
                                                rawVal
                                            }
                                        } else {
                                            rawVal
                                        }

                                        if (resolvedVal.isNotBlank()) {
                                            rowCells.add(resolvedVal)
                                        }
                                    }
                                    cellTextBuffer.setLength(0)
                                    currentCellType = null
                                }
                                name.equals("row", ignoreCase = true) || name.endsWith(":row") -> {
                                    if (rowCells.isNotEmpty()) {
                                        val rowText = rowCells.joinToString(" | ")
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
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing sheet ${sheetEntry.name}", e)
        }
    }
}
