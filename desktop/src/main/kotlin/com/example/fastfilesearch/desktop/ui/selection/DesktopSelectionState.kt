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
 * Tracks multi-cell and multi-row desktop selection, active keyboard-focus cell,
 * and Shift-selection anchor for Excel-like click/drag/arrow navigation.
 */
data class SelectionState(
    val mode: SelectionMode = SelectionMode.NONE,
    val selectedRows: Map<RowKey, SelectedRowData> = emptyMap(),
    val selectedCells: Map<CellKey, SelectedCellData> = emptyMap(),
    val activeCell: CellKey? = null,
    val anchorCell: CellKey? = null,
    val anchorRow: RowKey? = null
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
            anchorCell = cellData.key,
            anchorRow = null
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
            copy(
                mode = SelectionMode.CELLS,
                selectedRows = emptyMap(),
                selectedCells = newMap,
                activeCell = cellData.key,
                anchorCell = cellData.key,
                anchorRow = null
            )
        }
    }

    fun selectCellRange(
        rangeCells: List<SelectedCellData>,
        focusKey: CellKey,
        anchorKey: CellKey,
        additive: Boolean = false
    ): SelectionState {
        val newMap = if (additive && mode == SelectionMode.CELLS) {
            selectedCells.toMutableMap()
        } else {
            mutableMapOf()
        }
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
                anchorCell = anchorKey,
                anchorRow = null
            )
        }
    }

    fun selectSingleRow(rowData: SelectedRowData): SelectionState {
        return SelectionState(
            mode = SelectionMode.ROWS,
            selectedRows = mapOf(rowData.key to rowData),
            selectedCells = emptyMap(),
            activeCell = null,
            anchorCell = null,
            anchorRow = rowData.key
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
            copy(
                mode = SelectionMode.ROWS,
                selectedRows = newMap,
                selectedCells = emptyMap(),
                activeCell = null,
                anchorCell = null,
                anchorRow = rowData.key
            )
        }
    }

    fun selectRowRange(
        rangeRows: List<SelectedRowData>,
        focusKey: RowKey,
        anchorKey: RowKey,
        additive: Boolean = false
    ): SelectionState {
        val newMap = if (additive && mode == SelectionMode.ROWS) {
            selectedRows.toMutableMap()
        } else {
            mutableMapOf()
        }
        for (r in rangeRows) {
            newMap[r.key] = r
        }
        return if (newMap.isEmpty()) {
            SelectionState()
        } else {
            SelectionState(
                mode = SelectionMode.ROWS,
                selectedRows = newMap,
                selectedCells = emptyMap(),
                activeCell = null,
                anchorCell = null,
                anchorRow = anchorKey
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
            anchorCell = null,
            anchorRow = allRows.first().key
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
            anchorCell = null,
            anchorRow = rows.first().key
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
            anchorCell = allCells.first().key,
            anchorRow = null
        )
    }

    fun clear(): SelectionState = SelectionState()

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
