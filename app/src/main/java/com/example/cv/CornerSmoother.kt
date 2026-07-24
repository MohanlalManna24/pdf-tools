package com.example.cv

import androidx.compose.ui.geometry.Offset

/**
 * Adaptive Temporal Filtering & Exponential Moving Average (EMA) Corner Smoother.
 * Eliminates flickering and corner jumping across live camera frames like Adobe Scan.
 */
class CornerSmoother(
    private val baseAlpha: Float = 0.30f,
    private val holdTimeoutMs: Long = 500L
) {
    private var currentQuad: QuadPoints? = null
    private var lastUpdateTimeMs: Long = 0L
    private var lastConfidence: Float = 0f
    private var cornerDisplacement: Float = 0f
    private var stableFrameCount: Int = 0
    private var pendingJumpQuad: QuadPoints? = null
    private var pendingJumpCount: Int = 0

    /**
     * Process frame detection result and return temporally smoothed, jitter-free quad.
     */
    fun process(result: DetectionResult): QuadPoints? {
        val now = System.currentTimeMillis()

        if (result.isDocumentFound && result.quad != null) {
            val newQuad = result.quad
            val prevQuad = currentQuad

            if (prevQuad == null) {
                currentQuad = newQuad.copyQuad()
                cornerDisplacement = 1.0f
                stableFrameCount = 1
                pendingJumpCount = 0
            } else {
                // Calculate average displacement across 4 corners
                val dTL = (newQuad.topLeft - prevQuad.topLeft).getDistance()
                val dTR = (newQuad.topRight - prevQuad.topRight).getDistance()
                val dBR = (newQuad.bottomRight - prevQuad.bottomRight).getDistance()
                val dBL = (newQuad.bottomLeft - prevQuad.bottomLeft).getDistance()

                val avgDisplacement = (dTL + dTR + dBR + dBL) / 4f

                // Outlier jump rejection: Ignore sudden wild jumps unless sustained over multiple frames
                if (avgDisplacement > 0.22f && stableFrameCount >= 3) {
                    if (pendingJumpCount < 2) {
                        pendingJumpCount++
                        pendingJumpQuad = newQuad
                        // Retain previous stable quad during single-frame noise glitch
                        return currentQuad
                    }
                }
                pendingJumpCount = 0

                cornerDisplacement = avgDisplacement

                if (cornerDisplacement < 0.015f) {
                    stableFrameCount++
                } else {
                    stableFrameCount = 0
                }

                // Adaptive EMA alpha: Smaller alpha for low motion (ultra smooth), larger alpha for fast movement
                val adaptiveAlpha = when {
                    avgDisplacement < 0.01f -> 0.18f
                    avgDisplacement < 0.04f -> 0.32f
                    else -> 0.55f
                }

                currentQuad = QuadPoints(
                    topLeft = smoothOffset(prevQuad.topLeft, newQuad.topLeft, adaptiveAlpha),
                    topRight = smoothOffset(prevQuad.topRight, newQuad.topRight, adaptiveAlpha),
                    bottomRight = smoothOffset(prevQuad.bottomRight, newQuad.bottomRight, adaptiveAlpha),
                    bottomLeft = smoothOffset(prevQuad.bottomLeft, newQuad.bottomLeft, adaptiveAlpha)
                )
            }

            lastUpdateTimeMs = now
            lastConfidence = result.confidence
        } else {
            // Temporary frame drop: hold last valid quad within hold timeout window
            if (currentQuad != null && (now - lastUpdateTimeMs) > holdTimeoutMs) {
                currentQuad = null
                cornerDisplacement = 1.0f
                stableFrameCount = 0
                pendingJumpCount = 0
            }
        }

        return currentQuad
    }

    fun getSmoothedQuad(): QuadPoints? = currentQuad

    fun getCornerDisplacement(): Float = cornerDisplacement

    fun getStableFrameCount(): Int = stableFrameCount

    fun reset() {
        currentQuad = null
        lastUpdateTimeMs = 0L
        lastConfidence = 0f
        cornerDisplacement = 1.0f
        stableFrameCount = 0
        pendingJumpCount = 0
    }

    private fun smoothOffset(prev: Offset, target: Offset, alpha: Float): Offset {
        return Offset(
            x = prev.x + alpha * (target.x - prev.x),
            y = prev.y + alpha * (target.y - prev.y)
        )
    }
}
