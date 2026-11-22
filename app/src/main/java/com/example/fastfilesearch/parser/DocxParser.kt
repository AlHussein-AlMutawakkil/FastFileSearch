package com.example.fastfilesearch.parser

import android.util.Xml
import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.util.ArabicNormalizer
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

object DocxParser {

    /**
     * Extracts text from Word .docx files by reading word/document.xml directly
     * using streaming XmlPullParser, avoiding memory-heavy libraries.
     *
     * In Word OpenXML, a single word (especially in Arabic) is often broken across multiple
     * <w:t> tags within the same paragraph run. Therefore, <w:t> content is appended directly
     * without inserting extra spaces. Spaces are only inserted on explicit tab (<w:tab/>)
     * or line-break (<w:br/>, <w:cr/>) tags.
     */
    fun parse(
        inputStream: InputStream,
        fileUri: String,
        fileName: String,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    parseDocumentXml(zis, fileUri, fileName, onSegmentFound)
                    break
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun parseDocumentXml(
        stream: InputStream,
        fileUri: String,
        fileName: String,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setInput(stream, "UTF-8")

        var eventType = parser.eventType
        var paragraphIndex = 0
        var insideParagraph = false
        val currentParagraphText = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    when {
                        name.equals("p", ignoreCase = true) || name.endsWith(":p") -> {
                            insideParagraph = true
                            paragraphIndex++
                            currentParagraphText.setLength(0)
                        }
                        insideParagraph && (name.equals("t", ignoreCase = true) || name.endsWith(":t")) -> {
                            val text = parser.nextText()
                            if (text.isNotEmpty()) {
                                currentParagraphText.append(text)
                            }
                        }
                        insideParagraph && (name.equals("tab", ignoreCase = true) || name.endsWith(":tab")) -> {
                            currentParagraphText.append(" ")
                        }
                        insideParagraph && (name.equals("br", ignoreCase = true) || name.endsWith(":br") ||
                                name.equals("cr", ignoreCase = true) || name.endsWith(":cr")) -> {
                            currentParagraphText.append(" ")
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name
                    if (name.equals("p", ignoreCase = true) || name.endsWith(":p")) {
                        insideParagraph = false
                        val text = currentParagraphText.toString().trim()
                        if (text.isNotEmpty()) {
                            val normalized = ArabicNormalizer.normalize(text)
                            if (normalized.isNotEmpty()) {
                                onSegmentFound(
                                    FileSegment(
                                        fileUri = fileUri,
                                        fileName = fileName,
                                        location = "فقرة $paragraphIndex",
                                        content = text,
                                        normalizedContent = normalized
                                    )
                                )
                            }
                        }
                        currentParagraphText.setLength(0)
                    }
                }
            }
            eventType = parser.next()
        }
    }
}
