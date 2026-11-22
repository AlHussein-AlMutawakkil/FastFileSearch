package com.example.fastfilesearch.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.fastfilesearch.util.ArabicNormalizer

object TextHighlighter {

    /**
     * Highlights search terms inside the original text by matching normalized tokens,
     * so variations like (أ / إ / آ / ا, ة / ه, ى / ي, تشكيل) are accurately highlighted.
     */
    fun highlight(
        text: String,
        query: String,
        highlightColor: Color = Color(0xFFFFD54F),
        highlightTextColor: Color = Color(0xFF1A1A1A)
    ): AnnotatedString {
        if (text.isEmpty() || query.isBlank()) {
            return AnnotatedString(text)
        }

        val normalizedQuery = ArabicNormalizer.normalize(query)
        val queryTokens = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (queryTokens.isEmpty()) {
            return AnnotatedString(text)
        }

        // Tokenize original text into words while keeping track of original offsets
        val words = text.split(Regex("(?<=\\s)|(?=\\s)|(?<=[^\\p{L}\\p{Nd}])|(?=[^\\p{L}\\p{Nd}])"))
        val matchedRanges = mutableListOf<IntRange>()

        var currentOffset = 0
        for (word in words) {
            val length = word.length
            val normalizedWord = ArabicNormalizer.normalize(word)

            if (normalizedWord.isNotEmpty()) {
                val matches = queryTokens.any { token ->
                    normalizedWord.contains(token)
                }
                if (matches) {
                    matchedRanges.add(currentOffset until (currentOffset + length))
                }
            }
            currentOffset += length
        }

        return buildAnnotatedString {
            append(text)
            for (range in matchedRanges) {
                if (range.first in 0 until text.length && range.last < text.length) {
                    addStyle(
                        style = SpanStyle(
                            background = highlightColor,
                            color = highlightTextColor,
                            fontWeight = FontWeight.Bold
                        ),
                        start = range.first,
                        end = range.last + 1
                    )
                }
            }
        }
    }
}
