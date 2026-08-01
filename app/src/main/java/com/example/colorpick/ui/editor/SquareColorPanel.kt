package com.example.colorpick.ui.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun SquareColorPanel(
    offset: Offset,
    themeColor: Color,
    onOffsetChange: (Offset) -> Unit,
    onReset: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val baseHue = themeColor.hslHue
    val gridCount = 11
    val maxIndex = gridCount - 1
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val gridPadding = 8.dp
    val gridPaddingPx = gridPadding.value * density

    val dragScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }

    fun snapToGrid(value: Float): Float {
        val index = ((value + 1f) / 2f * maxIndex).roundToInt().coerceIn(0, maxIndex)
        return index / maxIndex.toFloat() * 2f - 1f
    }

    fun gridIndex(value: Float): Int {
        return ((value + 1f) / 2f * maxIndex).roundToInt().coerceIn(0, maxIndex)
    }

    val snappedOffset = remember(offset) {
        Offset(snapToGrid(offset.x), snapToGrid(offset.y))
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(25.dp))
            .background(Color.Black.copy(alpha = 0.42f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(25.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            scope.launch { dragScale.animateTo(3.8f, spring(dampingRatio = 0.55f, stiffness = 380f)) }
                        },
                        onDragEnd = {
                            isDragging = false
                            scope.launch { dragScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 380f)) }
                        },
                        onDragCancel = {
                            isDragging = false
                            scope.launch { dragScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 380f)) }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val pos = change.position
                            val innerW = (size.width - 2 * gridPaddingPx).coerceAtLeast(1f)
                            val innerH = (size.height - 2 * gridPaddingPx).coerceAtLeast(1f)
                            // Report the continuous offset so values like 99 are reachable.
                            val nx = (((pos.x - gridPaddingPx) / innerW) * 2f - 1f).coerceIn(-1f, 1f)
                            val ny = ((1f - (pos.y - gridPaddingPx) / innerH) * 2f - 1f).coerceIn(-1f, 1f)
                            onOffsetChange(Offset(nx, ny))
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offsetPx ->
                        val innerW = (size.width - 2 * gridPaddingPx).coerceAtLeast(1f)
                        val innerH = (size.height - 2 * gridPaddingPx).coerceAtLeast(1f)
                        val nx = (((offsetPx.x - gridPaddingPx) / innerW) * 2f - 1f).coerceIn(-1f, 1f)
                        val ny = ((1f - (offsetPx.y - gridPaddingPx) / innerH) * 2f - 1f).coerceIn(-1f, 1f)
                        if (abs(nx) < 0.12f && abs(ny) < 0.12f) {
                            onReset()
                        } else {
                            onOffsetChange(Offset(nx, ny))
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val hue = ((baseHue) % 360f + 360f) % 360f

            // Theme-color gradient background fills the entire rounded panel, including corners.
            val columns = 36
            for (col in 0 until columns) {
                val normX = col / (columns - 1f)
                val saturation = (0.08f + normX * 0.72f).coerceIn(0f, 1f)
                val left = col * w / columns
                val right = (col + 1) * w / columns
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.hsl(hue, saturation, 0.76f),
                            Color.hsl(hue, saturation, 0.16f)
                        ),
                        startY = 0f,
                        endY = h
                    ),
                    topLeft = Offset(left, 0f),
                    size = Size(right - left, h)
                )
            }

            // 11x11 dot matrix drawn inside the padded area.
            val innerW = (w - 2 * gridPaddingPx).coerceAtLeast(1f)
            val innerH = (h - 2 * gridPaddingPx).coerceAtLeast(1f)
            val cellW = innerW / gridCount
            val cellH = innerH / gridCount
            val dotRadius = min(cellW, cellH) * 0.15f
            val midIndex = maxIndex / 2

            val activeCol = gridIndex(snappedOffset.x)
            val activeRow = maxIndex - gridIndex(snappedOffset.y)

            for (row in 0 until gridCount) {
                for (col in 0 until gridCount) {
                    val cx = gridPaddingPx + (col + 0.5f) * cellW
                    val cy = gridPaddingPx + (row + 0.5f) * cellH
                    val isCenter = row == midIndex && col == midIndex
                    val isAxis = row == activeRow || col == activeCol
                    val dist = abs(row - activeRow) + abs(col - activeCol)

                    if (isCenter) {
                        val strokeAlpha = if (isAxis) 0.65f else 0.3f
                        drawCircle(
                            color = Color.White.copy(alpha = strokeAlpha),
                            radius = dotRadius,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2f * density)
                        )
                    } else {
                        val alpha: Float
                        val radius: Float
                        val glow: Boolean
                        when {
                            isDragging && dist == 1 -> {
                                alpha = 0.85f
                                radius = dotRadius * 3f
                                glow = true
                            }
                            isDragging && dist == 2 -> {
                                alpha = 0.6f
                                radius = dotRadius * 2f
                                glow = true
                            }
                            !isDragging && isAxis -> {
                                alpha = 0.65f
                                radius = dotRadius
                                glow = false
                            }
                            else -> {
                                alpha = 0.2f
                                radius = dotRadius
                                glow = false
                            }
                        }
                        if (glow) {
                            drawCircle(
                                color = Color.White.copy(alpha = alpha * 0.45f),
                                radius = radius * 1.35f,
                                center = Offset(cx, cy)
                            )
                        }
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = radius,
                            center = Offset(cx, cy)
                        )
                    }
                }
            }

            // Joystick snaps to the active grid dot and scales up while dragging.
            val handleX = gridPaddingPx + (activeCol + 0.5f) * cellW
            val handleY = gridPaddingPx + (activeRow + 0.5f) * cellH
            val scale = dragScale.value
            val baseHandleRadius = min(cellW, cellH) * 0.28f
            val handleRadius = baseHandleRadius * scale

            drawCircle(Color.White.copy(alpha = 0.25f), handleRadius + 2f * density, Offset(handleX, handleY))
            drawCircle(Color.White, handleRadius, Offset(handleX, handleY))
        }
    }
}
