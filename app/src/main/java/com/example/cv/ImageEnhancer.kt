package com.example.cv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.min

/**
 * Enterprise Production Scanner Image Enhancer Engine.
 *
 * Implements high-performance OpenCV document processing pipelines:
 * - CLAHE (Contrast Limited Adaptive Histogram Equalization)
 * - Morphological Close & Division Illumination/Shadow Removal
 * - Gaussian / Adaptive Thresholding for crisp B&W text binarization
 * - Bilateral Filter & Median Blur Noise Reduction
 * - Unsharp Mask Sharpening for maximum document legibility
 * - Magic Color & Luminance/Saturation boosts
 */
object ImageEnhancer {

    /**
     * Apply selected FilterType to a document page bitmap.
     */
    fun applyFilter(src: Bitmap, filterType: FilterType): Bitmap {
        if (src.width <= 0 || src.height <= 0) return src

        if (!OpenCVManager.isReady()) {
            OpenCVManager.init()
        }

        return when (filterType) {
            FilterType.ORIGINAL -> src
            FilterType.AUTO_ENHANCE -> applyAutoEnhance(src)
            FilterType.MAGIC_COLOR -> applyMagicColor(src)
            FilterType.BLACK_WHITE -> applyAdaptiveBW(src)
            FilterType.GRAYSCALE -> applyGrayscale(src)
            FilterType.HIGH_CONTRAST -> applyHighContrast(src)
        }
    }

    /**
     * Auto Enhance:
     * - Shadow Reduction via Morphological Closing background division
     * - CLAHE contrast adjustment on Luminance (Y) channel
     * - Bilateral Filter noise reduction
     * - Unsharp Mask sharpening for ultra-readable text
     */
    fun applyAutoEnhance(src: Bitmap): Bitmap {
        if (!OpenCVManager.isReady()) return fallbackAutoEnhance(src)

        val srcMat = Mat()
        val rgbMat = Mat()
        val ycrcbMat = Mat()
        val bgMat = Mat()
        val normY = Mat()
        val enhancedY = Mat()
        val denoisedY = Mat()
        val blurredY = Mat()
        val sharpY = Mat()
        val dstMat = Mat()

        return try {
            val mat = OpenCVManager.bitmapToMat(src)
            mat.copyTo(srcMat)
            mat.release()

            // Convert RGBA to YCrCb to isolate luminance
            Imgproc.cvtColor(srcMat, rgbMat, Imgproc.COLOR_RGBA2RGB)
            Imgproc.cvtColor(rgbMat, ycrcbMat, Imgproc.COLOR_RGB2YCrCb)

            val channels = ArrayList<Mat>()
            Core.split(ycrcbMat, channels)
            val yChannel = channels[0]

            // 1. Shadow Reduction & Illumination Normalization
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(25.0, 25.0))
            Imgproc.morphologyEx(yChannel, bgMat, Imgproc.MORPH_CLOSE, kernel)
            kernel.release()

            Core.divide(yChannel, bgMat, normY, 255.0)

            // 2. CLAHE (Contrast Limited Adaptive Histogram Equalization)
            val clahe = Imgproc.createCLAHE(2.5, Size(8.0, 8.0))
            clahe.apply(normY, enhancedY)
            clahe.collectGarbage()

            // 3. Bilateral Filter Noise Reduction
            Imgproc.bilateralFilter(enhancedY, denoisedY, 5, 50.0, 50.0)

            // 4. Unsharp Mask Sharpening
            Imgproc.GaussianBlur(denoisedY, blurredY, Size(0.0, 0.0), 3.0)
            Core.addWeighted(denoisedY, 1.4, blurredY, -0.4, 0.0, sharpY)

            // Reassemble YCrCb channels
            channels[0] = sharpY
            Core.merge(channels, ycrcbMat)

            for (c in channels) {
                if (c != sharpY) c.release()
            }

            Imgproc.cvtColor(ycrcbMat, rgbMat, Imgproc.COLOR_YCrCb2RGB)
            Imgproc.cvtColor(rgbMat, dstMat, Imgproc.COLOR_RGB2RGBA)

            OpenCVManager.matToBitmap(dstMat)
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackAutoEnhance(src)
        } finally {
            srcMat.release()
            rgbMat.release()
            ycrcbMat.release()
            bgMat.release()
            normY.release()
            enhancedY.release()
            denoisedY.release()
            blurredY.release()
            sharpY.release()
            dstMat.release()
        }
    }

    /**
     * Magic Color: Adobe Scan style filter.
     * Eliminates uneven shadows, paper creases, and yellow tinting while retaining vibrant text & logo colors.
     */
    fun applyMagicColor(src: Bitmap): Bitmap {
        if (!OpenCVManager.isReady()) return fallbackMagicColor(src)

        val srcMat = Mat()
        val rgbMat = Mat()
        val labMat = Mat()
        val bgL = Mat()
        val normL = Mat()
        val enhancedL = Mat()
        val sharpL = Mat()
        val dstMat = Mat()

        return try {
            val mat = OpenCVManager.bitmapToMat(src)
            mat.copyTo(srcMat)
            mat.release()

            // Convert RGBA to LAB color space
            Imgproc.cvtColor(srcMat, rgbMat, Imgproc.COLOR_RGBA2RGB)
            Imgproc.cvtColor(rgbMat, labMat, Imgproc.COLOR_RGB2Lab)

            val channels = ArrayList<Mat>()
            Core.split(labMat, channels)
            val lChannel = channels[0]

            // 1. Background Illumination Normalization on Lightness channel
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(31.0, 31.0))
            Imgproc.morphologyEx(lChannel, bgL, Imgproc.MORPH_CLOSE, kernel)
            kernel.release()

            Core.divide(lChannel, bgL, normL, 255.0)

            // 2. CLAHE for text contrast
            val clahe = Imgproc.createCLAHE(3.0, Size(8.0, 8.0))
            clahe.apply(normL, enhancedL)
            clahe.collectGarbage()

            // 3. Unsharp Mask Sharpening
            val blurredL = Mat()
            Imgproc.GaussianBlur(enhancedL, blurredL, Size(0.0, 0.0), 2.5)
            Core.addWeighted(enhancedL, 1.5, blurredL, -0.5, 0.0, sharpL)
            blurredL.release()

            channels[0] = sharpL
            Core.merge(channels, labMat)

            for (c in channels) {
                if (c != sharpL) c.release()
            }

            Imgproc.cvtColor(labMat, rgbMat, Imgproc.COLOR_Lab2RGB)
            Imgproc.cvtColor(rgbMat, dstMat, Imgproc.COLOR_RGB2RGBA)

            val tempBmp = OpenCVManager.matToBitmap(dstMat)

            // Additional background whitening pass
            fallbackMagicColor(tempBmp)
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackMagicColor(src)
        } finally {
            srcMat.release()
            rgbMat.release()
            labMat.release()
            bgL.release()
            normL.release()
            enhancedL.release()
            sharpL.release()
            dstMat.release()
        }
    }

    /**
     * Adaptive Black & White (Gaussian Adaptive Binarization):
     * Removes shadows, paper texture, and salt-and-pepper noise to generate crisp black text on white background.
     */
    fun applyAdaptiveBW(src: Bitmap): Bitmap {
        if (!OpenCVManager.isReady()) return fallbackAdaptiveBW(src)

        val srcMat = Mat()
        val grayMat = Mat()
        val denoisedMat = Mat()
        val bgMat = Mat()
        val normMat = Mat()
        val bwMat = Mat()
        val dstMat = Mat()

        return try {
            val mat = OpenCVManager.bitmapToMat(src)
            mat.copyTo(srcMat)
            mat.release()

            Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY)

            // 1. Noise Reduction
            Imgproc.medianBlur(grayMat, denoisedMat, 3)

            // 2. Shadow Removal & Illumination Correction
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(31.0, 31.0))
            Imgproc.morphologyEx(denoisedMat, bgMat, Imgproc.MORPH_CLOSE, kernel)
            kernel.release()

            Core.divide(denoisedMat, bgMat, normMat, 255.0)

            // 3. Brightness Normalization
            Core.normalize(normMat, normMat, 0.0, 255.0, Core.NORM_MINMAX)

            // 4. Adaptive Thresholding
            Imgproc.adaptiveThreshold(
                normMat,
                bwMat,
                255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,
                21,
                12.0
            )

            Imgproc.cvtColor(bwMat, dstMat, Imgproc.COLOR_GRAY2RGBA)

            OpenCVManager.matToBitmap(dstMat)
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackAdaptiveBW(src)
        } finally {
            srcMat.release()
            grayMat.release()
            denoisedMat.release()
            bgMat.release()
            normMat.release()
            bwMat.release()
            dstMat.release()
        }
    }

    /**
     * Grayscale Filter:
     * High-clarity grayscale document with shadow reduction, CLAHE, and noise smoothing.
     */
    fun applyGrayscale(src: Bitmap): Bitmap {
        if (!OpenCVManager.isReady()) return fallbackGrayscale(src)

        val srcMat = Mat()
        val grayMat = Mat()
        val bgMat = Mat()
        val normMat = Mat()
        val enhancedMat = Mat()
        val denoisedMat = Mat()
        val blurredMat = Mat()
        val sharpMat = Mat()
        val dstMat = Mat()

        return try {
            val mat = OpenCVManager.bitmapToMat(src)
            mat.copyTo(srcMat)
            mat.release()

            Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY)

            // 1. Shadow Removal
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(25.0, 25.0))
            Imgproc.morphologyEx(grayMat, bgMat, Imgproc.MORPH_CLOSE, kernel)
            kernel.release()

            Core.divide(grayMat, bgMat, normMat, 255.0)

            // 2. CLAHE
            val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
            clahe.apply(normMat, enhancedMat)
            clahe.collectGarbage()

            // 3. Bilateral Filter
            Imgproc.bilateralFilter(enhancedMat, denoisedMat, 5, 40.0, 40.0)

            // 4. Sharpen
            Imgproc.GaussianBlur(denoisedMat, blurredMat, Size(0.0, 0.0), 3.0)
            Core.addWeighted(denoisedMat, 1.4, blurredMat, -0.4, 0.0, sharpMat)

            Imgproc.cvtColor(sharpMat, dstMat, Imgproc.COLOR_GRAY2RGBA)

            OpenCVManager.matToBitmap(dstMat)
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackGrayscale(src)
        } finally {
            srcMat.release()
            grayMat.release()
            bgMat.release()
            normMat.release()
            enhancedMat.release()
            denoisedMat.release()
            blurredMat.release()
            sharpMat.release()
            dstMat.release()
        }
    }

    /**
     * High Contrast Monochrome Filter.
     */
    fun applyHighContrast(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint().apply {
            val cm = ColorMatrix()
            cm.setSaturation(0f)
            val scale = 2.4f
            val translate = -128f * scale + 128f
            val bwArray = floatArrayOf(
                scale, scale, scale, 0f, translate,
                scale, scale, scale, 0f, translate,
                scale, scale, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
            cm.set(bwArray)
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    // --- Android Fallback Routines ---

    private fun fallbackAutoEnhance(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint().apply {
            val cm = ColorMatrix()
            val contrast = 1.30f
            val brightness = 12f
            val array = floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
            cm.set(array)
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    private fun fallbackMagicColor(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val total = w * h
        val pixels = IntArray(total)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val outPixels = IntArray(total)

        for (i in 0 until total) {
            val p = pixels[i]
            var r = (p shr 16) and 0xFF
            var g = (p shr 8) and 0xFF
            var b = p and 0xFF

            val lum = (299 * r + 587 * g + 114 * b) / 1000

            if (lum > 170) {
                val boost = ((lum - 170) * 1.6f).toInt()
                r = min(255, r + boost)
                g = min(255, g + boost)
                b = min(255, b + boost)
            } else if (lum < 110) {
                r = max(0, (r * 0.75f).toInt())
                g = max(0, (g * 0.75f).toInt())
                b = max(0, (b * 0.75f).toInt())
            } else {
                r = min(255, max(0, ((r - 128) * 1.25f + 128).toInt()))
                g = min(255, max(0, ((g - 128) * 1.25f + 128).toInt()))
                b = min(255, max(0, ((b - 128) * 1.25f + 128).toInt()))
            }

            outPixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        val dest = Bitmap.createBitmap(w, h, src.config ?: Bitmap.Config.ARGB_8888)
        dest.setPixels(outPixels, 0, w, 0, 0, w, h)
        return dest
    }

    private fun fallbackAdaptiveBW(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val total = w * h
        val pixels = IntArray(total)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val integral = LongArray((w + 1) * (h + 1))
        val gray = IntArray(total)

        for (y in 0 until h) {
            val row = y * w
            var rowSum = 0L
            for (x in 0 until w) {
                val p = pixels[row + x]
                val lum = (299 * ((p shr 16) and 0xFF) + 587 * ((p shr 8) and 0xFF) + 114 * (p and 0xFF)) / 1000
                gray[row + x] = lum
                rowSum += lum
                integral[(y + 1) * (w + 1) + (x + 1)] = integral[y * (w + 1) + (x + 1)] + rowSum
            }
        }

        val outPixels = IntArray(total)
        val s = (w / 16).coerceAtLeast(8)
        val t = 0.15f

        for (y in 0 until h) {
            val row = y * w
            val y1 = max(0, y - s)
            val y2 = min(h - 1, y + s)

            for (x in 0 until w) {
                val x1 = max(0, x - s)
                val x2 = min(w - 1, x + s)

                val count = (x2 - x1 + 1) * (y2 - y1 + 1)

                val sum = integral[(y2 + 1) * (w + 1) + (x2 + 1)] -
                        integral[y1 * (w + 1) + (x2 + 1)] -
                        integral[(y2 + 1) * (w + 1) + x1] +
                        integral[y1 * (w + 1) + x1]

                val lum = gray[row + x]
                val isForeground = (lum * count) < (sum * (1.0f - t))

                outPixels[row + x] = if (isForeground) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }

        val dest = Bitmap.createBitmap(w, h, src.config ?: Bitmap.Config.ARGB_8888)
        dest.setPixels(outPixels, 0, w, 0, 0, w, h)
        return dest
    }

    private fun fallbackGrayscale(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint().apply {
            val cm = ColorMatrix()
            cm.setSaturation(0f)
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }
}
