package com.example.fastfilesearch.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.fastfilesearch.model.SearchMethod
import com.example.fastfilesearch.util.ArabicNormalizer

object TextHighlighter {

    private val WHITESPACE_REGEX = "\\s+".toRegex()
    private val TOKENIZE_REGEX = Regex("(?<=\\s)|(?=\\s)|(?<=[^\\p{L}\\p{Nd}])|(?=[^\\p{L}\\p{Nd}])")

    private data class WordInfo(
        val startOffset: Int,
        val length: Int,
        val text: String,
        val normalized: String
    )

    /**
     * Highlights search terms inside the original text based on [SearchMethod] and [ignoreDiacritics].
     */
    fun highlight(
        text: String,
        query: String,
        method: SearchMethod = SearchMethod.CONTAINS,
        ignoreDiacritics: Boolean = true,
        highlightColor: Color = Color(0xFFFFD54F),
        highlightTextColor: Color = Color(0xFF1A1A1A)
    ): AnnotatedString {
        if (text.isEmpty() || query.isBlank()) {
            return AnnotatedString(text)
        }

        val rawQueryTokens = query.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        if (rawQueryTokens.isEmpty()) {
            return AnnotatedString(text)
        }

        val queryTokens = if (ignoreDiacritics) {
            ArabicNormalizer.normalize(query).split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        } else {
            rawQueryTokens
        }

        if (queryTokens.isEmpty()) {
            return AnnotatedString(text)
        }

        val matchedRanges = mutableListOf<IntRange>()

        // Split text into tokens preserving original character offsets
        val words = text.split(TOKENIZE_REGEX)
        val wordInfos = mutableListOf<WordInfo>()
        var currentOffset = 0
        for (word in words) {
            val len = word.length
            val norm = if (ignoreDiacritics) ArabicNormalizer.normalize(word) else word
            wordInfos.add(WordInfo(currentOffset, len, word, norm))
            currentOffset += len
        }

        when (method) {
            SearchMethod.EXACT_PHRASE -> {
                val meaningful = wordInfos.filter { it.normalized.isNotBlank() }
                if (meaningful.size >= queryTokens.size) {
                    for (i in 0..(meaningful.size - queryTokens.size)) {
                        val sub = (0 until queryTokens.size).map { meaningful[i + it].normalized }
                        val matches = if (ignoreDiacritics) {
                            sub == queryTokens
                        } else {
                            sub.map { it.trim() } == queryTokens.map { it.trim() }
                        }
                        if (matches) {
                            val startChar = meaningful[i].startOffset
                            val lastWord = meaningful[i + queryTokens.size - 1]
                            val endChar = lastWord.startOffset + lastWord.length
                            matchedRanges.add(startChar until endChar)
                        }
                    }
                }
            }

            SearchMethod.WHOLE_WORD -> {
                for (info in wordInfos) {
                    if (info.normalized.isNotBlank()) {
                        val matches = if (ignoreDiacritics) {
                            queryTokens.any { it == info.normalized }
                        } else {
                            queryTokens.any { it == info.text }
                        }
                        if (matches) {
                            matchedRanges.add(info.startOffset until (info.startOffset + info.length))
                        }
                    }
                }
            }

            SearchMethod.CONTAINS, SearchMethod.ALL_WORDS -> {
                for (info in wordInfos) {
                    if (info.normalized.isNotBlank()) {
                        val matches = if (ignoreDiacritics) {
                            queryTokens.any { info.normalized.contains(it) }
                        } else {
                            queryTokens.any { info.text.contains(it) }
                        }
                        if (matches) {
                            matchedRanges.add(info.startOffset until (info.startOffset + info.length))
                        }
                    }
                }
            }
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
