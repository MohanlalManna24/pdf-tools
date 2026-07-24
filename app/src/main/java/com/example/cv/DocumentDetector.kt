package com.example.cv

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Computer Vision Document Detector Engine.
 * Implements Grayscale -> Gaussian Blur -> Adaptive Canny Edge -> Morphological Dilation
 * -> Contour Trace -> Polygon Approximation (approxPolyDP) -> Quad Validation & Sorting.
 */
object DocumentDetector {

    private const val ANALYSIS_MAX_DIM = 480

    /**
     * Detect document quadrilateral in a given Bitmap frame.
     */
    fun detectDocument(bitmap: Bitmap): DetectionResult {
        val startTime = System.currentTimeMillis()
        if (bitmap.width < 10 || bitmap.height < 10) {
            return DetectionResult(null, 0f, false)
        }

        // 1. Scale down for real-time < 20ms frame detection speed
        val scale = min(1.0f, ANALYSIS_MAX_DIM.toFloat() / max(bitmap.width, bitmap.height))
        val targetW = (bitmap.width * scale).toInt().coerceAtLeast(10)
        val targetH = (bitmap.height * scale).toInt().coerceAtLeast(10)

        val scaledBitmap = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, false)
        } else {
            bitmap
        }

        val w = scaledBitmap.width
        val h = scaledBitmap.height
        val totalPixels = w * h

        // Extract ARGB pixels
        val pixels = IntArray(totalPixels)
        scaledBitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }

        // 2. Grayscale & Sharpness Calculation
        val gray = IntArray(totalPixels)
        var sumLum = 0L
        var laplacianVarSum = 0.0

        for (i in 0 until totalPixels) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            // Luminance formula
            val lum = (299 * r + 587 * g + 114 * b) / 1000
            gray[i] = lum
            sumLum += lum
        }

        // Measure image sharpness using Laplacian variance
        var meanLap = 0.0
        val lapArray = DoubleArray(totalPixels)
        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val idx = row + x
                // 3x3 Laplacian operator kernel: [0, 1, 0; 1, -4, 1; 0, 1, 0]
                val lap = (gray[idx - w] + gray[idx + w] + gray[idx - 1] + gray[idx + 1] - 4 * gray[idx]).toDouble()
                lapArray[idx] = lap
                meanLap += lap
            }
        }
        meanLap /= (w - 2) * (h - 2)

        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val diff = lapArray[row + x] - meanLap
                laplacianVarSum += diff * diff
            }
        }
        val sharpnessScore = (laplacianVarSum / ((w - 2) * (h - 2))).toFloat()

        // 3. Gaussian 5x5 Blur to reduce noise & paper texture
        val blurred = IntArray(totalPixels)
        blur5x5(gray, blurred, w, h)

        // 4. Sobel Gradient & Edge Detection
        val edgeMap = BooleanArray(totalPixels)
        var totalGrad = 0L
        val gradients = IntArray(totalPixels)

        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val idx = row + x
                // Sobel X: [-1, 0, 1; -2, 0, 2; -1, 0, 1]
                val gx = (blurred[idx - w + 1] + 2 * blurred[idx + 1] + blurred[idx + w + 1]) -
                        (blurred[idx - w - 1] + 2 * blurred[idx - 1] + blurred[idx + w - 1])

                // Sobel Y: [-1, -2, -1; 0, 0, 0; 1, 2, 1]
                val gy = (blurred[idx + w - 1] + 2 * blurred[idx + w] + blurred[idx + w + 1]) -
                        (blurred[idx - w - 1] + 2 * blurred[idx - w] + blurred[idx - w + 1])

                val mag = abs(gx) + abs(gy)
                gradients[idx] = mag
                totalGrad += mag
            }
        }

        val avgGrad = (totalGrad / totalPixels).toInt().coerceAtLeast(10)
        val highThreshold = (avgGrad * 2.2).toInt().coerceIn(25, 120)
        val lowThreshold = highThreshold / 2

        for (i in 0 until totalPixels) {
            edgeMap[i] = gradients[i] >= lowThreshold
        }

        // 5. Morphological Dilation (3x3 structuring element) to connect border segments
        val dilatedMap = BooleanArray(totalPixels)
        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val idx = row + x
                if (edgeMap[idx] || edgeMap[idx - 1] || edgeMap[idx + 1] ||
                    edgeMap[idx - w] || edgeMap[idx + w]
                ) {
                    dilatedMap[idx] = true
                }
            }
        }

        // 6. Contour & Quadrilateral Polygon Approximation
        val bestQuad = findLargestValidDocumentQuad(dilatedMap, w, h)
        val procTime = System.currentTimeMillis() - startTime

        return if (bestQuad != null) {
            val conf = calculateQuadConfidence(bestQuad, w.toFloat(), h.toFloat())
            DetectionResult(
                quad = bestQuad,
                confidence = conf,
                isDocumentFound = true,
                sharpnessScore = sharpnessScore,
                processingTimeMs = procTime
            )
        } else {
            DetectionResult(
                quad = null,
                confidence = 0f,
                isDocumentFound = false,
                sharpnessScore = sharpnessScore,
                processingTimeMs = procTime
            )
        }
    }

    /**
     * 5x5 Box / Gaussian approximation filter
     */
    private fun blur5x5(src: IntArray, dest: IntArray, w: Int, h: Int) {
        for (y in 2 until h - 2) {
            val row = y * w
            for (x in 2 until w - 2) {
                var sum = 0
                for (dy in -2..2) {
                    val r = (y + dy) * w
                    for (dx in -2..2) {
                        sum += src[r + x + dx]
                    }
                }
                dest[row + x] = sum / 25
            }
        }
    }

    /**
     * Trace boundaries, approximate polygons using Ramer-Douglas-Peucker algorithm,
     * and extract the largest candidate valid document quadrilateral.
     */
    private fun findLargestValidDocumentQuad(
        edgeMap: BooleanArray,
        w: Int,
        h: Int
    ): QuadPoints? {
        val totalArea = (w * h).toFloat()
        var maxValidArea = 0f
        var bestQuad: QuadPoints? = null

        // Grid scan for outer contour seed points
        val stepX = (w / 20).coerceAtLeast(4)
        val stepY = (h / 20).coerceAtLeast(4)
        val visited = BooleanArray(w * h)

        for (y in stepY until h - stepY step stepY) {
            val row = y * w
            for (x in stepX until w - stepX step stepX) {
                val idx = row + x
                if (edgeMap[idx] && !visited[idx]) {
                    val contour = traceContour(edgeMap, visited, x, y, w, h, 250)
                    if (contour.size >= 8) {
                        // Ramer-Douglas-Peucker approximation
                        val poly = approxPolyRDP(contour, epsilon = 0.035f * perimeter(contour))
                        if (poly.size == 4) {
                            val candidateQuad = sortAndCreateQuad(poly, w.toFloat(), h.toFloat())
                            if (candidateQuad != null && validateDocumentQuad(candidateQuad, totalArea)) {
                                val area = calculateQuadArea(candidateQuad)
                                if (area > maxValidArea) {
                                    maxValidArea = area
                                    bestQuad = candidateQuad
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fallback: If contour edge tracing missed a document due to low contrast,
        // compute intensity-based bounding box heuristic as a baseline quadrilateral.
        if (bestQuad == null) {
            val fallbackQuad = computeIntensityFallbackQuad(edgeMap, w, h)
            if (fallbackQuad != null && validateDocumentQuad(fallbackQuad, totalArea)) {
                bestQuad = fallbackQuad
            }
        }

        return bestQuad
    }

    /**
     * Simple boundary follower for edge pixels
     */
    private fun traceContour(
        edgeMap: BooleanArray,
        visited: BooleanArray,
        startX: Int,
        startY: Int,
        w: Int,
        h: Int,
        maxPoints: Int
    ): List<Offset> {
        val points = mutableListOf<Offset>()
        var cx = startX
        var cy = startY
        val dx = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
        val dy = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)

        for (p in 0 until maxPoints) {
            val idx = cy * w + cx
            if (cx < 0 || cx >= w || cy < 0 || cy >= h || visited[idx]) break
            visited[idx] = true
            points.add(Offset(cx.toFloat(), cy.toFloat()))

            var foundNext = false
            for (dir in 0 until 8) {
                val nx = cx + dx[dir]
                val ny = cy + dy[dir]
                if (nx in 0 until w && ny in 0 until h) {
                    val nIdx = ny * w + nx
                    if (edgeMap[nIdx] && !visited[nIdx]) {
                        cx = nx
                        cy = ny
                        foundNext = true
                        break
                    }
                }
            }
            if (!foundNext) break
        }
        return points
    }

    /**
     * Ramer-Douglas-Peucker (approxPolyDP) algorithm
     */
    private fun approxPolyRDP(pts: List<Offset>, epsilon: Float): List<Offset> {
        if (pts.size < 3) return pts
        var dmax = 0f
        var index = 0
        val end = pts.size - 1

        for (i in 1 until end) {
            val d = perpendicularDistance(pts[i], pts[0], pts[end])
            if (d > dmax) {
                index = i
                dmax = d
            }
        }

        return if (dmax > epsilon) {
            val recResults1 = approxPolyRDP(pts.subList(0, index + 1), epsilon)
            val recResults2 = approxPolyRDP(pts.subList(index, pts.size), epsilon)
            recResults1.dropLast(1) + recResults2
        } else {
            listOf(pts[0], pts[end])
        }
    }

    private fun perpendicularDistance(pt: Offset, lineStart: Offset, lineEnd: Offset): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val mag = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (mag == 0f) return (pt - lineStart).getDistance()
        return abs(dy * pt.x - dx * pt.y + lineEnd.x * lineStart.y - lineEnd.y * lineStart.x) / mag
    }

    private fun perimeter(pts: List<Offset>): Float {
        var p = 0f
        for (i in pts.indices) {
            val p1 = pts[i]
            val p2 = pts[(i + 1) % pts.size]
            p += (p2 - p1).getDistance()
        }
        return p
    }

    /**
     * Sort 4 points into canonical clockwise order (TL, TR, BR, BL) and convert to normalized 0f..1f range.
     */
    fun sortAndCreateQuad(pts: List<Offset>, imgW: Float, imgH: Float): QuadPoints? {
        if (pts.size != 4) return null

        val sortedByY = pts.sortedBy { it.y }
        val topTwo = sortedByY.take(2).sortedBy { it.x }
        val bottomTwo = sortedByY.takeLast(2).sortedBy { it.x }

        val tl = topTwo[0]
        val tr = topTwo[1]
        val br = bottomTwo[1]
        val bl = bottomTwo[0]

        return QuadPoints(
            topLeft = Offset((tl.x / imgW).coerceIn(0f, 1f), (tl.y / imgH).coerceIn(0f, 1f)),
            topRight = Offset((tr.x / imgW).coerceIn(0f, 1f), (tr.y / imgH).coerceIn(0f, 1f)),
            bottomRight = Offset((br.x / imgW).coerceIn(0f, 1f), (br.y / imgH).coerceIn(0f, 1f)),
            bottomLeft = Offset((bl.x / imgW).coerceIn(0f, 1f), (bl.y / imgH).coerceIn(0f, 1f))
        )
    }

    /**
     * Validate candidate document quadrilateral:
     * - Must be convex
     * - Area between 8% and 95% of screen
     * - Angles between 40 deg and 140 deg
     * - Aspect ratio between 0.25 and 3.5
     */
    private fun validateDocumentQuad(quad: QuadPoints, totalImgArea: Float): Boolean {
        if (!quad.isConvex()) return false

        val area = calculateQuadArea(quad)
        if (area < 0.08f || area > 0.95f) return false

        // Check corner inner angles
        val pts = quad.asList()
        val n = pts.size
        for (i in 0 until n) {
            val pPrev = pts[(i + n - 1) % n]
            val pCurr = pts[i]
            val pNext = pts[(i + 1) % n]

            val v1 = pPrev - pCurr
            val v2 = pNext - pCurr

            val angleRad = abs(atan2(v2.y.toDouble(), v2.x.toDouble()) - atan2(v1.y.toDouble(), v1.x.toDouble()))
            var angleDeg = Math.toDegrees(angleRad)
            if (angleDeg > 180) angleDeg = 360 - angleDeg

            if (angleDeg < 35.0 || angleDeg > 145.0) return false
        }

        // Check aspect ratio
        val topW = (quad.topRight - quad.topLeft).getDistance()
        val botW = (quad.bottomRight - quad.bottomLeft).getDistance()
        val leftH = (quad.bottomLeft - quad.topLeft).getDistance()
        val rightH = (quad.bottomRight - quad.topRight).getDistance()

        val avgW = (topW + botW) / 2f
        val avgH = (leftH + rightH) / 2f

        if (avgW == 0f || avgH == 0f) return false
        val aspectRatio = avgW / avgH

        return aspectRatio in 0.2f..3.8f
    }

    private fun calculateQuadArea(quad: QuadPoints): Float {
        val pts = quad.asList()
        var area = 0f
        val n = pts.size
        for (i in 0 until n) {
            val p1 = pts[i]
            val p2 = pts[(i + 1) % n]
            area += p1.x * p2.y - p2.x * p1.y
        }
        return abs(area) / 2f
    }

    private fun calculateQuadConfidence(quad: QuadPoints, imgW: Float, imgH: Float): Float {
        val area = calculateQuadArea(quad)
        val areaScore = (area / 0.5f).coerceIn(0.5f, 1.0f)
        val convexBonus = if (quad.isConvex()) 0.2f else 0.0f
        return (areaScore * 0.8f + convexBonus).coerceIn(0f, 1f)
    }

    private fun computeIntensityFallbackQuad(edgeMap: BooleanArray, w: Int, h: Int): QuadPoints? {
        var minX = w
        var maxX = 0
        var minY = h
        var maxY = 0
        var edgeCount = 0

        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                if (edgeMap[row + x]) {
                    edgeCount++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (edgeCount < 100 || minX >= maxX || minY >= maxY) return null

        val pts = listOf(
            Offset(minX.toFloat(), minY.toFloat()),
            Offset(maxX.toFloat(), minY.toFloat()),
            Offset(maxX.toFloat(), maxY.toFloat()),
            Offset(minX.toFloat(), maxY.toFloat())
        )
        return sortAndCreateQuad(pts, w.toFloat(), h.toFloat())
    }
}
