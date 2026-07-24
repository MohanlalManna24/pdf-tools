package com.example.cv

import androidx.compose.ui.geometry.Offset

/**
 * 4 normalized corner points of a detected document (coordinates in 0.0f..1.0f range).
 */
data class QuadPoints(
    var topLeft: Offset,
    var topRight: Offset,
    var bottomRight: Offset,
    var bottomLeft: Offset
) {
    /**
     * Return list of corners in canonical order: [TL, TR, BR, BL]
     */
    fun asList(): List<Offset> = listOf(topLeft, topRight, bottomRight, bottomLeft)

    /**
     * Calculate quadrilateral perimeter in normalized units
     */
    fun perimeter(): Float {
        val d1 = (topRight - topLeft).getDistance()
        val d2 = (bottomRight - topRight).getDistance()
        val d3 = (bottomLeft - bottomRight).getDistance()
        val d4 = (topLeft - bottomLeft).getDistance()
        return d1 + d2 + d3 + d4
    }

    /**
     * Check if quad corners form a convex quadrilateral
     */
    fun isConvex(): Boolean {
        val pts = asList()
        var sign = false
        val n = pts.size
        for (i in 0 until n) {
            val p1 = pts[i]
            val p2 = pts[(i + 1) % n]
            val p3 = pts[(i + 2) % n]
            val cross = (p2.x - p1.x) * (p3.y - p2.y) - (p2.y - p1.y) * (p3.x - p2.x)
            if (i == 0) {
                sign = cross > 0
            } else if ((cross > 0) != sign) {
                return false
            }
        }
        return true
    }

    /**
     * Creates a deep copy of quad points
     */
    fun copyQuad(): QuadPoints = QuadPoints(
        topLeft = Offset(topLeft.x, topLeft.y),
        topRight = Offset(topRight.x, topRight.y),
        bottomRight = Offset(bottomRight.x, bottomRight.y),
        bottomLeft = Offset(bottomLeft.x, bottomLeft.y)
    )
}

/**
 * Result of document detection frame analysis
 */
data class DetectionResult(
    val quad: QuadPoints?,
    val confidence: Float,
    val isDocumentFound: Boolean,
    val sharpnessScore: Float = 0f,
    val processingTimeMs: Long = 0L
)

/**
 * Available document scan filters comparable to Adobe Scan
 */
enum class FilterType(val displayName: String) {
    ORIGINAL("Original"),
    AUTO_ENHANCE("Auto Enhance"),
    MAGIC_COLOR("Magic Color"),
    BLACK_WHITE("B&W"),
    GRAYSCALE("Grayscale"),
    HIGH_CONTRAST("High Contrast")
}
