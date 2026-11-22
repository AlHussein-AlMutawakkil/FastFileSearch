package com.example.fastfilesearch.parser

import android.content.Context
import com.example.fastfilesearch.model.FileSegment
import java.io.InputStream
import java.util.Locale

object FileParserDispatcher {

    private val SUPPORTED_EXTENSIONS = setOf(
        "xlsx", "xls", "docx", "pdf", "txt", "csv", "json", "md", "log"
    )

    fun isSupportedFile(fileName: String): Boolean {
        if (fileName.startsWith("~$")) return false
        val ext = getExtension(fileName)
        return SUPPORTED_EXTENSIONS.contains(ext)
    }

    fun getExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot != -1 && lastDot < fileName.length - 1) {
            fileName.substring(lastDot + 1).lowercase(Locale.ROOT)
        } else {
            ""
        }
    }

    /**
     * Parses the given stream based on its file extension and invokes [onSegmentFound] for every extracted segment,
     * and [onHeaderFound] when sheet column headers are determined.
     */
    fun parseFile(
        context: Context,
        inputStream: InputStream,
        fileUri: String,
        fileName: String,
        onHeaderFound: (sheetName: String, headers: List<String>) -> Unit = { _, _ -> },
        onSegmentFound: (FileSegment) -> Unit
    ) {
        val ext = getExtension(fileName)
        when (ext) {
            "xlsx" -> ExcelXlsxParser.parse(context, inputStream, fileUri, fileName, onHeaderFound, onSegmentFound)
            "xls" -> ExcelXlsParser.parse(inputStream, fileUri, fileName, onHeaderFound, onSegmentFound)
            "docx" -> DocxParser.parse(inputStream, fileUri, fileName, onSegmentFound)
            "pdf" -> PdfParser.parse(context, inputStream, fileUri, fileName, onSegmentFound)
            "txt", "csv", "json", "md", "log" -> TextFileParser.parse(inputStream, fileUri, fileName, onSegmentFound)
            else -> {
                // Unsupported format, skip safely
            }
        }
    }
}
