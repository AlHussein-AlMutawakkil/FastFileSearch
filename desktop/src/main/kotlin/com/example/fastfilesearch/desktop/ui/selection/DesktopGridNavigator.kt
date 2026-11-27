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
            val rowNums = sorted.map { res ->
                res.rowNumber?.toString() ?: DesktopTableCellHelper.extractSheetAndRow(res.location).second
            }
            val parsedRows = sorted.map { res ->
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
                results = sorted,
                maxCols = maxCols
            )
        }
    }

    fun navigateArrow(
        sheets: List<SheetGridModel>,
        currentState: SelectionState,
        direction: GridNavDirection,
        isRtl: Boolean = true
    ): SelectionState {
        if (sheets.isEmpty()) return currentState

        val nonEmptySheets = sheets.filter { it.rowCount > 0 && it.maxCols > 0 }
        if (nonEmptySheets.isEmpty()) return currentState

        // Support navigating directly from Row-Number selection mode cleanly
        if (currentState.activeCell == null && currentState.mode == SelectionMode.ROWS && currentState.focusedRow != null) {
            val focused = currentState.focusedRow
            val sheetIdx = nonEmptySheets.indexOfFirst { it.fileUri == focused.fileUri && it.sheetName == focused.sheetName }
            if (sheetIdx >= 0) {
                val targetSheet = nonEmptySheets[sheetIdx]
                val curRow = focused.rowIndex.coerceIn(0, targetSheet.rowCount - 1)
                when (direction) {
                    GridNavDirection.UP -> {
                        if (curRow > 0) {
                            targetSheet.buildRowData(curRow - 1)?.let { return currentState.selectSingleRow(it) }
                        } else if (sheetIdx > 0) {
                            val prev = nonEmptySheets[sheetIdx - 1]
                            prev.buildRowData(prev.rowCount - 1)?.let { return currentState.selectSingleRow(it) }
                        }
                        return currentState
                    }
                    GridNavDirection.DOWN -> {
                        if (curRow < targetSheet.rowCount - 1) {
                            targetSheet.buildRowData(curRow + 1)?.let { return currentState.selectSingleRow(it) }
                        } else if (sheetIdx < nonEmptySheets.lastIndex) {
                            val next = nonEmptySheets[sheetIdx + 1]
                            next.buildRowData(0)?.let { return currentState.selectSingleRow(it) }
                        }
                        return currentState
                    }
                    GridNavDirection.RIGHT, GridNavDirection.LEFT -> {
                        val entryCol = if (direction == GridNavDirection.LEFT && isRtl) 0 else if (!isRtl && direction == GridNavDirection.RIGHT) 0 else 0
                        targetSheet.buildCellData(curRow, entryCol)?.let { return currentState.selectSingleCell(it) }
                        return currentState
                    }
                }
            }
        }

        val currentKey = currentState.activeCell
        val targetSheetIdx = if (currentKey != null) {
            nonEmptySheets.indexOfFirst { it.fileUri == currentKey.fileUri && it.sheetName == currentKey.sheetName }
        } else {
            -1
        }
        if (targetSheetIdx < 0) {
            val firstSheet = nonEmptySheets.first()
            val firstCell = firstSheet.buildCellData(0, 0) ?: return currentState
            return currentState.selectSingleCell(firstCell)
        }

        val targetSheet = nonEmptySheets[targetSheetIdx]
        val curRow = currentKey!!.rowIndex.coerceIn(0, targetSheet.rowCount - 1)
        val curCol = currentKey.colIndex.coerceIn(0, targetSheet.maxCols - 1)

        var nextSheet = targetSheet
        var nextRow = curRow
        var nextCol = curCol

        when (direction) {
            GridNavDirection.UP -> {
                if (curRow > 0) {
                    nextRow = curRow - 1
                } else if (targetSheetIdx > 0) {
                    nextSheet = nonEmptySheets[targetSheetIdx - 1]
                    nextRow = nextSheet.rowCount - 1
                    nextCol = curCol.coerceIn(0, nextSheet.maxCols - 1)
                }
            }
            GridNavDirection.DOWN -> {
                if (curRow < targetSheet.rowCount - 1) {
                    nextRow = curRow + 1
                } else if (targetSheetIdx < nonEmptySheets.lastIndex) {
                    nextSheet = nonEmptySheets[targetSheetIdx + 1]
                    nextRow = 0
                    nextCol = curCol.coerceIn(0, nextSheet.maxCols - 1)
                }
            }
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

        val cellData = nextSheet.buildCellData(nextRow, nextCol) ?: return currentState
        return currentState.selectSingleCell(cellData)
    }
}
