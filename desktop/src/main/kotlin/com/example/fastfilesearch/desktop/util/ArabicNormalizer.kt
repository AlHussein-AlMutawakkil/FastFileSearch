package com.example.fastfilesearch.desktop.util

object ArabicNormalizer {

    fun normalize(text: String): String {
        if (text.isEmpty()) return ""

        val sb = StringBuilder(text.length)
        var lastWasSpace = false

        for (char in text) {
            when (char) {
                '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650',
                '\u0651', '\u0652', '\u0653', '\u0654', '\u0655', '\u0670', '\u0640' -> {
                    // Strip Arabic diacritics & Tatweel
                }

                '\u0622', '\u0623', '\u0625', '\u0671' -> {
                    sb.append('\u0627')
                    lastWasSpace = false
                }

                '\u0649' -> {
                    sb.append('\u064A')
                    lastWasSpace = false
                }

                '\u0629' -> {
                    sb.append('\u0647')
                    lastWasSpace = false
                }

                '\u06A9' -> {
                    sb.append('\u0643')
                    lastWasSpace = false
                }

                '\u06CC' -> {
                    sb.append('\u064A')
                    lastWasSpace = false
                }

                in '\u0660'..'\u0669' -> {
                    sb.append(('0'.code + (char - '\u0660')).toChar())
                    lastWasSpace = false
                }

                in '\u06F0'..'\u06F9' -> {
                    sb.append(('0'.code + (char - '\u06F0')).toChar())
                    lastWasSpace = false
                }

                else -> {
                    if (char.isWhitespace()) {
                        if (!lastWasSpace && sb.isNotEmpty()) {
                            sb.append(' ')
                            lastWasSpace = true
                        }
                    } else {
                        sb.append(char.lowercaseChar())
                        lastWasSpace = false
                    }
                }
            }
        }

        return sb.toString().trim()
    }
}
