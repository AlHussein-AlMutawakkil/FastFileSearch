package com.example.fastfilesearch.desktop.ui

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor

data class DesktopTableDimensions(
    val rowNumWidth: Dp,
    val columnWidths: List<Dp>,
    val totalTableWidth: Dp
)

/**
 * Desktop port of Android's TableLayoutCalculator with support for manual column resizing
 * (RTL-aware drag deltas + double-click reset to auto-fit width).
 */
object DesktopTableLayoutCalculator {

    val MIN_RESIZE_COL_WIDTH: Dp = 48.dp
    val MAX_RESIZE_COL_WIDTH: Dp = 900.dp

    fun calculate(
        rowsData: List<Triple<String, List<String>, *>>,
        columnHeaders: List<String>,
        maxCols: Int,
        totalContainerWidth: Dp,
        stretchSingleColumn: Boolean,
        textMeasurer: TextMeasurer,
        density: Density,
        cellTextStyle: TextStyle,
        headerTextStyle: TextStyle,
        rowNumTextStyle: TextStyle
    ): DesktopTableDimensions {
        val longestRowNum = rowsData.maxByOrNull { it.first.length }?.first ?: "1"
        val rowNumContent = if ("صف".length > longestRowNum.length) "صف" else longestRowNum
        val measuredRowNumPx = maxOf(
            textMeasurer.measure(text = longestRowNum, style = rowNumTextStyle).size.width,
            textMeasurer.measure(text = "صف", style = headerTextStyle).size.width,
            textMeasurer.measure(text = rowNumContent, style = rowNumTextStyle).size.width
        )
        val measuredRowNumDp = with(density) { measuredRowNumPx.toDp() } + 16.dp
        val rowNumWidth = measuredRowNumDp.coerceIn(42.dp, 68.dp)

        val remainingWidth = if (totalContainerWidth > rowNumWidth) {
            totalContainerWidth - rowNumWidth
        } else {
            0.dp
        }

        if (maxCols <= 1) {
            val singleColWidth = if (stretchSingleColumn) {
                maxOf(remainingWidth, 56.dp)
            } else {
                val longestText = rowsData.take(80)
                    .mapNotNull { it.second.firstOrNull() }
                    .maxByOrNull { it.length } ?: ""
                val measuredLinePx = if (longestText.isNotEmpty()) {
                    textMeasurer.measure(
                        text = longestText,
                        style = cellTextStyle
                    ).size.width
                } else {
                    0
                }
                val longestLineDp = with(density) { measuredLinePx.toDp() }
                maxOf(remainingWidth, longestLineDp + 24.dp).coerceIn(56.dp, 1000.dp)
            }
            return DesktopTableDimensions(
                rowNumWidth = rowNumWidth,
                columnWidths = listOf(singleColWidth),
                totalTableWidth = rowNumWidth + singleColWidth
            )
        }

        val baseWidths = (0 until maxCols).map { colIndex ->
            val colTitle = columnHeaders.getOrNull(colIndex)?.takeIf { it.isNotBlank() } ?: "عمود ${colIndex + 1}"
            val headerWidthPx = textMeasurer.measure(
                text = colTitle,
                style = headerTextStyle
            ).size.width
            val headerWidthDp = with(density) { headerWidthPx.toDp() } + 22.dp

            val sampleCells = rowsData.take(80).mapNotNull { it.second.getOrNull(colIndex) }
            val longestCell = sampleCells.maxByOrNull { it.length } ?: ""
            val cellWidthPx = if (longestCell.isNotEmpty()) {
                textMeasurer.measure(
                    text = longestCell,
                    style = cellTextStyle
                ).size.width
            } else {
                0
            }
            val cellWidthDp = with(density) { cellWidthPx.toDp() } + 24.dp

            maxOf(headerWidthDp, cellWidthDp).coerceIn(56.dp, 240.dp)
        }.toMutableList()

        val totalBaseColumnsWidth = baseWidths.fold(0.dp) { acc, d -> acc + d }

        if (totalBaseColumnsWidth < remainingWidth) {
            val surplus = remainingWidth - totalBaseColumnsWidth
            val textColIndices = (0 until maxCols).filter { colIndex ->
                rowsData.take(80).any { (it.second.getOrNull(colIndex)?.length ?: 0) > 10 }
            }

            val targetIndices = if (textColIndices.isNotEmpty()) textColIndices else (0 until maxCols).toList()
            val count = targetIndices.size
            if (count > 0) {
                val surplusPerCol = surplus.value / count
                val flooredAdd = floor(surplusPerCol.toDouble()).toFloat()
                var addedSoFar = 0f

                targetIndices.forEachIndexed { idx, colIdx ->
                    if (idx == targetIndices.lastIndex) {
                        val remainder = surplus.value - addedSoFar
                        baseWidths[colIdx] = baseWidths[colIdx] + remainder.dp
                    } else {
                        baseWidths[colIdx] = baseWidths[colIdx] + flooredAdd.dp
                        addedSoFar += flooredAdd
                    }
                }
            }
        }

        val totalTableWidth = rowNumWidth + baseWidths.fold(0.dp) { acc, d -> acc + d }

        return DesktopTableDimensions(
            rowNumWidth = rowNumWidth,
            columnWidths = baseWidths,
            totalTableWidth = totalTableWidth
        )
    }

    fun applyColumnResizeDelta(
        currentWidth: Dp,
        dragDeltaDp: Dp,
        isRtl: Boolean,
        minWidth: Dp = MIN_RESIZE_COL_WIDTH,
        maxWidth: Dp = MAX_RESIZE_COL_WIDTH
    ): Dp {
        // In RTL, a column's trailing resize border sits on its LEFT edge:
        // dragging left (negative screen delta) widens the column, dragging right narrows it.
        val signedDelta = if (isRtl) -dragDeltaDp else dragDeltaDp
        return (currentWidth + signedDelta).coerceIn(minWidth, maxWidth)
    }

    fun applyCustomColumnWidths(
        baseDims: DesktopTableDimensions,
        customColumnWidths: Map<Int, Dp>
    ): DesktopTableDimensions {
        if (customColumnWidths.isEmpty()) return baseDims
        val merged = baseDims.columnWidths.mapIndexed { idx, autoWidth ->
            customColumnWidths[idx]?.coerceIn(MIN_RESIZE_COL_WIDTH, MAX_RESIZE_COL_WIDTH) ?: autoWidth
        }
        val total = baseDims.rowNumWidth + merged.fold(0.dp) { acc, d -> acc + d }
        return DesktopTableDimensions(
            rowNumWidth = baseDims.rowNumWidth,
            columnWidths = merged,
            totalTableWidth = total
        )
    }
}
