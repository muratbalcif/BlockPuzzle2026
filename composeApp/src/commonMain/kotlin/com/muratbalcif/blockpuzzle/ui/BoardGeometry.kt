package com.muratbalcif.blockpuzzle.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.muratbalcif.blockpuzzle.engine.Cell
import kotlin.math.floor

/** Full scrolled grid bounds and clipped viewport, both in root coordinates. */
class BoardGeometry(val bounds: Rect, val visibleBounds: Rect, val size: Int) {
    fun originAt(point: Offset, anchor: Cell): Cell? {
        if (!point.x.isFinite() || !point.y.isFinite() || size <= 0) return null
        if (bounds.width <= 0 || bounds.height <= 0 || !visibleBounds.contains(point)) return null
        if (!bounds.contains(point)) return null
        val row = floor((point.y - bounds.top) / (bounds.height / size)).toInt()
        val column = floor((point.x - bounds.left) / (bounds.width / size)).toInt()
        return Cell(row - anchor.row, column - anchor.column)
    }
}
