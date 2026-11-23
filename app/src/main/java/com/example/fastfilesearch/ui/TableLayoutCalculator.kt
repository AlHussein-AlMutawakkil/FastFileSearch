package com.example.fastfilesearch.ui

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor

data class TableDimensions(
    val rowNumWidth: Dp,
    val columnWidths: List<Dp>,
    val totalTableWidth: Dp
)

object TableLayoutCalculator {

    /**
     * Calculates exact column widths, row number width, and total table width using TextMeasurer:
     * - rowNumWidth is measured dynamically within 42.dp..68.dp.
     * - For single column (maxCols == 1):
     *     - if stretchSingleColumn is true: fills remainingWidth (100% of container).
     *     - if stretchSingleColumn is false: takes maxOf(remainingWidth, longestLineDp + 24.dp).coerceAtMost(1000.dp).
     * - For multiple columns (maxCols > 1):
     *     - measures the larger between column title and longest cell in sample (take 80) within 56.dp..240.dp.
     *     - distributes any surplus space to text columns first (cells > 10 chars) or equally, using floor
     *       and assigning remainder to the last column to eliminate ghost 1px horizontal scrolling.
     */
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
    ): TableDimensions {
        // 1. Calculate rowNumWidth dynamically via TextMeasurer
        val longestRowNum = rowsData.maxOfOrNull { it.first } ?: "1"
        val measuredRowNumPx = textMeasurer.measure(
            text = longestRowNum,
            style = rowNumTextStyle
        ).size.width
        val measuredRowNumDp = with(density) { measuredRowNumPx.toDp() } + 12.dp
        val rowNumWidth = measuredRowNumDp.coerceIn(28.dp, 58.dp)

        val remainingWidth = if (totalContainerWidth > rowNumWidth) {
            totalContainerWidth - rowNumWidth
        } else {
            0.dp
        }

        // Single column (maxCols == 1)
        if (maxCols <= 1) {
            val singleColWidth = if (stretchSingleColumn) {
                remainingWidth
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
                maxOf(remainingWidth, longestLineDp + 24.dp).coerceAtMost(1000.dp)
            }
            return TableDimensions(
                rowNumWidth = rowNumWidth,
                columnWidths = listOf(singleColWidth),
                totalTableWidth = rowNumWidth + singleColWidth
            )
        }

        // Multiple columns (maxCols > 1)
        val baseWidths = (0 until maxCols).map { colIndex ->
            val colTitle = columnHeaders.getOrNull(colIndex)?.takeIf { it.isNotBlank() } ?: "عمود ${colIndex + 1}"
            val headerWidthPx = textMeasurer.measure(
                text = colTitle,
                style = headerTextStyle
            ).size.width
            val headerWidthDp = with(density) { headerWidthPx.toDp() } + 20.dp

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

        // If sum is less than remainingWidth, distribute surplus space
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

        return TableDimensions(
            rowNumWidth = rowNumWidth,
            columnWidths = baseWidths,
            totalTableWidth = totalTableWidth
        )
    }
}
