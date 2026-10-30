package com.muratbalcif.blockpuzzle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.muratbalcif.blockpuzzle.engine.Cell
import com.muratbalcif.blockpuzzle.ui.BoardGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardGeometryTest {
    @Test
    fun convertsRootCoordinatesAndGrabbedCellAnchor() {
        val geometry = BoardGeometry(Rect(30f, 100f, 350f, 420f), Rect(30f, 100f, 350f, 420f), 8)
        assertEquals(Cell(0, 0), geometry.originAt(Offset(31f, 101f), Cell(0, 0)))
        assertEquals(Cell(7, 7), geometry.originAt(Offset(349f, 419f), Cell(0, 0)))
        assertEquals(Cell(2, 3), geometry.originAt(Offset(191f, 221f), Cell(1, 1)))
    }

    @Test
    fun rejectsOutsideAndExactFarEdges() {
        val geometry = BoardGeometry(Rect(0f, 0f, 320f, 320f), Rect(0f, 0f, 320f, 320f), 8)
        for (point in listOf(
            Offset(-1f, 0f),
            Offset(320f, 1f),
            Offset(1f, 320f),
            Offset(Float.NaN, 1f),
        )) {
            assertNull(geometry.originAt(point, Cell(0, 0)))
        }
    }

    @Test
    fun zoomScrollUsesFullGridOriginButClippedVisibleViewport() {
        val geometry = BoardGeometry(Rect(-240f, -96f, 528f, 672f), Rect(0f, 0f, 320f, 320f), 16)
        assertEquals(Cell(2, 5), geometry.originAt(Offset(1f, 1f), Cell(0, 0)))
        assertNull(geometry.originAt(Offset(350f, 200f), Cell(0, 0)))
    }
}
