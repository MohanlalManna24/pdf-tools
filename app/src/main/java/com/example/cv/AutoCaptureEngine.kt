package com.example.cv

/**
 * State of auto-capture trigger
 */
data class AutoCaptureState(
    val progress: Float, // 0.0f..1.0f
    val isHolding: Boolean,
    val isReadyToCapture: Boolean,
    val statusMessage: String
)

/**
 * Smart Auto-Capture Manager Engine.
 * Automatically snaps photo when document is fully visible, steady, and sharp.
 */
class AutoCaptureEngine(
    private val requiredHoldFrames: Int = 20, // Frames required (~0.7 - 1.0 second at 30fps)
    private val motionThreshold: Float = 0.02f,
    private val minSharpness: Float = 10.0f
) {
    private var holdCounter: Int = 0
    private var isCapturingTriggered: Boolean = false

    /**
     * Evaluate frame detection state and return AutoCaptureState.
     */
    fun evaluate(
        detectionResult: DetectionResult,
        cornerDisplacement: Float,
        isAutoModeEnabled: Boolean
    ): AutoCaptureState {
        if (!isAutoModeEnabled) {
            reset()
            return AutoCaptureState(0f, false, false, "Manual Mode")
        }

        if (isCapturingTriggered) {
            return AutoCaptureState(1f, true, true, "Capturing...")
        }

        val isDocFound = detectionResult.isDocumentFound && detectionResult.quad != null
        val isSteady = cornerDisplacement < motionThreshold
        val isSharp = detectionResult.sharpnessScore >= minSharpness

        return if (isDocFound && isSteady && isSharp) {
            holdCounter++
            val progress = (holdCounter.toFloat() / requiredHoldFrames).coerceIn(0f, 1f)

            if (holdCounter >= requiredHoldFrames) {
                isCapturingTriggered = true
                AutoCaptureState(1f, true, true, "Hold Steady - Capturing!")
            } else {
                AutoCaptureState(progress, true, false, "Hold Camera Steady...")
            }
        } else {
            // Motion detected or document lost - reset auto capture timer
            holdCounter = 0
            val msg = when {
                !isDocFound -> "Searching for document..."
                !isSteady -> "Hold steady..."
                !isSharp -> "Focusing..."
                else -> "Align document in frame"
            }
            AutoCaptureState(0f, false, false, msg)
        }
    }

    /**
     * Reset auto capture engine state after capture complete
     */
    fun reset() {
        holdCounter = 0
        isCapturingTriggered = false
    }
}
