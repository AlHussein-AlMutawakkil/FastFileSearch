package com.example.fastfilesearch.desktop.ui

import com.example.fastfilesearch.desktop.util.ArabicNormalizer
import java.math.BigDecimal

enum class ColumnSortOrder {
    NONE,
    ASCENDING,
    DESCENDING
}

enum class TextFilterOperator(val label: String) {
    CONTAINS("يحتوي على"),
    NOT_CONTAINS("لا يحتوي على"),
    STARTS_WITH("يبدأ بـ"),
    ENDS_WITH("ينتهي بـ"),
    EQUALS("يساوي"),
    NOT_EQUALS("لا يساوي")
}

data class ColumnTextFilter(
    val operator: TextFilterOperator,
    val query: String
)

data class ColumnSortSpec(
    val colIndex: Int,
    val order: ColumnSortOrder
)

data class TableSortFilterState(
    val sort: ColumnSortSpec? = null,
    val activeFilters: Map<Int, Set<String>> = emptyMap(),
    val textFilters: Map<Int, ColumnTextFilter> = emptyMap()
) {
    fun getColumnSortOrder(colIndex: Int): ColumnSortOrder {
        val s = sort ?: return ColumnSortOrder.NONE
        return if (s.colIndex == colIndex) s.order else ColumnSortOrder.NONE
    }

    fun isColumnFiltered(colIndex: Int): Boolean =
        activeFilters.containsKey(colIndex) || textFilters.containsKey(colIndex)

    fun hasActiveConstraints(): Boolean {
        val sorted = sort != null && sort.order != ColumnSortOrder.NONE
        return sorted || activeFilters.isNotEmpty() || textFilters.isNotEmpty()
    }

    fun withSort(colIndex: Int, order: ColumnSortOrder): TableSortFilterState {
        return if (order == ColumnSortOrder.NONE) {
            copy(sort = null)
        } else {
            copy(sort = ColumnSortSpec(colIndex = colIndex, order = order))
        }
    }

    fun withColumnFilter(
        colIndex: Int,
        selectedValues: Set<String>,
        allUniqueValues: Set<String>
    ): TableSortFilterState {
        val next = activeFilters.toMutableMap()
        if (selectedValues.size >= allUniqueValues.size && selectedValues.containsAll(allUniqueValues)) {
            next.remove(colIndex)
        } else {
            next[colIndex] = selectedValues.toSet()
        }
        return copy(activeFilters = next)
    }

    fun withTextFilter(colIndex: Int, filter: ColumnTextFilter?): TableSortFilterState {
        val next = textFilters.toMutableMap()
        if (filter == null || filter.query.isBlank()) {
            next.remove(colIndex)
        } else {
            next[colIndex] = filter
        }
        return copy(textFilters = next)
    }

    fun clearColumn(colIndex: Int): TableSortFilterState {
        val nextFilters = activeFilters - colIndex
        val nextTextFilters = textFilters - colIndex
        val nextSort = if (sort?.colIndex == colIndex) null else sort
        return TableSortFilterState(sort = nextSort, activeFilters = nextFilters, textFilters = nextTextFilters)
    }
}

/**
 * Pure Excel-style column sorting (numeric + Arabic-normalized text with blanks always at bottom),
 * column-value checkbox filtering, and text-condition filtering helper.
 */
object DesktopTableSortFilterHelper {

    private val NUMERIC_PATTERN = Regex("^[+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)$")

    fun normalizeFilterKey(rawCell: String): String = rawCell.trim()

    fun formatValueLabel(filterKey: String): String =
        if (filterKey.isEmpty()) "(فارغ)" else filterKey

    fun parseNumericOrNull(raw: String): BigDecimal? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val normalizedDigits = buildString(trimmed.length) {
            for (ch in trimmed) {
                when (ch) {
                    in '٠'..'٩' -> append(('0'.code + (ch.code - '٠'.code)).toChar())
                    in '۰'..'۹' -> append(('0'.code + (ch.code - '۰'.code)).toChar())
                    ',', '٬', ' ' -> Unit
                    '٫' -> append('.')
                    else -> append(ch)
                }
            }
        }
        if (!NUMERIC_PATTERN.matches(normalizedDigits)) return null
        return runCatching { BigDecimal(normalizedDigits) }.getOrNull()
    }

    /**
     * Compares two raw cell strings according to [order].
     * Blank values ALWAYS sink to the bottom in both [ColumnSortOrder.ASCENDING] and [ColumnSortOrder.DESCENDING].
     */
    fun compareCells(a: String, b: String, order: ColumnSortOrder): Int {
        if (order == ColumnSortOrder.NONE) return 0
        val blankA = a.isBlank()
        val blankB = b.isBlank()
        if (blankA && blankB) return 0
        if (blankA) return 1
        if (blankB) return -1

        val numA = parseNumericOrNull(a)
        val numB = parseNumericOrNull(b)
        val baseCmp = when {
            numA != null && numB != null -> numA.compareTo(numB)
            numA != null -> -1
            numB != null -> 1
            else -> {
                val normA = ArabicNormalizer.normalize(a.trim())
                val normB = ArabicNormalizer.normalize(b.trim())
                val nCmp = normA.compareTo(normB)
                if (nCmp != 0) nCmp else a.trim().compareTo(b.trim())
            }
        }
        return if (order == ColumnSortOrder.DESCENDING) -baseCmp else baseCmp
    }

    /**
     * Extracts sorted unique values actually present in [colIndex].
     * Filters in other columns are respected so the dropdown cascades cleanly, while [colIndex]'s
     * own filter is excluded so the user can always re-enable hidden values.
     */
    fun <T> extractUniqueColumnValues(
        items: List<T>,
        colIndex: Int,
        state: TableSortFilterState,
        cellsExtractor: (T) -> List<String>
    ): List<String> {
        val otherValues = state.activeFilters - colIndex
        val otherText = state.textFilters - colIndex
        val distinctKeys = linkedSetOf<String>()
        for (item in items) {
            val cells = cellsExtractor(item)
            if (matchesFilters(cells, otherValues, otherText)) {
                distinctKeys += normalizeFilterKey(cells.getOrElse(colIndex) { "" })
            }
        }
        return distinctKeys.sortedWith { x, y -> compareCells(x, y, ColumnSortOrder.ASCENDING) }
    }

    fun extractUniqueColumnValues(
        rows: List<List<String>>,
        colIndex: Int,
        state: TableSortFilterState = TableSortFilterState()
    ): List<String> = extractUniqueColumnValues(rows, colIndex, state) { it }

    fun <T> applyFilterAndSort(
        items: List<T>,
        state: TableSortFilterState,
        cellsExtractor: (T) -> List<String>
    ): List<T> {
        val filtered = if (state.activeFilters.isEmpty() && state.textFilters.isEmpty()) {
            items
        } else {
            items.filter { item -> matchesFilters(cellsExtractor(item), state.activeFilters, state.textFilters) }
        }
        val sort = state.sort ?: return filtered
        if (sort.order == ColumnSortOrder.NONE) return filtered

        return filtered.sortedWith { left, right ->
            val leftVal = cellsExtractor(left).getOrElse(sort.colIndex) { "" }
            val rightVal = cellsExtractor(right).getOrElse(sort.colIndex) { "" }
            compareCells(leftVal, rightVal, sort.order)
        }
    }

    fun applyFilterAndSortRows(
        rows: List<List<String>>,
        state: TableSortFilterState
    ): List<List<String>> = applyFilterAndSort(rows, state) { it }

    private fun matchesTextCondition(cellValue: String, filter: ColumnTextFilter): Boolean {
        val q = ArabicNormalizer.normalize(filter.query.trim())
        if (q.isEmpty()) return true
        val norm = ArabicNormalizer.normalize(cellValue.trim())
        return when (filter.operator) {
            TextFilterOperator.CONTAINS -> norm.contains(q)
            TextFilterOperator.NOT_CONTAINS -> !norm.contains(q)
            TextFilterOperator.STARTS_WITH -> norm.startsWith(q)
            TextFilterOperator.ENDS_WITH -> norm.endsWith(q)
            TextFilterOperator.EQUALS -> norm == q
            TextFilterOperator.NOT_EQUALS -> norm != q
        }
    }

    private fun matchesFilters(
        cells: List<String>,
        filters: Map<Int, Set<String>>,
        textFilters: Map<Int, ColumnTextFilter>
    ): Boolean {
        for ((colIdx, allowed) in filters) {
            val valueKey = normalizeFilterKey(cells.getOrElse(colIdx) { "" })
            if (valueKey !in allowed) return false
        }
        for ((colIdx, tf) in textFilters) {
            val cellVal = cells.getOrElse(colIdx) { "" }
            if (!matchesTextCondition(cellVal, tf)) return false
        }
        return true
    }
}
