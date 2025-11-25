package com.glyphmatrix.tasker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.glyphmatrix.tasker.util.GlyphEncoder

/**
 * A 25x25 grid painter composable for drawing Glyph Matrix patterns.
 *
 * @param brightness Current brightness array (625 values, 0-255)
 * @param selectedBrightness Current brush brightness (0-255)
 * @param onBrightnessChange Callback when a pixel brightness changes
 */
@Composable
fun GlyphPainterView(
    brightness: IntArray,
    selectedBrightness: Int,
    onBrightnessChange: (index: Int, value: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridSize = GlyphEncoder.GRID_SIZE
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val backgroundColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .background(backgroundColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(selectedBrightness) {
                    detectTapGestures { offset ->
                        val cellWidth = size.width.toFloat() / gridSize
                        val cellHeight = size.height.toFloat() / gridSize
                        val col = (offset.x / cellWidth).toInt().coerceIn(0, gridSize - 1)
                        val row = (offset.y / cellHeight).toInt().coerceIn(0, gridSize - 1)
                        val index = row * gridSize + col
                        onBrightnessChange(index, selectedBrightness)
                    }
                }
                .pointerInput(selectedBrightness) {
                    detectDragGestures { change, _ ->
                        val cellWidth = size.width.toFloat() / gridSize
                        val cellHeight = size.height.toFloat() / gridSize
                        val col = (change.position.x / cellWidth).toInt().coerceIn(0, gridSize - 1)
                        val row = (change.position.y / cellHeight).toInt().coerceIn(0, gridSize - 1)
                        val index = row * gridSize + col
                        onBrightnessChange(index, selectedBrightness)
                    }
                }
        ) {
            canvasSize = size
            val cellWidth = size.width / gridSize
            val cellHeight = size.height / gridSize

            // Draw pixels
            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    val index = row * gridSize + col
                    val value = if (index < brightness.size) brightness[index] else 0
                    val pixelColor = brightnessToColor(value)

                    drawRect(
                        color = pixelColor,
                        topLeft = Offset(col * cellWidth, row * cellHeight),
                        size = Size(cellWidth, cellHeight)
                    )
                }
            }

            // Draw grid lines
            drawGridLines(gridSize, cellWidth, cellHeight, gridLineColor)
        }
    }
}

/**
 * Convert brightness (0-255) to display color.
 * Uses a warm white color to simulate LED appearance.
 */
private fun brightnessToColor(brightness: Int): Color {
    val normalizedBrightness = brightness.coerceIn(0, 255) / 255f
    // Warm white LEDs have a slight yellow/amber tint
    return Color(
        red = normalizedBrightness,
        green = normalizedBrightness * 0.95f,
        blue = normalizedBrightness * 0.8f,
        alpha = 1f
    )
}

/**
 * Draw grid lines on the canvas.
 */
private fun DrawScope.drawGridLines(
    gridSize: Int,
    cellWidth: Float,
    cellHeight: Float,
    lineColor: Color
) {
    // Vertical lines
    for (col in 0..gridSize) {
        drawLine(
            color = lineColor,
            start = Offset(col * cellWidth, 0f),
            end = Offset(col * cellWidth, size.height),
            strokeWidth = 1f
        )
    }
    // Horizontal lines
    for (row in 0..gridSize) {
        drawLine(
            color = lineColor,
            start = Offset(0f, row * cellHeight),
            end = Offset(size.width, row * cellHeight),
            strokeWidth = 1f
        )
    }
}
