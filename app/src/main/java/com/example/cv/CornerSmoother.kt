package com.example.cv

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/**
 * Temporal filtering and corner smoothing engine for live camera preview overlays.
 * Prevents jittering/flickering using Exponential Moving Average (EMA) and confidence holding.
 */
class CornerSmoother(
    private val alpha: Float = 0.35f, // Smoothing weight (lower = smoother, higher = faster response)
    private val holdTimeoutMs: Long = 400L // Duration to hold last quad on temporary frame loss
) {
    private var currentQuad: QuadPoints? = null
    private var lastUpdateTimeMs: Long = 0L
    private var lastConfidence: Float = 0f
    private var cornerDisplacement: Float = 0f
    private var stableFrameCount: Int = 0

    /**
     * Process new frame detection result and return smoothed quadrilateral.
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
            } else {
                // Calculate corner movement displacement across frames
                val dTL = (newQuad.topLeft - prevQuad.topLeft).getDistance()
                val dTR = (newQuad.topRight - prevQuad.topRight).getDistance()
                val dBR = (newQuad.bottomRight - prevQuad.bottomRight).getDistance()
                val dBL = (newQuad.bottomLeft - prevQuad.bottomLeft).getDistance()

                cornerDisplacement = (dTL + dTR + dBR + dBL) / 4f

                if (cornerDisplacement < 0.015f) {
                    stableFrameCount++
                } else {
                    stableFrameCount = 0
                }

                // Exponential Moving Average (EMA) corner position update
                currentQuad = QuadPoints(
                    topLeft = smoothOffset(prevQuad.topLeft, newQuad.topLeft, alpha),
                    topRight = smoothOffset(prevQuad.topRight, newQuad.topRight, alpha),
                    bottomRight = smoothOffset(prevQuad.bottomRight, newQuad.bottomRight, alpha),
                    bottomLeft = smoothOffset(prevQuad.bottomLeft, newQuad.bottomLeft, alpha)
                )
            }

            lastUpdateTimeMs = now
            lastConfidence = result.confidence
        } else {
            // Temporary detection loss: keep last stable quad within hold timeout window
            if (currentQuad != null && (now - lastUpdateTimeMs) > holdTimeoutMs) {
                currentQuad = null
                cornerDisplacement = 1.0f
                stableFrameCount = 0
            }
        }

        return currentQuad
    }

    /**
     * Get smoothed quad
     */
    fun getSmoothedQuad(): QuadPoints? = currentQuad

    /**
     * Get recent corner movement displacement (0.0f = completely motionless, > 0.05f = moving)
     */
    fun getCornerDisplacement(): Float = cornerDisplacement

    /**
     * Get consecutive stable frames count
     */
    fun getStableFrameCount(): Int = stableFrameCount

    /**
     * Reset smoother state
     */
    fun reset() {
        currentQuad = null
        lastUpdateTimeMs = 0L
        lastConfidence = 0f
        cornerDisplacement = 1.0f
        stableFrameCount = 0
    }

    private fun smoothOffset(prev: Offset, target: Offset, alpha: Float): Offset {
        return Offset(
            x = prev.x + alpha * (target.x - prev.x),
            y = prev.y + alpha * (target.y - prev.y)
        )
    }
}
