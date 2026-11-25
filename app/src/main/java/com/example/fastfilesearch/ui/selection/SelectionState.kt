package com.example.fastfilesearch.ui.selection

enum class SelectionMode {
    NONE,
    ROWS,
    CELLS
}

data class RowKey(
    val fileUri: String,
    val sheetName: String,
    val rowIndex: Int
)

data class CellKey(
    val fileUri: String,
    val sheetName: String,
    val rowIndex: Int,
    val colIndex: Int
)

data class SelectedRowData(
    val key: RowKey,
    val fileName: String,
    val sheetName: String,
    val rowNumber: String,
    val cells: List<String>,
    val headers: List<String>,
    val isDocument: Boolean = false,
    val snippet: String = "",
    val location: String = ""
)

data class SelectedCellData(
    val key: CellKey,
    val fileName: String,
    val sheetName: String,
    val rowNumber: String,
    val columnName: String,
    val value: String
)

data class SelectionState(
    val mode: SelectionMode = SelectionMode.NONE,
    val selectedRows: Map<RowKey, SelectedRowData> = emptyMap(),
    val selectedCells: Map<CellKey, SelectedCellData> = emptyMap()
) {
    val count: Int
        get() = when (mode) {
            SelectionMode.ROWS -> selectedRows.size
            SelectionMode.CELLS -> selectedCells.size
            SelectionMode.NONE -> 0
        }

    val isActive: Boolean
        get() = mode != SelectionMode.NONE && count > 0

    fun isRowSelected(key: RowKey): Boolean = selectedRows.containsKey(key)
    fun isCellSelected(key: CellKey): Boolean = selectedCells.containsKey(key)

    fun toggleRow(rowData: SelectedRowData): SelectionState {
        val newMap = selectedRows.toMutableMap()
        if (newMap.containsKey(rowData.key)) {
            newMap.remove(rowData.key)
        } else {
            newMap[rowData.key] = rowData
        }
        return if (newMap.isEmpty()) {
            SelectionState(SelectionMode.NONE)
        } else {
            copy(mode = SelectionMode.ROWS, selectedRows = newMap, selectedCells = emptyMap())
        }
    }

    fun toggleCell(cellData: SelectedCellData): SelectionState {
        val newMap = selectedCells.toMutableMap()
        if (newMap.containsKey(cellData.key)) {
            newMap.remove(cellData.key)
        } else {
            newMap[cellData.key] = cellData
        }
        return if (newMap.isEmpty()) {
            SelectionState(SelectionMode.NONE)
        } else {
            copy(mode = SelectionMode.CELLS, selectedCells = newMap, selectedRows = emptyMap())
        }
    }

    fun selectAllRows(allRows: List<SelectedRowData>): SelectionState {
        if (allRows.isEmpty()) return SelectionState(SelectionMode.NONE)
        val newMap = allRows.associateBy { it.key }
        return copy(mode = SelectionMode.ROWS, selectedRows = newMap, selectedCells = emptyMap())
    }

    fun addRows(rows: List<SelectedRowData>): SelectionState {
        if (rows.isEmpty()) return this
        val newMap = if (mode == SelectionMode.ROWS) selectedRows.toMutableMap() else mutableMapOf()
        rows.forEach { newMap[it.key] = it }
        return copy(mode = SelectionMode.ROWS, selectedRows = newMap, selectedCells = emptyMap())
    }

    fun selectAllCells(allCells: List<SelectedCellData>): SelectionState {
        if (allCells.isEmpty()) return SelectionState(SelectionMode.NONE)
        val newMap = allCells.associateBy { it.key }
        return copy(mode = SelectionMode.CELLS, selectedCells = newMap, selectedRows = emptyMap())
    }

    fun clear(): SelectionState = SelectionState(SelectionMode.NONE)

    fun getFormattedCountArabic(): String {
        return when (mode) {
            SelectionMode.ROWS -> {
                when (count) {
                    1 -> "صف واحد"
                    2 -> "صفان"
                    in 3..10 -> "$count صفوف"
                    else -> "$count صفاً"
                }
            }
            SelectionMode.CELLS -> {
                when (count) {
                    1 -> "خلية واحدة"
                    2 -> "خليتان"
                    in 3..10 -> "$count خلايا"
                    else -> "$count خلية"
                }
            }
            SelectionMode.NONE -> ""
        }
    }
}
