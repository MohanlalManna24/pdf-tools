package com.example.analyzer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.cv.AutoCaptureEngine
import com.example.cv.AutoCaptureState
import com.example.cv.CornerSmoother
import com.example.cv.DocumentDetector
import com.example.cv.OpenCVManager
import com.example.cv.QuadPoints
import com.example.cv.useMat
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * CameraX ImageAnalysis.Analyzer powered by high-performance OpenCV.
 * Executes on a dedicated background thread pool for 30 FPS non-blocking camera preview.
 */
class DocumentImageAnalyzer(
    private var isAutoModeEnabled: Boolean = false,
    private val onAnalysisResult: (smoothedQuad: QuadPoints?, confidence: Float, autoCaptureState: AutoCaptureState) -> Unit,
    private val onAutoCaptureTriggered: () -> Unit
) : ImageAnalysis.Analyzer {

    private val cornerSmoother = CornerSmoother(baseAlpha = 0.30f)
    private val autoCaptureEngine = AutoCaptureEngine()
    @Volatile private var isProcessingFrame = false
    private var reusableYBuffer: ByteArray? = null

    fun setAutoModeEnabled(enabled: Boolean) {
        isAutoModeEnabled = enabled
        if (!enabled) autoCaptureEngine.reset()
    }

    fun resetAutoCapture() {
        autoCaptureEngine.reset()
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessingFrame) {
            imageProxy.close()
            return
        }

        isProcessingFrame = true
        try {
            if (OpenCVManager.isReady()) {
                val image = imageProxy.image
                if (image != null && imageProxy.format == ImageFormat.YUV_420_888) {
                    val requiredSize = image.planes[0].buffer.remaining()
                    if (reusableYBuffer == null || reusableYBuffer!!.size < requiredSize) {
                        reusableYBuffer = ByteArray(requiredSize)
                    }

                    // Extract downscaled 1-channel Grayscale Mat directly from Y-plane (sub-10ms)
                    val grayMat = OpenCVManager.imageProxyToDownscaledGrayscaleMat(
                        imageProxy = imageProxy,
                        maxDim = 480,
                        reusableBuffer = reusableYBuffer
                    )

                    if (grayMat != null) {
                        grayMat.useMat { srcMat ->
                            val detectionResult = DocumentDetector.detectDocumentFromMat(srcMat)
                            processDetectionResult(detectionResult)
                        }
                    } else {
                        fallbackBitmapAnalysis(imageProxy)
                    }
                } else {
                    fallbackBitmapAnalysis(imageProxy)
                }
            } else {
                fallbackBitmapAnalysis(imageProxy)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isProcessingFrame = false
            imageProxy.close()
        }
    }

    private fun processDetectionResult(detectionResult: com.example.cv.DetectionResult) {
        val smoothedQuad = cornerSmoother.process(detectionResult)
        val cornerDisplacement = cornerSmoother.getCornerDisplacement()

        val autoState = autoCaptureEngine.evaluate(
            detectionResult = detectionResult,
            cornerDisplacement = cornerDisplacement,
            isAutoModeEnabled = isAutoModeEnabled
        )

        onAnalysisResult(smoothedQuad, detectionResult.confidence, autoState)

        if (autoState.isReadyToCapture) {
            onAutoCaptureTriggered()
            autoCaptureEngine.reset()
        }
    }

    private fun fallbackBitmapAnalysis(imageProxy: ImageProxy) {
        val bitmap = imageProxyToBitmap(imageProxy)
        if (bitmap != null) {
            val detectionResult = DocumentDetector.detectDocument(bitmap)
            bitmap.recycle()
            processDetectionResult(detectionResult)
        }
    }

    @OptIn(ExperimentalGetImage::class)
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

    private fun yuv420888ToNv21(imageProxy: ImageProxy): ByteArray {
        val yPlane = imageProxy.planes[0]
        val uPlane = imageProxy.planes[1]
        val vPlane = imageProxy.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + (imageProxy.width * imageProxy.height / 2))

        yBuffer.get(nv21, 0, ySize)

        val pixelStride = uPlane.pixelStride

        var offset = ySize
        if (pixelStride == 2) {
            vBuffer.get(nv21, offset, vSize)
        } else {
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
