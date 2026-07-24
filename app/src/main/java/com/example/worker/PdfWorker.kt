package com.example.worker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.db.PdfEntity
import com.example.data.repository.PdfRepository
import com.example.util.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File

class PdfWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_TOOL_ID = "tool_id"
        const val KEY_PATHS = "paths"
        const val KEY_EXTRA_PARAM = "extra_param"
        const val KEY_BATTERY_SAVER = "battery_saver"

        const val OUTPUT_TITLE = "output_title"
        const val OUTPUT_PATH = "output_path"
        const val OUTPUT_SIZE = "output_size"
        const val OUTPUT_PAGES = "output_pages"
        const val OUTPUT_ERROR = "output_error"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val toolId = inputData.getString(KEY_TOOL_ID) ?: return@withContext Result.failure()
        val rawPaths = inputData.getString(KEY_PATHS) ?: ""
        val extraParam = inputData.getString(KEY_EXTRA_PARAM) ?: ""
        val isBatterySaver = inputData.getBoolean(KEY_BATTERY_SAVER, false)

        val pathsList = rawPaths.split("|||").filter { it.isNotBlank() }
        val repository = PdfRepository.getInstance(appContext)

        try {
            setProgress(workDataOf("progress" to 0.10f, "status" to "Starting background job..."))
            
            // If battery saver is active, yield CPU to prevent thermal spikes and battery drain
            if (isBatterySaver) {
                delay(150)
                yield()
            }

            setProgress(workDataOf("progress" to 0.30f, "status" to "Processing document..."))

            val displayTitle: String
            val resultPath: String
            val sizeBytes: Long
            val pageCount: Int

            when (toolId) {
                "merge" -> {
                    val info = PdfEngine.mergePdfs(appContext, pathsList)
                    resultPath = info.file.absolutePath
                    displayTitle = "Merged_BG_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    sizeBytes = info.sizeBytes
                    pageCount = info.pageCount
                }
                "compress" -> {
                    val firstPath = pathsList.firstOrNull() ?: ""
                    val info = PdfEngine.compressPdf(appContext, firstPath, extraParam)
                    resultPath = info.file.absolutePath
                    displayTitle = "Compressed_BG_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    sizeBytes = info.sizeBytes
                    pageCount = info.pageCount
                }
                "rotate" -> {
                    val firstPath = pathsList.firstOrNull() ?: ""
                    val info = PdfEngine.rotatePdf(appContext, firstPath, extraParam)
                    resultPath = info.file.absolutePath
                    displayTitle = "Rotated_BG_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    sizeBytes = info.sizeBytes
                    pageCount = info.pageCount
                }
                "password" -> {
                    val firstPath = pathsList.firstOrNull() ?: ""
                    val info = PdfEngine.protectPdf(appContext, firstPath, extraParam)
                    resultPath = info.file.absolutePath
                    displayTitle = "Protected_BG_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    sizeBytes = info.sizeBytes
                    pageCount = info.pageCount
                }
                "watermark" -> {
                    val firstPath = pathsList.firstOrNull() ?: ""
                    val info = PdfEngine.createWatermarkedPdf(appContext, firstPath, extraParam)
                    resultPath = info.file.absolutePath
                    displayTitle = "Watermarked_BG_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    sizeBytes = info.sizeBytes
                    pageCount = info.pageCount
                }
                "image_to_pdf" -> {
                    val imageBitmaps = mutableListOf<Bitmap>()
                    pathsList.forEach { path ->
                        try {
                            val file = File(path)
                            if (file.exists()) {
                                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                                if (bmp != null) imageBitmaps.add(bmp)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    val info = PdfEngine.convertImagesToPdf(appContext, imageBitmaps)
                    resultPath = info.file.absolutePath
                    displayTitle = "ImageScan_BG_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    sizeBytes = info.sizeBytes
                    pageCount = info.pageCount
                }
                "ocr" -> {
                    val text = if (extraParam.isNotBlank()) extraParam else PdfEngine.performLocalOcr(pathsList.firstOrNull() ?: "")
                    val info = PdfEngine.createPdfFromText(appContext, "OCR Extracted Text", text)
                    resultPath = info.file.absolutePath
                    displayTitle = "OCR_BG_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    sizeBytes = info.sizeBytes
                    pageCount = info.pageCount
                }
                else -> {
                    val info = PdfEngine.mergePdfs(appContext, pathsList)
                    resultPath = info.file.absolutePath
                    displayTitle = "Processed_BG_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    sizeBytes = info.sizeBytes
                    pageCount = info.pageCount
                }
            }

            if (isBatterySaver) {
                delay(100)
                yield()
            }

            setProgress(workDataOf("progress" to 0.90f, "status" to "Finalizing document..."))

            val sizeKB = (sizeBytes / 1024).coerceAtLeast(45)
            val sizeFormatted = if (sizeKB > 1024) "${String.format("%.1f", sizeKB / 1024.0)} MB" else "$sizeKB KB"

            val newEntity = PdfEntity(
                title = displayTitle,
                path = resultPath,
                sizeFormatted = sizeFormatted,
                sizeBytes = sizeBytes,
                pageCount = pageCount,
                dateModifiedFormatted = "Just now",
                timestamp = System.currentTimeMillis(),
                category = toolId.uppercase(),
                extractedText = if (toolId == "ocr") extraParam else null
            )

            repository.insertPdf(newEntity)

            setProgress(workDataOf("progress" to 1.0f, "status" to "Completed successfully"))

            Result.success(
                workDataOf(
                    OUTPUT_TITLE to displayTitle,
                    OUTPUT_PATH to resultPath,
                    OUTPUT_SIZE to sizeFormatted,
                    OUTPUT_PAGES to pageCount
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(workDataOf(OUTPUT_ERROR to (e.localizedMessage ?: "Worker processing error")))
        }
    }
}
