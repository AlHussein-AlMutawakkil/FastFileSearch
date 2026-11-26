package com.example.fastfilesearch.desktop.parser

import com.example.fastfilesearch.desktop.model.FileSegment
import java.io.File
import java.util.Locale

object DesktopFileParserDispatcher {

    val SUPPORTED_EXTENSIONS = setOf(
        "xlsx", "xls", "docx", "doc", "pdf", "mdb", "accdb", "txt", "csv", "json", "md", "log"
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

    fun parseFile(
        file: File,
        fileUri: String = file.absolutePath,
        fileName: String = file.name,
        onHeaderFound: (sheetName: String, headers: List<String>) -> Unit = { _, _ -> },
        onSegmentFound: (FileSegment) -> Unit
    ) {
        val ext = getExtension(fileName)
        when (ext) {
            "xlsx", "xls" -> DesktopExcelParser.parseFile(file, fileUri, fileName, onHeaderFound, onSegmentFound)
            "docx", "doc" -> DesktopWordParser.parseFile(file, fileUri, fileName, onSegmentFound)
            "pdf" -> DesktopPdfParser.parseFile(file, fileUri, fileName, onSegmentFound)
            "mdb", "accdb" -> DesktopAccessParser.parseFile(file, fileUri, fileName, onHeaderFound, onSegmentFound)
            "txt", "csv", "json", "md", "log" -> DesktopTextParser.parseFile(
                file = file,
                fileUri = fileUri,
                fileName = fileName,
                onHeaderFound = onHeaderFound,
                onSegmentFound = onSegmentFound
            )
        }
    }
}
