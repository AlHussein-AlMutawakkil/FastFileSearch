package com.example.fastfilesearch.desktop.ui.selection

import com.example.fastfilesearch.desktop.model.FileResultGroup
import com.example.fastfilesearch.desktop.model.SearchResult
import com.example.fastfilesearch.desktop.ui.DesktopTableCellHelper

enum class GridNavDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

data class SheetGridModel(
    val fileUri: String,
    val fileName: String,
    val sheetName: String,
    val headers: List<String>,
    val rowNumbers: List<String>,
    val rows: List<List<String>>,
    val results: List<SearchResult>,
    val maxCols: Int
) {
    val rowCount: Int get() = rows.size

    fun buildCellData(rowIndex: Int, colIndex: Int): SelectedCellData? {
        if (rowIndex !in 0 until rowCount || colIndex !in 0 until maxCols) return null
        val rowNum = rowNumbers.getOrElse(rowIndex) { (rowIndex + 1).toString() }
        val colName = headers.getOrNull(colIndex)?.takeIf { it.isNotBlank() } ?: "عمود ${colIndex + 1}"
        val cellVal = rows[rowIndex].getOrNull(colIndex) ?: ""
        return SelectedCellData(
            key = CellKey(fileUri = fileUri, sheetName = sheetName, rowIndex = rowIndex, colIndex = colIndex),
            fileName = fileName,
            sheetName = sheetName,
            rowNumber = rowNum,
            columnName = colName,
            value = cellVal
        )
    }

    fun buildCellRange(anchor: CellKey, target: CellKey): List<SelectedCellData> {
        val minRow = minOf(anchor.rowIndex, target.rowIndex).coerceIn(0, (rowCount - 1).coerceAtLeast(0))
        val maxRow = maxOf(anchor.rowIndex, target.rowIndex).coerceIn(0, (rowCount - 1).coerceAtLeast(0))
        val minCol = minOf(anchor.colIndex, target.colIndex).coerceIn(0, (maxCols - 1).coerceAtLeast(0))
        val maxCol = maxOf(anchor.colIndex, target.colIndex).coerceIn(0, (maxCols - 1).coerceAtLeast(0))

        val list = ArrayList<SelectedCellData>((maxRow - minRow + 1) * (maxCol - minCol + 1))
        for (r in minRow..maxRow) {
            for (c in minCol..maxCol) {
                buildCellData(r, c)?.let { list.add(it) }
            }
        }
        return list
    }

    fun buildRowData(rowIndex: Int): SelectedRowData? {
        if (rowIndex !in 0 until rowCount) return null
        val rowNum = rowNumbers.getOrElse(rowIndex) { (rowIndex + 1).toString() }
        return SelectedRowData(
            key = RowKey(fileUri = fileUri, sheetName = sheetName, rowIndex = rowIndex),
            fileName = fileName,
            sheetName = sheetName,
            rowNumber = rowNum,
            cells = rows[rowIndex],
            headers = headers,
            isDocument = false
        )
    }

    fun buildRowRange(anchor: RowKey, target: RowKey): List<SelectedRowData> {
        val minRow = minOf(anchor.rowIndex, target.rowIndex).coerceIn(0, (rowCount - 1).coerceAtLeast(0))
        val maxRow = maxOf(anchor.rowIndex, target.rowIndex).coerceIn(0, (rowCount - 1).coerceAtLeast(0))
        val list = ArrayList<SelectedRowData>(maxRow - minRow + 1)
        for (r in minRow..maxRow) {
            buildRowData(r)?.let { list.add(it) }
        }
        return list
    }

    fun buildAllCells(): List<SelectedCellData> {
        val list = ArrayList<SelectedCellData>(rowCount * maxCols)
        for (r in 0 until rowCount) {
            for (c in 0 until maxCols) {
                buildCellData(r, c)?.let { list.add(it) }
            }
        }
        return list
    }
}

/**
 * Pure helper for building desktop sheet models and handling RTL-aware keyboard arrow navigation.
 */
object DesktopGridNavigator {

    fun buildSheetModelsForGroup(
        group: FileResultGroup,
        showAllRows: Boolean,
        expandedSheets: Set<String> = emptySet()
    ): List<SheetGridModel> {
        if (group.isDocument) return emptyList()
        val sheetGroups = group.results.groupBy { res ->
            res.sheetName ?: DesktopTableCellHelper.extractSheetAndRow(res.location).first
        }
        return sheetGroups.map { (sheetName, sheetResults) ->
            val sorted = sheetResults.sortedBy { res ->
                res.rowNumber ?: DesktopTableCellHelper.extractSheetAndRow(res.location).second.toIntOrNull() ?: 0
            }
            val headers = sorted.firstOrNull { it.columnHeaders.isNotEmpty() }?.columnHeaders ?: emptyList()
            val sheetId = "${group.fileUri}::$sheetName"
            val visible = if (showAllRows || sheetId in expandedSheets || sorted.size <= 30) {
                sorted
            } else {
                sorted.take(30)
            }
            val rowNums = visible.map { res ->
                res.rowNumber?.toString() ?: DesktopTableCellHelper.extractSheetAndRow(res.location).second
            }
            val parsedRows = visible.map { res ->
                DesktopTableCellHelper.splitIntoCells(res.snippet)
            }
            val maxCols = maxOf(parsedRows.maxOfOrNull { it.size } ?: 1, headers.size).coerceAtLeast(1)

            SheetGridModel(
                fileUri = group.fileUri,
                fileName = group.fileName,
                sheetName = sheetName,
                headers = headers,
                rowNumbers = rowNums,
                rows = parsedRows,
                results = visible,
                maxCols = maxCols
            )
        }
    }

    fun navigateArrow(
        sheets: List<SheetGridModel>,
        currentState: SelectionState,
        direction: GridNavDirection,
        isShiftPressed: Boolean,
        isRtl: Boolean = true
    ): SelectionState {
        if (sheets.isEmpty()) return currentState

        val currentKey = currentState.activeCell
        val targetSheet = if (currentKey != null) {
            sheets.firstOrNull { it.fileUri == currentKey.fileUri && it.sheetName == currentKey.sheetName }
        } else {
            null
        } ?: sheets.firstOrNull { it.rowCount > 0 && it.maxCols > 0 } ?: return currentState

        if (targetSheet.rowCount <= 0 || targetSheet.maxCols <= 0) return currentState

        if (currentKey == null || currentKey.fileUri != targetSheet.fileUri || currentKey.sheetName != targetSheet.sheetName) {
            val firstCell = targetSheet.buildCellData(0, 0) ?: return currentState
            return currentState.selectSingleCell(firstCell)
        }

        val curRow = currentKey.rowIndex.coerceIn(0, targetSheet.rowCount - 1)
        val curCol = currentKey.colIndex.coerceIn(0, targetSheet.maxCols - 1)

        var nextRow = curRow
        var nextCol = curCol

        when (direction) {
            GridNavDirection.UP -> nextRow = (curRow - 1).coerceAtLeast(0)
            GridNavDirection.DOWN -> nextRow = (curRow + 1).coerceAtMost(targetSheet.rowCount - 1)
            GridNavDirection.RIGHT -> {
                nextCol = if (isRtl) {
                    (curCol - 1).coerceAtLeast(0)
                } else {
                    (curCol + 1).coerceAtMost(targetSheet.maxCols - 1)
                }
            }
            GridNavDirection.LEFT -> {
                nextCol = if (isRtl) {
                    (curCol + 1).coerceAtMost(targetSheet.maxCols - 1)
                } else {
                    (curCol - 1).coerceAtLeast(0)
                }
            }
        }

        val nextKey = CellKey(
            fileUri = targetSheet.fileUri,
            sheetName = targetSheet.sheetName,
            rowIndex = nextRow,
            colIndex = nextCol
        )

        return if (isShiftPressed) {
            val anchor = currentState.anchorCell
                ?.takeIf { it.fileUri == targetSheet.fileUri && it.sheetName == targetSheet.sheetName }
                ?: currentKey
            val rangeCells = targetSheet.buildCellRange(anchor, nextKey)
            currentState.selectCellRange(
                rangeCells = rangeCells,
                focusKey = nextKey,
                anchorKey = anchor,
                additive = false
            )
        } else {
            val cellData = targetSheet.buildCellData(nextRow, nextCol) ?: return currentState
            currentState.selectSingleCell(cellData)
        }
    }
}
