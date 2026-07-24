package com.example.cv

/**
 * State of auto-capture trigger
 */
data class AutoCaptureState(
    val progress: Float, // 0.0f..1.0f (fraction of 8 stable frames)
    val holdFrameCount: Int,
    val isHolding: Boolean,
    val isReadyToCapture: Boolean,
    val statusMessage: String
)

/**
 * Intelligent Auto-Capture Manager Engine.
 *
 * Rules:
 * 1. Document detected: isDocumentFound == true & quad != null
 * 2. Confidence > threshold (e.g. >= 0.50f)
 * 3. Camera stable & no movement: cornerDisplacement < motionThreshold
 * 4. Corners stable for 8 consecutive frames
 * 5. Image sharp: sharpnessScore >= minSharpness
 * 6. Instant Cancellation: If movement or instability resumes, reset hold counter to 0 immediately.
 */
class AutoCaptureEngine(
    private val requiredHoldFrames: Int = 8,
    private val motionThreshold: Float = 0.015f,
    private val minConfidenceThreshold: Float = 0.50f,
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
            return AutoCaptureState(0f, 0, false, false, "Manual Mode")
        }

        if (isCapturingTriggered) {
            return AutoCaptureState(1f, requiredHoldFrames, true, true, "Capturing...")
        }

        val isDocFound = detectionResult.isDocumentFound && detectionResult.quad != null
        val isHighConfidence = detectionResult.confidence >= minConfidenceThreshold
        val isCameraStable = cornerDisplacement < motionThreshold
        val isSharp = detectionResult.sharpnessScore >= minSharpness

        val isValidFrame = isDocFound && isHighConfidence && isCameraStable && isSharp

        return if (isValidFrame) {
            holdCounter++
            val progress = (holdCounter.toFloat() / requiredHoldFrames).coerceIn(0f, 1f)

            if (holdCounter >= requiredHoldFrames) {
                isCapturingTriggered = true
                AutoCaptureState(1f, holdCounter, true, true, "Hold Steady - Auto Capturing!")
            } else {
                val remaining = requiredHoldFrames - holdCounter
                AutoCaptureState(progress, holdCounter, true, false, "Hold Steady ($remaining)")
            }
        } else {
            // Motion detected, document lost, or image blurry -> Cancel capture & reset immediately!
            holdCounter = 0
            isCapturingTriggered = false

            val msg = when {
                !isDocFound -> "Searching for document..."
                !isHighConfidence -> "Align document in frame"
                !isCameraStable -> "Hold camera steady..."
                !isSharp -> "Focusing camera..."
                else -> "Hold camera steady..."
            }
            AutoCaptureState(0f, 0, false, false, msg)
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
