package com.example.cv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Enterprise Homography & Perspective Transformation Engine using OpenCV.
 * Warps skewed camera documents into flat rectangular output pages.
 *
 * Key Capabilities:
 * 1. Automatic Corner Re-ordering (Top-Left, Top-Right, Bottom-Right, Bottom-Left).
 * 2. OpenCV getPerspectiveTransform() + warpPerspective() with INTER_CUBIC interpolation.
 * 3. Exact aspect ratio & resolution calculations to prevent image stretching or document clipping.
 * 4. Full support for portrait and landscape document layouts.
 */
object PerspectiveTransformer {

    fun transform(src: Bitmap, quad: QuadPoints): Bitmap {
        if (src.width <= 0 || src.height <= 0) return src

        if (OpenCVManager.isReady()) {
            try {
                val srcMat = OpenCVManager.bitmapToMat(src)
                val srcWidth = src.width.toDouble()
                val srcHeight = src.height.toDouble()

                // Convert normalized quad coordinates to pixel space
                val rawPoints = arrayOf(
                    Point(quad.topLeft.x * srcWidth, quad.topLeft.y * srcHeight),
                    Point(quad.topRight.x * srcWidth, quad.topRight.y * srcHeight),
                    Point(quad.bottomRight.x * srcWidth, quad.bottomRight.y * srcHeight),
                    Point(quad.bottomLeft.x * srcWidth, quad.bottomLeft.y * srcHeight)
                )

                // 1. Automatic Corner Ordering Detection
                val sortedPoints = sortCorners(rawPoints)
                val pTL = sortedPoints[0]
                val pTR = sortedPoints[1]
                val pBR = sortedPoints[2]
                val pBL = sortedPoints[3]

                // 2. Calculate Unclipped Target Dimensions
                val topWidth = distance(pTL, pTR)
                val bottomWidth = distance(pBL, pBR)
                val targetW = max(topWidth, bottomWidth).roundToInt().coerceAtLeast(100).toDouble()

                val leftHeight = distance(pTL, pBL)
                val rightHeight = distance(pTR, pBR)
                val targetH = max(leftHeight, rightHeight).roundToInt().coerceAtLeast(100).toDouble()

                // 3. Perform OpenCV Homography Transform
                val srcMat2f = MatOfPoint2f(pTL, pTR, pBR, pBL)
                val dstMat2f = MatOfPoint2f(
                    Point(0.0, 0.0),
                    Point(targetW, 0.0),
                    Point(targetW, targetH),
                    Point(0.0, targetH)
                )

                val transformMat = Imgproc.getPerspectiveTransform(srcMat2f, dstMat2f)
                val dstMat = Mat(targetH.toInt(), targetW.toInt(), srcMat.type())

                Imgproc.warpPerspective(
                    srcMat,
                    dstMat,
                    transformMat,
                    Size(targetW, targetH),
                    Imgproc.INTER_CUBIC
                )

                val resultBitmap = OpenCVManager.matToBitmap(dstMat)

                srcMat.release()
                dstMat.release()
                transformMat.release()
                srcMat2f.release()
                dstMat2f.release()

                return resultBitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback using Android Matrix setPolyToPoly
        return fallbackTransform(src, quad)
    }

    /**
     * Automatic Corner Re-Ordering Algorithm.
     * Orders 4 raw points into canonical clockwise order:
     * Index 0: Top-Left (min sum x+y)
     * Index 1: Top-Right (min diff y-x)
     * Index 2: Bottom-Right (max sum x+y)
     * Index 3: Bottom-Left (max diff y-x)
     */
    private fun sortCorners(pts: Array<Point>): Array<Point> {
        if (pts.size != 4) return pts

        val sums = pts.map { it.x + it.y }
        val diffs = pts.map { it.y - it.x }

        val tlIdx = sums.indices.minByOrNull { sums[it] } ?: 0
        val brIdx = sums.indices.maxByOrNull { sums[it] } ?: 2
        val trIdx = diffs.indices.minByOrNull { diffs[it] } ?: 1
        val blIdx = diffs.indices.maxByOrNull { diffs[it] } ?: 3

        return arrayOf(pts[tlIdx], pts[trIdx], pts[brIdx], pts[blIdx])
    }

    private fun distance(p1: Point, p2: Point): Double {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun fallbackTransform(src: Bitmap, quad: QuadPoints): Bitmap {
        val srcWidth = src.width.toFloat()
        val srcHeight = src.height.toFloat()

        val rawPts = arrayOf(
            Point((quad.topLeft.x * srcWidth).toDouble(), (quad.topLeft.y * srcHeight).toDouble()),
            Point((quad.topRight.x * srcWidth).toDouble(), (quad.topRight.y * srcHeight).toDouble()),
            Point((quad.bottomRight.x * srcWidth).toDouble(), (quad.bottomRight.y * srcHeight).toDouble()),
            Point((quad.bottomLeft.x * srcWidth).toDouble(), (quad.bottomLeft.y * srcHeight).toDouble())
        )

        val sortedPts = sortCorners(rawPts)
        val ptTL = sortedPts[0]
        val ptTR = sortedPts[1]
        val ptBR = sortedPts[2]
        val ptBL = sortedPts[3]

        val topW = distance(ptTL, ptTR).toFloat()
        val botW = distance(ptBL, ptBR).toFloat()
        val targetW = max(topW, botW).toInt().coerceAtLeast(100)

        val leftH = distance(ptTL, ptBL).toFloat()
        val rightH = distance(ptTR, ptBR).toFloat()
        val targetH = max(leftH, rightH).toInt().coerceAtLeast(100)

        val srcPts = floatArrayOf(
            ptTL.x.toFloat(), ptTL.y.toFloat(),
            ptTR.x.toFloat(), ptTR.y.toFloat(),
            ptBR.x.toFloat(), ptBR.y.toFloat(),
            ptBL.x.toFloat(), ptBL.y.toFloat()
        )

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
                src
            }
        } else {
            src
        }
    }
}
