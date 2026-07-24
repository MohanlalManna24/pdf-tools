package com.example.analyzer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.cv.AutoCaptureEngine
import com.example.cv.AutoCaptureState
import com.example.cv.CornerSmoother
import com.example.cv.DocumentDetector
import com.example.cv.QuadPoints
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * CameraX ImageAnalysis.Analyzer for real-time background document detection.
 * Executes on a dedicated background thread pool for 30 FPS non-blocking camera preview.
 */
class DocumentImageAnalyzer(
    private var isAutoModeEnabled: Boolean = false,
    private val onAnalysisResult: (smoothedQuad: QuadPoints?, confidence: Float, autoCaptureState: AutoCaptureState) -> Unit,
    private val onAutoCaptureTriggered: () -> Unit
) : ImageAnalysis.Analyzer {

    private val cornerSmoother = CornerSmoother(alpha = 0.35f)
    private val autoCaptureEngine = AutoCaptureEngine()
    private var isProcessingFrame = false

    fun setAutoModeEnabled(enabled: Boolean) {
        isAutoModeEnabled = enabled
        if (!enabled) autoCaptureEngine.reset()
    }

    fun resetAutoCapture() {
        autoCaptureEngine.reset()
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessingFrame) {
            imageProxy.close()
            return
        }

        isProcessingFrame = true
        try {
            val bitmap = imageProxyToBitmap(imageProxy)
            if (bitmap != null) {
                // 1. Run Computer Vision Detection
                val detectionResult = DocumentDetector.detectDocument(bitmap)

                // 2. Temporal Corner Smoothing (no jitter/flickering)
                val smoothedQuad = cornerSmoother.process(detectionResult)
                val cornerDisplacement = cornerSmoother.getCornerDisplacement()

                // 3. Evaluate Auto-Capture Engine
                val autoState = autoCaptureEngine.evaluate(
                    detectionResult = detectionResult,
                    cornerDisplacement = cornerDisplacement,
                    isAutoModeEnabled = isAutoModeEnabled
                )

                bitmap.recycle()

                // Notify UI on main thread
                onAnalysisResult(smoothedQuad, detectionResult.confidence, autoState)

                if (autoState.isReadyToCapture) {
                    onAutoCaptureTriggered()
                    autoCaptureEngine.reset()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isProcessingFrame = false
            imageProxy.close()
        }
    }

    /**
     * Convert CameraX YUV_420_888 ImageProxy to Bitmap
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null
        return try {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            if (imageProxy.format == ImageFormat.YUV_420_888) {
                val nv21 = yuv420888ToNv21(imageProxy)
                val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
                val out = ByteArrayOutputStream()
                yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 75, out)
                val imageBytes = out.toByteArray()
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                if (rotationDegrees != 0 && bitmap != null) {
                    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    bitmap.recycle()
                    rotated
                } else {
                    bitmap
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            torchFallback(imageProxy)
        }
    }

    private fun torchFallback(imageProxy: ImageProxy): Bitmap? {
        return try {
            val planeProxy = imageProxy.planes[0]
            val buffer: ByteBuffer = planeProxy.buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + (image.width * image.height / 2))

        yBuffer.get(nv21, 0, ySize)

        val pixelStride = uPlane.pixelStride
        val rowStride = uPlane.rowStride

        var offset = ySize
        if (pixelStride == 2) {
            // Interleaved UV plane
            vBuffer.get(nv21, offset, vSize)
        } else {
            // Non-interleaved UV plane
            val uBytes = ByteArray(uSize)
            val vBytes = ByteArray(vSize)
            uBuffer.get(uBytes)
            vBuffer.get(vBytes)

            for (i in 0 until uSize) {
                nv21[offset++] = vBytes[i]
                nv21[offset++] = uBytes[i]
            }
        }
        return nv21
    }
}
