package com.example.fastfilesearch.desktop.ui.selection

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

/**
 * Tracks single or Ctrl-additive multi-cell and multi-row desktop selection,
 * plus active keyboard-focus cell for arrow navigation.
 */
data class SelectionState(
    val mode: SelectionMode = SelectionMode.NONE,
    val selectedRows: Map<RowKey, SelectedRowData> = emptyMap(),
    val selectedCells: Map<CellKey, SelectedCellData> = emptyMap(),
    val activeCell: CellKey? = null,
    val focusedRow: RowKey? = null
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
    fun isActiveCell(key: CellKey): Boolean = activeCell == key

    fun selectSingleCell(cellData: SelectedCellData): SelectionState {
        return SelectionState(
            mode = SelectionMode.CELLS,
            selectedRows = emptyMap(),
            selectedCells = mapOf(cellData.key to cellData),
            activeCell = cellData.key,
            focusedRow = null
        )
    }

    fun toggleCell(cellData: SelectedCellData): SelectionState {
        val newMap = selectedCells.toMutableMap()
        if (newMap.containsKey(cellData.key)) {
            newMap.remove(cellData.key)
        } else {
            newMap[cellData.key] = cellData
        }
        return if (newMap.isEmpty()) {
            SelectionState()
        } else {
            SelectionState(
                mode = SelectionMode.CELLS,
                selectedRows = emptyMap(),
                selectedCells = newMap,
                activeCell = if (newMap.containsKey(cellData.key)) cellData.key else newMap.keys.lastOrNull(),
                focusedRow = null
            )
        }
    }

    fun selectDraggedCellRange(
        rangeCells: List<SelectedCellData>,
        focusKey: CellKey,
        baseCells: Map<CellKey, SelectedCellData> = emptyMap(),
        additive: Boolean = false
    ): SelectionState {
        val newMap = if (additive) baseCells.toMutableMap() else mutableMapOf()
        for (c in rangeCells) {
            newMap[c.key] = c
        }
        return if (newMap.isEmpty()) {
            SelectionState()
        } else {
            SelectionState(
                mode = SelectionMode.CELLS,
                selectedRows = emptyMap(),
                selectedCells = newMap,
                activeCell = focusKey,
                focusedRow = null
            )
        }
    }

    fun selectSingleRow(rowData: SelectedRowData): SelectionState {
        return SelectionState(
            mode = SelectionMode.ROWS,
            selectedRows = mapOf(rowData.key to rowData),
            selectedCells = emptyMap(),
            activeCell = null,
            focusedRow = rowData.key
        )
    }

    fun toggleRow(rowData: SelectedRowData): SelectionState {
        val newMap = selectedRows.toMutableMap()
        if (newMap.containsKey(rowData.key)) {
            newMap.remove(rowData.key)
        } else {
            newMap[rowData.key] = rowData
        }
        return if (newMap.isEmpty()) {
            SelectionState()
        } else {
            SelectionState(
                mode = SelectionMode.ROWS,
                selectedRows = newMap,
                selectedCells = emptyMap(),
                activeCell = null,
                focusedRow = if (newMap.containsKey(rowData.key)) rowData.key else newMap.keys.lastOrNull()
            )
        }
    }

    fun selectAllRows(allRows: List<SelectedRowData>): SelectionState {
        if (allRows.isEmpty()) return SelectionState()
        val newMap = allRows.associateBy { it.key }
        return SelectionState(
            mode = SelectionMode.ROWS,
            selectedRows = newMap,
            selectedCells = emptyMap(),
            activeCell = null,
            focusedRow = allRows.first().key
        )
    }

    fun addRows(rows: List<SelectedRowData>): SelectionState {
        if (rows.isEmpty()) return this
        val newMap = if (mode == SelectionMode.ROWS) selectedRows.toMutableMap() else mutableMapOf()
        rows.forEach { newMap[it.key] = it }
        return SelectionState(
            mode = SelectionMode.ROWS,
            selectedRows = newMap,
            selectedCells = emptyMap(),
            activeCell = null,
            focusedRow = rows.first().key
        )
    }

    fun selectAllCells(allCells: List<SelectedCellData>): SelectionState {
        if (allCells.isEmpty()) return SelectionState()
        val newMap = allCells.associateBy { it.key }
        return SelectionState(
            mode = SelectionMode.CELLS,
            selectedRows = emptyMap(),
            selectedCells = newMap,
            activeCell = allCells.first().key,
            focusedRow = null
        )
    }

    fun clear(): SelectionState = SelectionState()

    fun shouldShowPreviewMenuItem(): Boolean {
        return (mode == SelectionMode.CELLS && count == 1) ||
            (mode == SelectionMode.ROWS && count == 1)
    }

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
