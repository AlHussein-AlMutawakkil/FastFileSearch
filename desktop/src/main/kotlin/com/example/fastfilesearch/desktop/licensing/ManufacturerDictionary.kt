package com.example.fastfilesearch.desktop.licensing

import java.util.Locale

/**
 * Shared 1-byte manufacturer dictionary (1..255).
 * 0 represents an unlisted / unknown manufacturer.
 * Must stay synchronized with :app and :license_admin.
 */
object ManufacturerDictionary {

    private data class Entry(
        val id: Int,
        val canonical: String,
        val aliases: List<String> = emptyList()
    )

    private val dictEntries = listOf(
        Entry(1, "Samsung", listOf("SAMSUNG ELECTRONICS")),
        Entry(2, "Xiaomi", listOf("XIAOMI COMMUNICATIONS")),
        Entry(3, "Redmi"),
        Entry(4, "POCO"),
        Entry(5, "Huawei", listOf("HUAWEI TECHNOLOGIES")),
        Entry(6, "HONOR", listOf("HONOR TECHNOLOGIES")),
        Entry(7, "OPPO"),
        Entry(8, "vivo"),
        Entry(9, "realme"),
        Entry(10, "TECNO", listOf("TECNO MOBILE")),
        Entry(11, "Infinix", listOf("INFINIX MOBILITY")),
        Entry(12, "itel"),
        Entry(13, "OnePlus"),
        Entry(14, "Google"),
        Entry(15, "Motorola", listOf("MOTOROLA MOBILITY", "MOTO")),
        Entry(16, "Nokia", listOf("HMD GLOBAL")),
        Entry(17, "Sony", listOf("SONY MOBILE", "SONY ERICSSON")),
        Entry(18, "Lenovo"),
        Entry(19, "Dell", listOf("DELL INC", "DELL INC.")),
        Entry(20, "HP", listOf("HEWLETT-PACKARD", "HEWLETT PACKARD", "HP INC")),
        Entry(21, "ASUS", listOf("ASUSTEK", "ASUSTEK COMPUTER", "ASUSTEK COMPUTER INC.")),
        Entry(22, "Acer", listOf("ACER INC")),
        Entry(23, "MSI", listOf("MICRO-STAR", "MICRO-STAR INTERNATIONAL", "MICRO-STAR INT'L")),
        Entry(24, "Toshiba", listOf("DYNABOOK")),
        Entry(25, "Apple"),
        Entry(26, "Nothing"),
        Entry(27, "ZTE"),
        Entry(28, "nubia"),
        Entry(29, "Meizu"),
        Entry(30, "HTC"),
        Entry(31, "LG", listOf("LGE", "LG ELECTRONICS")),
        Entry(32, "Microsoft", listOf("MICROSOFT CORPORATION")),
        Entry(33, "Alcatel"),
        Entry(34, "TCL"),
        Entry(35, "Sharp"),
        Entry(36, "Panasonic"),
        Entry(37, "Fujitsu"),
        Entry(38, "Gigabyte", listOf("GIGA-BYTE")),
        Entry(39, "Razer"),
        Entry(40, "BlackBerry", listOf("RIM")),
        Entry(41, "Black Shark", listOf("BLACKSHARK")),
        Entry(42, "iQOO"),
        Entry(43, "ROG"),
        Entry(44, "Doogee"),
        Entry(45, "Ulefone"),
        Entry(46, "OUKITEL"),
        Entry(47, "Blackview"),
        Entry(48, "Cubot"),
        Entry(49, "Umidigi"),
        Entry(50, "AGM"),
        Entry(51, "Cat", listOf("BULLITT")),
        Entry(52, "BLU"),
        Entry(53, "Lava"),
        Entry(54, "Micromax"),
        Entry(55, "Karbonn"),
        Entry(56, "HMD"),
        Entry(57, "Fairphone"),
        Entry(58, "Wiko"),
        Entry(59, "Archos"),
        Entry(60, "Hisense"),
        Entry(61, "ZUK"),
        Entry(62, "LeEco", listOf("LETV")),
        Entry(63, "Coolpad"),
        Entry(64, "Gionee"),
        Entry(65, "Chuwi"),
        Entry(66, "Teclast"),
        Entry(67, "Alldocube"),
        Entry(68, "BMAX"),
        Entry(69, "Framework"),
        Entry(70, "Alienware"),
        Entry(71, "VAIO"),
        Entry(72, "ASRock"),
        Entry(73, "Supermicro"),
        Entry(74, "Intel"),
        Entry(75, "AMD"),
        Entry(76, "Compaq"),
        Entry(77, "Gateway"),
        Entry(78, "Packard Bell"),
        Entry(79, "eMachines"),
        Entry(80, "Symphony"),
        Entry(81, "Walton"),
        Entry(82, "Condor"),
        Entry(83, "Unihertz"),
        Entry(84, "Smartisan"),
        Entry(85, "Essential"),
        Entry(86, "RedMagic", listOf("RED MAGIC")),
        Entry(87, "Meitu"),
        Entry(88, "InFocus"),
        Entry(89, "Kyocera"),
        Entry(90, "Casio")
    )

    private val idToName: Map<Int, String> = dictEntries.associate { it.id to it.canonical }

    private val lookupToId: Map<String, Int> = buildMap {
        for (entry in dictEntries) {
            put(normalizeKey(entry.canonical), entry.id)
            for (alias in entry.aliases) {
                put(normalizeKey(alias), entry.id)
            }
        }
    }

    private fun normalizeKey(raw: String): String {
        return raw.trim()
            .replace("\\s+".toRegex(), " ")
            .uppercase(Locale.ROOT)
    }

    fun findId(manufacturer: String?): Int {
        if (manufacturer.isNullOrBlank()) return 0
        val normalized = normalizeKey(manufacturer)
        return lookupToId[normalized] ?: 0
    }

    fun getName(id: Int): String {
        return idToName[id] ?: ""
    }

    fun size(): Int = dictEntries.size
}
