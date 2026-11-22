package com.example.fastfilesearch.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.fastfilesearch.parser.FileParserDispatcher

data class FileTypeVisual(
    val icon: ImageVector,
    val color: Color,
    val typeName: String
)

object FileTypeHelper {

    fun getVisual(fileName: String): FileTypeVisual {
        val ext = FileParserDispatcher.getExtension(fileName)
        return when (ext) {
            "xlsx", "xls" -> FileTypeVisual(
                icon = Icons.Default.TableChart,
                color = Color(0xFF1E7E34), // Excel Green
                typeName = if (ext == "xlsx") "Excel (XLSX)" else "Excel (XLS)"
            )
            "docx" -> FileTypeVisual(
                icon = Icons.Default.Description,
                color = Color(0xFF185ABD), // Word Blue
                typeName = "Word (DOCX)"
            )
            "pdf" -> FileTypeVisual(
                icon = Icons.Default.PictureAsPdf,
                color = Color(0xFFD32F2F), // PDF Red
                typeName = "PDF"
            )
            "txt", "log" -> FileTypeVisual(
                icon = Icons.AutoMirrored.Filled.Article,
                color = Color(0xFF455A64), // Text Slate
                typeName = ext.uppercase()
            )
            "csv" -> FileTypeVisual(
                icon = Icons.Default.TableChart,
                color = Color(0xFF00796B), // Teal
                typeName = "CSV"
            )
            "json" -> FileTypeVisual(
                icon = Icons.Default.Code,
                color = Color(0xFFE65100), // Orange
                typeName = "JSON"
            )
            "md" -> FileTypeVisual(
                icon = Icons.AutoMirrored.Filled.Article,
                color = Color(0xFF5E35B1), // Purple
                typeName = "Markdown"
            )
            else -> FileTypeVisual(
                icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                color = Color(0xFF757575),
                typeName = ext.uppercase()
            )
        }
    }
}

