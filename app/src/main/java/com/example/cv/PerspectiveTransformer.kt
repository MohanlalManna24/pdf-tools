package com.example.cv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Homography & Perspective Transformation Engine.
 * Warps skewed camera documents into perfectly rectangular, flat output pages.
 */
object PerspectiveTransformer {

    /**
     * Transform quadrilateral region in source bitmap into a flat, cropped output bitmap.
     *
     * @param src Source captured bitmap
     * @param quad Normalized quad points (0f..1f range)
     * @return Perspective corrected full-resolution document bitmap
     */
    fun transform(src: Bitmap, quad: QuadPoints): Bitmap {
        if (src.width <= 0 || src.height <= 0) return src

        val srcWidth = src.width.toFloat()
        val srcHeight = src.height.toFloat()

        // Convert normalized points (0f..1f) to source pixel coordinates
        val ptTL = floatArrayOf(quad.topLeft.x * srcWidth, quad.topLeft.y * srcHeight)
        val ptTR = floatArrayOf(quad.topRight.x * srcWidth, quad.topRight.y * srcHeight)
        val ptBR = floatArrayOf(quad.bottomRight.x * srcWidth, quad.bottomRight.y * srcHeight)
        val ptBL = floatArrayOf(quad.bottomLeft.x * srcWidth, quad.bottomLeft.y * srcHeight)

        // Compute output document dimensions based on maximum edge lengths
        val topWidth = distance(ptTL, ptTR)
        val bottomWidth = distance(ptBL, ptBR)
        val targetW = max(topWidth, bottomWidth).toInt().coerceAtLeast(100)

        val leftHeight = distance(ptTL, ptBL)
        val rightHeight = distance(ptTR, ptBR)
        val targetH = max(leftHeight, rightHeight).toInt().coerceAtLeast(100)

        // Source 4 points array
        val srcPts = floatArrayOf(
            ptTL[0], ptTL[1],
            ptTR[0], ptTR[1],
            ptBR[0], ptBR[1],
            ptBL[0], ptBL[1]
        )

        // Target rectangular 4 points array
        val dstPts = floatArrayOf(
            0f, 0f,
            targetW.toFloat(), 0f,
            targetW.toFloat(), targetH.toFloat(),
            0f, targetH.toFloat()
        )

        val matrix = Matrix()
        val success = matrix.setPolyToPoly(srcPts, 0, dstPts, 0, 4)

        return if (success) {
            try {
                val outputBitmap = Bitmap.createBitmap(targetW, targetH, src.config ?: Bitmap.Config.ARGB_8888)
                val canvas = Canvas(outputBitmap)
                val paint = Paint().apply {
                    isAntiAlias = true
                    isFilterBitmap = true
                }
                canvas.drawBitmap(src, matrix, paint)
                outputBitmap
            } catch (e: Exception) {
                e.printStackTrace()
                src
            }
        } else {
            // Fallback simple bounding box crop if matrix poly-to-poly fails
            fallbackBoundingCrop(src, quad)
        }
    }

    private fun distance(p1: FloatArray, p2: FloatArray): Float {
        val dx = p2[0] - p1[0]
        val dy = p2[1] - p1[1]
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun fallbackBoundingCrop(src: Bitmap, quad: QuadPoints): Bitmap {
        val minX = (minOf(quad.topLeft.x, quad.bottomLeft.x) * src.width).toInt().coerceIn(0, src.width - 20)
        val maxX = (maxOf(quad.topRight.x, quad.bottomRight.x) * src.width).toInt().coerceIn(minX + 20, src.width)
        val minY = (minOf(quad.topLeft.y, quad.topRight.y) * src.height).toInt().coerceIn(0, src.height - 20)
        val maxY = (maxOf(quad.bottomLeft.y, quad.bottomRight.y) * src.height).toInt().coerceIn(minY + 20, src.height)

        return try {
            Bitmap.createBitmap(src, minX, minY, maxX - minX, maxY - minY)
        } catch (e: Exception) {
            src
        }
    }
}
