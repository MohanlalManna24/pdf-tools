package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface

enum class ScanFilterType {
    ORIGINAL,
    MAGIC_COLOR,
    GRAYSCALE,
    BLACK_WHITE
}

object ScanImageProcessor {

    /**
     * Decode image file with proper EXIF orientation and downsampling
     */
    fun decodeOrientedBitmap(file: java.io.File): Bitmap? {
        if (!file.exists()) return null
        return try {
            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            var sampleSize = 1
            val maxDimension = 2048
            while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }
            val decodeOptions = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

            val exif = android.media.ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )
            val rotationDegrees = when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            if (rotationDegrees != 0) {
                rotateBitmap(bitmap, rotationDegrees.toFloat())
            } else {
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Applies brightness adjustment to a bitmap.
     * @param brightness Range -100f to +100f
     */
    fun applyBrightness(source: Bitmap, brightness: Float): Bitmap {
        if (brightness == 0f) return source
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val cm = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, brightness,
            0f, 1f, 0f, 0f, brightness,
            0f, 0f, 1f, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    /**
     * Applies document filters (Magic Color, Grayscale, B&W, Original)
     */
    fun applyFilter(source: Bitmap, filterType: ScanFilterType): Bitmap {
        if (filterType == ScanFilterType.ORIGINAL) return source

        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val cm = ColorMatrix()
        when (filterType) {
            ScanFilterType.ORIGINAL -> return source

            ScanFilterType.MAGIC_COLOR -> {
                // Boost contrast and slight saturation for crisp document scans
                val contrast = 1.25f
                val translate = (-0.5f * contrast + 0.5f) * 255f
                cm.set(floatArrayOf(
                    contrast, 0f, 0f, 0f, translate + 10f,
                    0f, contrast, 0f, 0f, translate + 10f,
                    0f, 0f, contrast, 0f, translate + 10f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }

            ScanFilterType.GRAYSCALE -> {
                cm.setSaturation(0f)
            }

            ScanFilterType.BLACK_WHITE -> {
                // High contrast document threshold filter
                val grayscale = ColorMatrix()
                grayscale.setSaturation(0f)
                val scale = 2.0f
                val translate = -128f * scale + 128f
                val bwMatrix = ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                bwMatrix.postConcat(grayscale)
                cm.set(bwMatrix)
            }
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    /**
     * Crops bitmap according to percentage bounds (0f to 1f)
     */
    fun cropBitmap(
        source: Bitmap,
        leftPercent: Float,
        topPercent: Float,
        rightPercent: Float,
        bottomPercent: Float
    ): Bitmap {
        val left = (source.width * leftPercent.coerceIn(0f, 0.45f)).toInt()
        val top = (source.height * topPercent.coerceIn(0f, 0.45f)).toInt()
        val right = (source.width * (1f - rightPercent.coerceIn(0f, 0.45f))).toInt()
        val bottom = (source.height * (1f - bottomPercent.coerceIn(0f, 0.45f))).toInt()

        val newWidth = (right - left).coerceAtLeast(100)
        val newHeight = (bottom - top).coerceAtLeast(100)

        return try {
            Bitmap.createBitmap(source, left, top, newWidth, newHeight)
        } catch (e: Exception) {
            e.printStackTrace()
            source
        }
    }

    /**
     * Applies a high quality markup stamp (APPROVED, CONFIDENTIAL, SIGNED, NOTED) onto the bitmap
     */
    fun applyMarkupStamp(source: Bitmap, stampText: String, colorHex: String = "#DC2626"): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val stampPaint = Paint().apply {
            color = Color.parseColor(colorHex)
            textSize = (source.width * 0.08f).coerceIn(36f, 120f)
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            alpha = 220
        }

        val boxPaint = Paint().apply {
            color = Color.parseColor(colorHex)
            style = Paint.Style.STROKE
            strokeWidth = (source.width * 0.012f).coerceIn(6f, 18f)
            isAntiAlias = true
            alpha = 220
        }

        val textWidth = stampPaint.measureText(stampText)
        val textHeight = stampPaint.textSize

        canvas.save()
        // Rotate stamp slightly for realistic impression
        canvas.rotate(-15f, source.width * 0.7f, source.height * 0.3f)

        val paddingHorizontal = 30f
        val paddingVertical = 20f
        val rectLeft = source.width * 0.7f - textWidth / 2f - paddingHorizontal
        val rectTop = source.height * 0.3f - textHeight + paddingVertical / 2f
        val rectRight = source.width * 0.7f + textWidth / 2f + paddingHorizontal
        val rectBottom = source.height * 0.3f + paddingVertical * 1.5f

        canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, boxPaint)
        canvas.drawText(
            stampText,
            source.width * 0.7f - textWidth / 2f,
            source.height * 0.3f + textHeight / 3f,
            stampPaint
        )
        canvas.restore()

        return result
    }

    /**
     * Rotates bitmap by given degrees (90, 180, 270)
     */
    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360 == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Generates a realistic document page bitmap for preview / fallback scanning mode.
     */
    fun createSampleScanBitmap(pageNumber: Int): Bitmap {
        val width = 1200
        val height = 1600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clean white paper background
        canvas.drawColor(Color.parseColor("#FBFBFD"))

        val paintHeader = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 54f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val paintSub = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 32f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val paintLine = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 3f
            isAntiAlias = true
        }

        val paintRedAccent = Paint().apply {
            color = Color.parseColor("#DC2626")
            strokeWidth = 8f
            isAntiAlias = true
        }

        // Top accent line
        canvas.drawLine(100f, 120f, 1100f, 120f, paintRedAccent)

        // Header Title
        canvas.drawText("MASTER SERVICES AGREEMENT", 100f, 200f, paintHeader)
        canvas.drawText("Document Scan - Page $pageNumber", 100f, 250f, paintSub)

        // Mock document text paragraphs
        val paintBody = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 26f
            isAntiAlias = true
        }

        val sampleLines = listOf(
            "1. Scope of Services & Contract Terms",
            "This Agreement sets forth the terms and conditions under which the Provider",
            "shall deliver professional digital transformation and document management services.",
            "",
            "2. Confidentiality & Non-Disclosure",
            "Each party agrees that all confidential information disclosed shall remain the sole",
            "and exclusive property of the disclosing party, protected under strict security controls.",
            "",
            "3. Service Level Guarantees",
            "Services shall be provided with at least 99.9% uptime compliance and sub-second response",
            "times across all verified regional endpoints and cloud infrastructures.",
            "",
            "4. Authorization & Signatures",
            "In witness whereof, the parties have executed this Master Services Agreement as of the date",
            "written below by their authorized representatives.",
            "",
            "Signature:   Sarah J. Thompson",
            "Name:        Sarah J. Thompson, CEO",
            "Date:        July 27, 2026"
        )

        var currentY = 340f
        for (line in sampleLines) {
            if (line.isEmpty()) {
                currentY += 24f
            } else if (line.startsWith("1.") || line.startsWith("2.") || line.startsWith("3.") || line.startsWith("4.")) {
                val paintBold = Paint(paintBody).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
                canvas.drawText(line, 100f, currentY, paintBold)
                currentY += 42f
            } else if (line.startsWith("Signature:")) {
                val paintSig = Paint(paintBody).apply {
                    typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                    textSize = 36f
                    color = Color.parseColor("#0F172A")
                }
                canvas.drawText(line, 100f, currentY, paintSig)
                currentY += 48f
            } else {
                canvas.drawText(line, 100f, currentY, paintBody)
                currentY += 38f
            }
        }

        // Horizontal dividing lines near bottom
        canvas.drawLine(100f, 1420f, 1100f, 1420f, paintLine)

        val paintFooter = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 22f
            isAntiAlias = true
        }
        canvas.drawText("CONFIDENTIAL & PROPRIETARY  |  PDF TOOLS SCANNER ENGINE", 100f, 1470f, paintFooter)

        return bitmap
    }
}
