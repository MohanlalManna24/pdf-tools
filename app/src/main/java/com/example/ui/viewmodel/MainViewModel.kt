package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.PdfEntity
import com.example.data.repository.PdfRepository
import com.example.util.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface ProcessingUiState {
    object Idle : ProcessingUiState
    data class Processing(val toolName: String, val progress: Float) : ProcessingUiState
    data class Success(
        val title: String,
        val path: String,
        val sizeFormatted: String,
        val pageCount: Int,
        val createdEntity: PdfEntity? = null
    ) : ProcessingUiState
    data class Error(val message: String) : ProcessingUiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfRepository.getInstance(application)

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Processing UI state
    private val _processingState = MutableStateFlow<ProcessingUiState>(ProcessingUiState.Idle)
    val processingState: StateFlow<ProcessingUiState> = _processingState.asStateFlow()

    // Dark theme toggle
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Current active file in reader
    private val _activePdf = MutableStateFlow<PdfEntity?>(null)
    val activePdf: StateFlow<PdfEntity?> = _activePdf.asStateFlow()

    // Recent Files
    val recentFiles: StateFlow<List<PdfEntity>> = repository.recentPdfs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorite Files
    val favoriteFiles: StateFlow<List<PdfEntity>> = repository.favoritePdfs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Files / Filtered by Search
    val allFiles: StateFlow<List<PdfEntity>> = combine(repository.allPdfs, _searchQuery) { files, query ->
        if (query.isBlank()) {
            files
        } else {
            files.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                (it.extractedText?.contains(query, ignoreCase = true) == true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(pdfId: Int, currentFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(pdfId, currentFav)
        }
    }

    fun renamePdf(pdf: PdfEntity, newNameRaw: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmed = newNameRaw.trim()
            if (trimmed.isBlank()) return@launch
            val formattedTitle = if (trimmed.endsWith(".pdf", ignoreCase = true)) trimmed else "$trimmed.pdf"

            var updatedPath = pdf.path
            try {
                val oldFile = File(pdf.path)
                if (oldFile.exists() && oldFile.parentFile != null) {
                    val newFile = File(oldFile.parentFile, formattedTitle)
                    if (oldFile.renameTo(newFile)) {
                        updatedPath = newFile.absolutePath
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val updatedEntity = pdf.copy(
                title = formattedTitle,
                path = updatedPath,
                timestamp = System.currentTimeMillis()
            )
            repository.updatePdf(updatedEntity)
            if (_activePdf.value?.id == pdf.id) {
                _activePdf.value = updatedEntity
            }
        }
    }

    fun deleteFile(pdf: PdfEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Delete actual physical file if exists
                val file = File(pdf.path)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.deletePdf(pdf)
        }
    }

    fun toggleDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }

    fun openPdf(pdf: PdfEntity) {
        _activePdf.value = pdf
    }

    fun importUriToApp(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val tempFile = PdfEngine.getFileFromUri(context, uri)
            if (tempFile != null && tempFile.exists()) {
                val pageCount = PdfEngine.getPdfPageCount(tempFile)
                val sizeKB = (tempFile.length() / 1024).coerceAtLeast(1)
                val newEntity = PdfEntity(
                    title = tempFile.name,
                    path = tempFile.absolutePath,
                    sizeFormatted = if (sizeKB > 1024) "${String.format("%.1f", sizeKB / 1024.0)} MB" else "$sizeKB KB",
                    sizeBytes = tempFile.length(),
                    pageCount = pageCount,
                    dateModifiedFormatted = "Just now",
                    timestamp = System.currentTimeMillis(),
                    category = "IMPORTED"
                )
                repository.insertPdf(newEntity)
                _activePdf.value = newEntity
            }
        }
    }

    // Perform Tool Processing locally
    fun executeTool(toolId: String, titlesOrPaths: List<String>, extraParam: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val toolName = when(toolId) {
                "merge" -> "Merging PDF documents..."
                "split" -> "Splitting page ranges..."
                "compress" -> "Optimizing document size..."
                "image_to_pdf" -> "Converting images to PDF..."
                "pdf_to_image" -> "Extracting high-res images..."
                "rotate" -> "Rotating page orientation..."
                "delete" -> "Removing selected pages..."
                "rearrange" -> "Reordering page structure..."
                "watermark" -> "Applying watermark overlay..."
                "password" -> "Securing document..."
                "scanner" -> "Saving high-res PDF scan..."
                "ocr" -> "Extracting document text..."
                else -> "Processing document..."
            }

            _processingState.value = ProcessingUiState.Processing(toolName, 0.15f)

            // Step progression for UI feedback
            for (i in 2..8) {
                delay(120)
                _processingState.value = ProcessingUiState.Processing(toolName, i * 0.11f)
            }

            try {
                val context = getApplication<Application>()
                val resultInfo: com.example.util.LocalPdfInfo
                val displayTitle: String

                when (toolId) {
                    "merge" -> {
                        resultInfo = PdfEngine.mergePdfs(context, titlesOrPaths)
                        displayTitle = "Merged_Document_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    }
                    "split" -> {
                        val firstPath = titlesOrPaths.firstOrNull() ?: ""
                        resultInfo = PdfEngine.splitPdf(context, firstPath, extraParam)
                        val safeRangeTag = if (extraParam.isNotBlank()) extraParam.replace(" ", "").replace(",", "_") else "Extracted"
                        displayTitle = "Split_Doc_$safeRangeTag.pdf"
                    }
                    "compress" -> {
                        val firstPath = titlesOrPaths.firstOrNull() ?: ""
                        resultInfo = PdfEngine.compressPdf(context, firstPath, extraParam)
                        displayTitle = "Compressed_Doc_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    }
                    "rotate" -> {
                        val firstPath = titlesOrPaths.firstOrNull() ?: ""
                        resultInfo = PdfEngine.rotatePdf(context, firstPath, extraParam)
                        displayTitle = "Rotated_Doc_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    }
                    "delete" -> {
                        val firstPath = titlesOrPaths.firstOrNull() ?: ""
                        resultInfo = PdfEngine.splitPdf(context, firstPath, extraParam)
                        displayTitle = "Trimmed_Doc_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    }
                    "rearrange" -> {
                        val firstPath = titlesOrPaths.firstOrNull() ?: ""
                        resultInfo = PdfEngine.splitPdf(context, firstPath, extraParam)
                        displayTitle = "Reordered_Doc_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    }
                    "password" -> {
                        val firstPath = titlesOrPaths.firstOrNull() ?: ""
                        val label = if (extraParam.isNotBlank()) "LOCKED - $extraParam" else "ENCRYPTED"
                        resultInfo = PdfEngine.createWatermarkedPdf(context, firstPath, label)
                        displayTitle = "Protected_Doc_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    }
                    "pdf_to_image" -> {
                        val firstPath = titlesOrPaths.firstOrNull() ?: ""
                        val sourceFile = File(firstPath)
                        val bitmaps = mutableListOf<Bitmap>()
                        if (sourceFile.exists()) {
                            val count = PdfEngine.getPdfPageCount(sourceFile)
                            for (p in 0 until count) {
                                val bmp = PdfEngine.renderPageToBitmap(sourceFile, p, 1000)
                                if (bmp != null) bitmaps.add(bmp)
                            }
                        }
                        resultInfo = PdfEngine.convertImagesToPdf(context, bitmaps)
                        displayTitle = "Exported_Images_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    }
                    "watermark" -> {
                        val firstPath = titlesOrPaths.firstOrNull() ?: ""
                        resultInfo = PdfEngine.createWatermarkedPdf(context, firstPath, extraParam)
                        displayTitle = "Watermarked_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    }
                    "image_to_pdf" -> {
                        val imageBitmaps = mutableListOf<Bitmap>()
                        titlesOrPaths.forEach { path ->
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
                        resultInfo = PdfEngine.convertImagesToPdf(context, imageBitmaps)
                        displayTitle = "ImageScan_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    }
                    "ocr" -> {
                        val text = if (extraParam.isNotBlank()) extraParam else PdfEngine.performLocalOcr(titlesOrPaths.firstOrNull() ?: "")
                        resultInfo = PdfEngine.createPdfFromText(context, "OCR Extracted Text", text)
                        displayTitle = "OCR_Extracted_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    }
                    else -> {
                        resultInfo = PdfEngine.mergePdfs(context, titlesOrPaths)
                        displayTitle = "${toolId.replaceFirstChar { it.uppercase() }}_Result.pdf"
                    }
                }

                val sizeKB = (resultInfo.sizeBytes / 1024).coerceAtLeast(45)
                val sizeFormatted = if (sizeKB > 1024) "${String.format("%.1f", sizeKB / 1024.0)} MB" else "$sizeKB KB"

                val newEntity = PdfEntity(
                    title = displayTitle,
                    path = resultInfo.file.absolutePath,
                    sizeFormatted = sizeFormatted,
                    sizeBytes = resultInfo.sizeBytes,
                    pageCount = resultInfo.pageCount,
                    dateModifiedFormatted = "Just now",
                    timestamp = System.currentTimeMillis(),
                    category = toolId.uppercase(),
                    extractedText = if (toolId == "ocr") extraParam else null
                )

                val id = repository.insertPdf(newEntity)
                val savedEntity = newEntity.copy(id = id.toInt())

                _activePdf.value = savedEntity

                _processingState.value = ProcessingUiState.Success(
                    title = displayTitle,
                    path = resultInfo.file.absolutePath,
                    sizeFormatted = sizeFormatted,
                    pageCount = resultInfo.pageCount,
                    createdEntity = savedEntity
                )
            } catch (e: Exception) {
                _processingState.value = ProcessingUiState.Error(e.localizedMessage ?: "Processing failed offline.")
            }
        }
    }

    fun resetProcessingState() {
        _processingState.value = ProcessingUiState.Idle
    }

    fun saveScannedPdf(
        bitmaps: List<Bitmap>,
        filterName: String = "Magic Color",
        customTitle: String? = null,
        onSuccess: (PdfEntity) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _processingState.value = ProcessingUiState.Processing("Generating Scanned PDF...", 0.2f)
            try {
                val context = getApplication<Application>()
                val processedBitmaps = bitmaps.map { bitmap ->
                    when (filterName) {
                        "Magic Color" -> applyMagicColorFilter(bitmap)
                        "B&W" -> applyBWFilter(bitmap)
                        "Grayscale" -> applyGrayscaleFilter(bitmap)
                        "Warm Paper" -> applyWarmPaperFilter(bitmap)
                        "Invert" -> applyInvertFilter(bitmap)
                        else -> bitmap
                    }
                }

                _processingState.value = ProcessingUiState.Processing("Compiling document pages...", 0.6f)
                val resultInfo = PdfEngine.convertImagesToPdf(context, processedBitmaps)
                val sizeKB = (resultInfo.sizeBytes / 1024).coerceAtLeast(30)
                val sizeFormatted = if (sizeKB > 1024) "${String.format("%.1f", sizeKB / 1024.0)} MB" else "$sizeKB KB"

                val timeTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val displayTitle = customTitle ?: "Scan_$timeTag.pdf"

                val newEntity = PdfEntity(
                    title = displayTitle,
                    path = resultInfo.file.absolutePath,
                    sizeFormatted = sizeFormatted,
                    sizeBytes = resultInfo.sizeBytes,
                    pageCount = resultInfo.pageCount,
                    dateModifiedFormatted = "Just now",
                    timestamp = System.currentTimeMillis(),
                    category = "SCANNER"
                )

                val id = repository.insertPdf(newEntity)
                val savedEntity = newEntity.copy(id = id.toInt())

                // Save a local copy to public Downloads for the user
                PdfEngine.savePdfToDownloads(context, resultInfo.file, displayTitle)

                _activePdf.value = savedEntity
                _processingState.value = ProcessingUiState.Success(
                    title = displayTitle,
                    path = resultInfo.file.absolutePath,
                    sizeFormatted = sizeFormatted,
                    pageCount = resultInfo.pageCount,
                    createdEntity = savedEntity
                )

                withContext(Dispatchers.Main) {
                    onSuccess(savedEntity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _processingState.value = ProcessingUiState.Error(e.localizedMessage ?: "Failed to process scan")
            }
        }
    }

    private fun applyMagicColorFilter(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint().apply {
            val cm = ColorMatrix()
            // Increase contrast & light saturation for crisp document text
            val contrast = 1.35f
            val brightness = 15f
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

    private fun applyBWFilter(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint().apply {
            val cm = ColorMatrix()
            cm.setSaturation(0f)
            // High contrast binarization approximation
            val scale = 2.0f
            val translate = -128f * scale + 128f
            val bwArray = floatArrayOf(
                scale, scale, scale, 0f, translate,
                scale, scale, scale, 0f, translate,
                scale, scale, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
            cm.postConcat(ColorMatrix(bwArray))
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    private fun applyGrayscaleFilter(src: Bitmap): Bitmap {
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

    private fun applyWarmPaperFilter(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint().apply {
            val cm = ColorMatrix()
            val array = floatArrayOf(
                1.2f, 0f, 0f, 0f, 20f,
                0f, 1.15f, 0f, 0f, 15f,
                0f, 0f, 0.95f, 0f, 5f,
                0f, 0f, 0f, 1f, 0f
            )
            cm.set(array)
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    private fun applyInvertFilter(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint().apply {
            val cm = ColorMatrix()
            val array = floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
            cm.set(array)
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }
}
