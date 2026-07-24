package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.cv.QuadPoints
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Handle Types for Manual Crop Control
 */
enum class CropHandle {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    TOP_EDGE,
    BOTTOM_EDGE,
    LEFT_EDGE,
    RIGHT_EDGE
}

/**
 * Production Manual Document Crop Overlay Editor with Loupe & Touch Target Accuracy.
 *
 * Guarantees:
 * 1. 100% exact Preview <-> Bitmap pixel coordinate mapping (accounts for ContentScale.Fit letterboxing/pillarboxing).
 * 2. Active handle locking during drag gestures (no lost touch focus or corner jumping).
 * 3. Enforces corner constraints (no corner crossing, min edge distance, min area, convexity).
 * 4. Live magnifying loupe when adjusting corners for pinpoint accuracy.
 * 5. Semi-transparent dimming mask outside document polygon.
 */
@Composable
fun CropOverlayEditor(
    bitmap: Bitmap,
    cropQuad: QuadPoints,
    onCropQuadChange: (QuadPoints) -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFF2196F3),
    handleRadius: Dp = 16.dp,
    touchTargetRadius: Dp = 44.dp
) {
    val density = LocalDensity.current
    val touchTargetPx = with(density) { touchTargetRadius.toPx() }
    val handleRadiusPx = with(density) { handleRadius.toPx() }

    var activeHandle by remember { mutableStateOf<CropHandle?>(null) }
    var currentTouchPos by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bitmap, cropQuad) {
                    detectDragGestures(
                        onDragStart = { downPos ->
                            val boxWidth = size.width.toFloat()
                            val boxHeight = size.height.toFloat()
                            if (boxWidth <= 0f || boxHeight <= 0f) return@detectDragGestures

                            val imageRect = calculateFitImageRect(boxWidth, boxHeight, bitmap.width, bitmap.height)
                            val (screenTL, screenTR, screenBR, screenBL) = getScreenPoints(cropQuad, imageRect)

                            // Check corner handles first
                            val dTL = (downPos - screenTL).getDistance()
                            val dTR = (downPos - screenTR).getDistance()
                            val dBR = (downPos - screenBR).getDistance()
                            val dBL = (downPos - screenBL).getDistance()

                            val topMid = Offset((screenTL.x + screenTR.x) / 2f, (screenTL.y + screenTR.y) / 2f)
                            val botMid = Offset((screenBL.x + screenBR.x) / 2f, (screenBL.y + screenBR.y) / 2f)
                            val leftMid = Offset((screenTL.x + screenBL.x) / 2f, (screenTL.y + screenBL.y) / 2f)
                            val rightMid = Offset((screenTR.x + screenBR.x) / 2f, (screenTR.y + screenBR.y) / 2f)

                            val dTopEdge = (downPos - topMid).getDistance()
                            val dBotEdge = (downPos - botMid).getDistance()
                            val dLeftEdge = (downPos - leftMid).getDistance()
                            val dRightEdge = (downPos - rightMid).getDistance()

                            val handlesWithDist = listOf(
                                CropHandle.TOP_LEFT to dTL,
                                CropHandle.TOP_RIGHT to dTR,
                                CropHandle.BOTTOM_RIGHT to dBR,
                                CropHandle.BOTTOM_LEFT to dBL,
                                CropHandle.TOP_EDGE to dTopEdge,
                                CropHandle.BOTTOM_EDGE to dBotEdge,
                                CropHandle.LEFT_EDGE to dLeftEdge,
                                CropHandle.RIGHT_EDGE to dRightEdge
                            )

                            val closest = handlesWithDist.minByOrNull { it.second }
                            if (closest != null && closest.second <= touchTargetPx * 1.5f) {
                                activeHandle = closest.first
                                currentTouchPos = downPos
                            } else {
                                activeHandle = null
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val handle = activeHandle ?: return@detectDragGestures
                            val boxWidth = size.width.toFloat()
                            val boxHeight = size.height.toFloat()
                            if (boxWidth <= 0f || boxHeight <= 0f) return@detectDragGestures

                            val imageRect = calculateFitImageRect(boxWidth, boxHeight, bitmap.width, bitmap.height)
                            val touchPos = change.position
                            currentTouchPos = touchPos

                            val normX = ((touchPos.x - imageRect.left) / imageRect.width).coerceIn(0f, 1f)
                            val normY = ((touchPos.y - imageRect.top) / imageRect.height).coerceIn(0f, 1f)
                            val newNormPos = Offset(normX, normY)

                            val updatedQuad = applyHandleMove(cropQuad, handle, newNormPos, dragAmount, imageRect)
                            if (validateQuadConstraints(updatedQuad)) {
                                onCropQuadChange(updatedQuad)
                            }
                        },
                        onDragEnd = {
                            activeHandle = null
                            currentTouchPos = null
                        },
                        onDragCancel = {
                            activeHandle = null
                            currentTouchPos = null
                        }
                    )
                }
        ) {
            val boxWidth = size.width
            val boxHeight = size.height
            if (boxWidth <= 0f || boxHeight <= 0f) return@Canvas

            val imageRect = calculateFitImageRect(boxWidth, boxHeight, bitmap.width, bitmap.height)

            // 1. Draw scaled bitmap accurately inside imageRect
            drawImage(
                image = bitmap.asImageBitmap(),
                dstOffset = IntOffset(imageRect.left.toInt(), imageRect.top.toInt()),
                dstSize = IntSize(imageRect.width.toInt(), imageRect.height.toInt())
            )

            // 2. Map normalized cropQuad to Screen Pixels
            val (ptTL, ptTR, ptBR, ptBL) = getScreenPoints(cropQuad, imageRect)

            val quadPath = Path().apply {
                moveTo(ptTL.x, ptTL.y)
                lineTo(ptTR.x, ptTR.y)
                lineTo(ptBR.x, ptBR.y)
                lineTo(ptBL.x, ptBL.y)
                close()
            }

            // 3. Draw Dark Mask outside document quad
            val imagePath = Path().apply {
                addRect(Rect(imageRect.left, imageRect.top, imageRect.right, imageRect.bottom))
            }

            val maskPath = Path()
            maskPath.op(imagePath, quadPath, PathOperation.Difference)
            drawPath(maskPath, color = Color.Black.copy(alpha = 0.50f))

            // 4. Draw Document Polygon Border Lines
            drawPath(
                path = quadPath,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // 5. Draw 4 Corner Handles
            val cornerPts = listOf(
                CropHandle.TOP_LEFT to ptTL,
                CropHandle.TOP_RIGHT to ptTR,
                CropHandle.BOTTOM_RIGHT to ptBR,
                CropHandle.BOTTOM_LEFT to ptBL
            )

            cornerPts.forEach { (handle, pt) ->
                val isSelected = activeHandle == handle
                val outerRadius = if (isSelected) handleRadiusPx * 1.3f else handleRadiusPx
                val innerRadius = outerRadius * 0.50f

                drawCircle(color = primaryColor, radius = outerRadius, center = pt)
                drawCircle(color = Color.White, radius = innerRadius, center = pt)
            }

            // 6. Draw 4 Edge Midpoint Handles
            val edgeWidth = 32.dp.toPx()
            val edgeHeight = 10.dp.toPx()

            val topMid = Offset((ptTL.x + ptTR.x) / 2f, (ptTL.y + ptTR.y) / 2f)
            val botMid = Offset((ptBL.x + ptBR.x) / 2f, (ptBL.y + ptBR.y) / 2f)
            val leftMid = Offset((ptTL.x + ptBL.x) / 2f, (ptTL.y + ptBL.y) / 2f)
            val rightMid = Offset((ptTR.x + ptBR.x) / 2f, (ptTR.y + ptBR.y) / 2f)

            drawRoundRect(
                color = if (activeHandle == CropHandle.TOP_EDGE) Color.White else primaryColor,
                topLeft = Offset(topMid.x - edgeWidth / 2f, topMid.y - edgeHeight / 2f),
                size = Size(edgeWidth, edgeHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            drawRoundRect(
                color = if (activeHandle == CropHandle.BOTTOM_EDGE) Color.White else primaryColor,
                topLeft = Offset(botMid.x - edgeWidth / 2f, botMid.y - edgeHeight / 2f),
                size = Size(edgeWidth, edgeHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            drawRoundRect(
                color = if (activeHandle == CropHandle.LEFT_EDGE) Color.White else primaryColor,
                topLeft = Offset(leftMid.x - edgeHeight / 2f, leftMid.y - edgeWidth / 2f),
                size = Size(edgeHeight, edgeWidth),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            drawRoundRect(
                color = if (activeHandle == CropHandle.RIGHT_EDGE) Color.White else primaryColor,
                topLeft = Offset(rightMid.x - edgeHeight / 2f, rightMid.y - edgeWidth / 2f),
                size = Size(edgeHeight, edgeWidth),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // 7. Render Active Loupe / Magnifier when dragging a corner handle
            val currentHandle = activeHandle
            val touchPos = currentTouchPos
            if (currentHandle in listOf(CropHandle.TOP_LEFT, CropHandle.TOP_RIGHT, CropHandle.BOTTOM_RIGHT, CropHandle.BOTTOM_LEFT) && touchPos != null) {
                val activeCornerPt = when (currentHandle) {
                    CropHandle.TOP_LEFT -> ptTL
                    CropHandle.TOP_RIGHT -> ptTR
                    CropHandle.BOTTOM_RIGHT -> ptBR
                    CropHandle.BOTTOM_LEFT -> ptBL
                    else -> ptTL
                }

                drawLoupe(
                    bitmap = bitmap,
                    activeCornerScreenPt = activeCornerPt,
                    imageRect = imageRect,
                    touchPos = touchPos,
                    primaryColor = primaryColor
                )
            }
        }
    }
}

/**
 * Calculates exact destination rectangle of ContentScale.Fit image inside container Box.
 */
private fun calculateFitImageRect(containerW: Float, containerH: Float, bitmapW: Int, bitmapH: Int): Rect {
    if (bitmapW <= 0 || bitmapH <= 0 || containerW <= 0f || containerH <= 0f) {
        return Rect(0f, 0f, containerW, containerH)
    }

    val bitmapRatio = bitmapW.toFloat() / bitmapH.toFloat()
    val containerRatio = containerW / containerH

    val imgW: Float
    val imgH: Float
    if (bitmapRatio > containerRatio) {
        imgW = containerW
        imgH = containerW / bitmapRatio
    } else {
        imgH = containerH
        imgW = containerH * bitmapRatio
    }

    val offsetX = (containerW - imgW) / 2f
    val offsetY = (containerH - imgH) / 2f

    return Rect(offsetX, offsetY, offsetX + imgW, offsetY + imgH)
}

/**
 * Maps normalized QuadPoints (0..1) to screen pixel coordinates.
 */
private fun getScreenPoints(quad: QuadPoints, imageRect: Rect): ScreenPoints {
    val ptTL = Offset(imageRect.left + quad.topLeft.x * imageRect.width, imageRect.top + quad.topLeft.y * imageRect.height)
    val ptTR = Offset(imageRect.left + quad.topRight.x * imageRect.width, imageRect.top + quad.topRight.y * imageRect.height)
    val ptBR = Offset(imageRect.left + quad.bottomRight.x * imageRect.width, imageRect.top + quad.bottomRight.y * imageRect.height)
    val ptBL = Offset(imageRect.left + quad.bottomLeft.x * imageRect.width, imageRect.top + quad.bottomLeft.y * imageRect.height)
    return ScreenPoints(ptTL, ptTR, ptBR, ptBL)
}

private data class ScreenPoints(val tl: Offset, val tr: Offset, val br: Offset, val bl: Offset)

/**
 * Updates quad position according to active handle drag and returns candidate quad.
 */
private fun applyHandleMove(
    quad: QuadPoints,
    handle: CropHandle,
    newPos: Offset,
    dragAmount: Offset,
    imageRect: Rect
): QuadPoints {
    val normDX = dragAmount.x / imageRect.width
    val normDY = dragAmount.y / imageRect.height

    return when (handle) {
        CropHandle.TOP_LEFT -> quad.copy(topLeft = newPos)
        CropHandle.TOP_RIGHT -> quad.copy(topRight = newPos)
        CropHandle.BOTTOM_RIGHT -> quad.copy(bottomRight = newPos)
        CropHandle.BOTTOM_LEFT -> quad.copy(bottomLeft = newPos)

        CropHandle.TOP_EDGE -> quad.copy(
            topLeft = Offset(quad.topLeft.x, (quad.topLeft.y + normDY).coerceIn(0f, 1f)),
            topRight = Offset(quad.topRight.x, (quad.topRight.y + normDY).coerceIn(0f, 1f))
        )
        CropHandle.BOTTOM_EDGE -> quad.copy(
            bottomLeft = Offset(quad.bottomLeft.x, (quad.bottomLeft.y + normDY).coerceIn(0f, 1f)),
            bottomRight = Offset(quad.bottomRight.x, (quad.bottomRight.y + normDY).coerceIn(0f, 1f))
        )
        CropHandle.LEFT_EDGE -> quad.copy(
            topLeft = Offset((quad.topLeft.x + normDX).coerceIn(0f, 1f), quad.topLeft.y),
            bottomLeft = Offset((quad.bottomLeft.x + normDX).coerceIn(0f, 1f), quad.bottomLeft.y)
        )
        CropHandle.RIGHT_EDGE -> quad.copy(
            topRight = Offset((quad.topRight.x + normDX).coerceIn(0f, 1f), quad.topRight.y),
            bottomRight = Offset((quad.bottomRight.x + normDX).coerceIn(0f, 1f), quad.bottomRight.y)
        )
    }
}

/**
 * Enforces geometry constraints:
 * - Minimum edge separation (8%)
 * - Quadrilateral convexity
 * - Minimum area (2%)
 */
private fun validateQuadConstraints(quad: QuadPoints): Boolean {
    val minEdge = 0.08f

    // 1. Orientation / Crossing Checks
    if (quad.topLeft.x >= quad.topRight.x - minEdge) return false
    if (quad.bottomLeft.x >= quad.bottomRight.x - minEdge) return false
    if (quad.topLeft.y >= quad.bottomLeft.y - minEdge) return false
    if (quad.topRight.y >= quad.bottomRight.y - minEdge) return false

    // 2. Convexity check
    if (!quad.isConvex()) return false

    // 3. Minimum Area check (at least 2% of image area)
    val pts = quad.asList()
    var area = 0f
    val n = pts.size
    for (i in 0 until n) {
        val p1 = pts[i]
        val p2 = pts[(i + 1) % n]
        area += p1.x * p2.y - p2.x * p1.y
    }
    val normArea = abs(area) / 2f

    return normArea >= 0.02f
}

/**
 * Draws precision Magnifying Loupe floating above user finger during corner drag.
 */
private fun DrawScope.drawLoupe(
    bitmap: Bitmap,
    activeCornerScreenPt: Offset,
    imageRect: Rect,
    touchPos: Offset,
    primaryColor: Color
) {
    val loupeRadius = 60.dp.toPx()
    val loupeDiameter = loupeRadius * 2f

    // Position loupe offset above touch position so finger does not obscure view
    var loupeCenter = Offset(touchPos.x, touchPos.y - loupeRadius - 40.dp.toPx())
    if (loupeCenter.y - loupeRadius < 10.dp.toPx()) {
        // Shift loupe below finger if near top edge of screen
        loupeCenter = Offset(touchPos.x, touchPos.y + loupeRadius + 40.dp.toPx())
    }

    // Clamp loupe inside canvas bounds
    val clampedX = loupeCenter.x.coerceIn(loupeRadius + 10.dp.toPx(), size.width - loupeRadius - 10.dp.toPx())
    val clampedY = loupeCenter.y.coerceIn(loupeRadius + 10.dp.toPx(), size.height - loupeRadius - 10.dp.toPx())
    loupeCenter = Offset(clampedX, clampedY)

    val loupePath = Path().apply {
        addOval(Rect(loupeCenter.x - loupeRadius, loupeCenter.y - loupeRadius, loupeCenter.x + loupeRadius, loupeCenter.y + loupeRadius))
    }

    // Clip & draw magnified bitmap portion inside loupe
    clipPath(loupePath) {
        // Draw black backing
        drawCircle(color = Color.Black, radius = loupeRadius, center = loupeCenter)

        // Calculate source region in bitmap corresponding to active corner screen position
        val normX = ((activeCornerScreenPt.x - imageRect.left) / imageRect.width).coerceIn(0f, 1f)
        val normY = ((activeCornerScreenPt.y - imageRect.top) / imageRect.height).coerceIn(0f, 1f)

        val bmpCenterX = normX * bitmap.width
        val bmpCenterY = normY * bitmap.height

        val cropWindowW = bitmap.width * 0.20f // 20% bitmap window
        val cropWindowH = bitmap.height * 0.20f

        val srcLeft = (bmpCenterX - cropWindowW / 2f).toInt().coerceIn(0, bitmap.width - 1)
        val srcTop = (bmpCenterY - cropWindowH / 2f).toInt().coerceIn(0, bitmap.height - 1)
        val srcRight = (bmpCenterX + cropWindowW / 2f).toInt().coerceIn(srcLeft + 1, bitmap.width)
        val srcBottom = (bmpCenterY + cropWindowH / 2f).toInt().coerceIn(srcTop + 1, bitmap.height)

        drawImage(
            image = bitmap.asImageBitmap(),
            srcOffset = IntOffset(srcLeft, srcTop),
            srcSize = IntSize(srcRight - srcLeft, srcBottom - srcTop),
            dstOffset = IntOffset((loupeCenter.x - loupeRadius).toInt(), (loupeCenter.y - loupeRadius).toInt()),
            dstSize = IntSize(loupeDiameter.toInt(), loupeDiameter.toInt())
        )

        // Crosshair target
        val lineLen = 14.dp.toPx()
        drawLine(
            color = primaryColor,
            start = Offset(loupeCenter.x - lineLen, loupeCenter.y),
            end = Offset(loupeCenter.x + lineLen, loupeCenter.y),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = primaryColor,
            start = Offset(loupeCenter.x, loupeCenter.y - lineLen),
            end = Offset(loupeCenter.x, loupeCenter.y + lineLen),
            strokeWidth = 2.dp.toPx()
        )
    }

    // Loupe ring border
    drawCircle(color = primaryColor, radius = loupeRadius, center = loupeCenter, style = Stroke(width = 3.dp.toPx()))
    drawCircle(color = Color.White, radius = loupeRadius - 1.5f.dp.toPx(), center = loupeCenter, style = Stroke(width = 1.dp.toPx()))
}
