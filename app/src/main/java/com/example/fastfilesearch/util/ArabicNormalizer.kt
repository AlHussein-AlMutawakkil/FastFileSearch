package com.example.fastfilesearch.util

object ArabicNormalizer {

    /**
     * Normalizes Arabic text for uniform search matching:
     * - Strips all Tashkeel (Fatha, Damma, Kasra, Sukun, Shadda, Tanween, etc.)
     * - Strips Tatweel (Kashida 'ـ')
     * - Normalizes Alif variants (أ, إ, آ, ٱ) -> ا
     * - Normalizes Alef Maksura (ى) -> ي
     * - Normalizes Ta Marbuta (ة) -> ه
     * - Normalizes Persian/Urdu variants (ک -> ك, ی -> ي)
     * - Converts Eastern Arabic-Indic numerals (٠..٩) and Persian numerals (۰..۹) to ASCII digits (0..9)
     * - Converts to lowercase and normalizes whitespace
     */
    fun normalize(text: String): String {
        if (text.isEmpty()) return ""

        val sb = StringBuilder(text.length)
        var lastWasSpace = false

        for (char in text) {
            when (char) {
                // Diacritics (Harakat) & Tatweel -> Skip
                '\u064B', // Fathatan
                '\u064C', // Dammatan
                '\u064D', // Kasratan
                '\u064E', // Fatha
                '\u064F', // Damma
                '\u0650', // Kasra
                '\u0651', // Shadda
                '\u0652', // Sukun
                '\u0653', // Maddah above
                '\u0654', // Hamza above
                '\u0655', // Hamza below
                '\u0670', // Superscript Alef
                '\u0640'  // Tatweel / Kashida
                -> {
                    // Do nothing (strip)
                }

                // Alif variants -> Bare Alif
                '\u0622', // آ
                '\u0623', // أ
                '\u0625', // إ
                '\u0671'  // ٱ
                -> {
                    sb.append('\u0627')
                    lastWasSpace = false
                }

                // Alef Maksura -> Ya
                '\u0649' // ى
                -> {
                    sb.append('\u064A') // ي
                    lastWasSpace = false
                }

                // Ta Marbuta -> Ha
                '\u0629' // ة
                -> {
                    sb.append('\u0647') // ه
                    lastWasSpace = false
                }

                // Persian/Urdu variants
                '\u06A9' // ک -> ك
                -> {
                    sb.append('\u0643')
                    lastWasSpace = false
                }
                '\u06CC' // ی -> ي
                -> {
                    sb.append('\u064A')
                    lastWasSpace = false
                }

                // Eastern Arabic-Indic numerals (٠..٩) -> 0..9
                in '\u0660'..'\u0669' -> {
                    sb.append(('0'.code + (char - '\u0660')).toChar())
                    lastWasSpace = false
                }

                // Persian/Urdu numerals (۰..۹) -> 0..9
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
