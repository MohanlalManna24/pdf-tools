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

import com.example.cv.FilterType
import com.example.cv.ImageEnhancer
import com.example.util.BatteryOptimizationManager
import com.example.util.BatteryInfo
import com.example.worker.PdfWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.UUID

data class UserProfile(
    val name: String = "Guest Account",
    val email: String = "guest@pdftools.local",
    val accountType: String = "Guest User",
    val phone: String = ""
)

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

    // Battery & WorkManager State
    private val _isBatterySaverEnabled = MutableStateFlow(BatteryOptimizationManager.isBatterySaverEnabled(application))
    val isBatterySaverEnabled: StateFlow<Boolean> = _isBatterySaverEnabled.asStateFlow()

    private val _pauseOnLowBattery = MutableStateFlow(BatteryOptimizationManager.isPauseLowBatteryEnabled(application))
    val pauseOnLowBattery: StateFlow<Boolean> = _pauseOnLowBattery.asStateFlow()

    private val _requireCharging = MutableStateFlow(BatteryOptimizationManager.isRequireChargingEnabled(application))
    val requireCharging: StateFlow<Boolean> = _requireCharging.asStateFlow()

    private val _batteryInfo = MutableStateFlow(BatteryOptimizationManager.getBatteryInfo(application))
    val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo.asStateFlow()

    private val _activeWorkId = MutableStateFlow<UUID?>(null)
    val activeWorkId: StateFlow<UUID?> = _activeWorkId.asStateFlow()

    fun refreshBatteryInfo() {
        _batteryInfo.value = BatteryOptimizationManager.getBatteryInfo(getApplication())
    }

    fun setBatterySaverEnabled(enabled: Boolean) {
        val app = getApplication<Application>()
        BatteryOptimizationManager.setBatterySaverEnabled(app, enabled)
        _isBatterySaverEnabled.value = enabled
    }

    fun setPauseOnLowBattery(enabled: Boolean) {
        val app = getApplication<Application>()
        BatteryOptimizationManager.setPauseLowBatteryEnabled(app, enabled)
        _pauseOnLowBattery.value = enabled
    }

    fun setRequireCharging(enabled: Boolean) {
        val app = getApplication<Application>()
        BatteryOptimizationManager.setRequireChargingEnabled(app, enabled)
        _requireCharging.value = enabled
    }

    // User Profile State
    private val profilePrefs = application.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(
        UserProfile(
            name = profilePrefs.getString("user_name", "Guest Account") ?: "Guest Account",
            email = profilePrefs.getString("user_email", "guest@pdftools.local") ?: "guest@pdftools.local",
            accountType = profilePrefs.getString("account_type", "Guest User") ?: "Guest User",
            phone = profilePrefs.getString("user_phone", "") ?: ""
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    fun updateUserProfile(name: String, email: String, phone: String, accountType: String = _userProfile.value.accountType) {
        val newProfile = UserProfile(
            name = name.ifBlank { "Guest Account" },
            email = email.ifBlank { "guest@pdftools.local" },
            accountType = accountType.ifBlank { "Guest User" },
            phone = phone.trim()
        )
        profilePrefs.edit()
            .putString("user_name", newProfile.name)
            .putString("user_email", newProfile.email)
            .putString("account_type", newProfile.accountType)
            .putString("user_phone", newProfile.phone)
            .apply()
        _userProfile.value = newProfile
    }

    fun resetToGuestAccount() {
        profilePrefs.edit().clear().apply()
        _userProfile.value = UserProfile()
    }

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

    fun toggleFavorite(pdf: PdfEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(pdf.id, pdf.isFavorite)
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

    fun importUriToApp(uri: Uri, onComplete: ((PdfEntity) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val tempFile = PdfEngine.getFileFromUri(context, uri)
            if (tempFile != null && tempFile.exists()) {
                val pageCount = PdfEngine.getPdfPageCount(tempFile)
                val sizeBytes = tempFile.length()
                val sizeKB = (sizeBytes / 1024).coerceAtLeast(1)
                val sizeFormatted = if (sizeKB > 1024) "${String.format("%.1f", sizeKB / 1024.0)} MB" else "$sizeKB KB"

                // Duplicate prevention check: search by exact path OR title + size
                val existing = repository.getPdfByTitleAndSize(tempFile.name, sizeBytes)
                    ?: repository.getPdfByPath(tempFile.absolutePath)

                val targetEntity: PdfEntity
                if (existing != null) {
                    // Update timestamp to float to top of recents without creating duplicate entry
                    targetEntity = existing.copy(
                        timestamp = System.currentTimeMillis(),
                        dateModifiedFormatted = "Just now"
                    )
                    repository.updatePdf(targetEntity)
                } else {
                    val newEntity = PdfEntity(
                        title = tempFile.name,
                        path = tempFile.absolutePath,
                        sizeFormatted = sizeFormatted,
                        sizeBytes = sizeBytes,
                        pageCount = pageCount,
                        dateModifiedFormatted = "Just now",
                        timestamp = System.currentTimeMillis(),
                        category = "IMPORTED"
                    )
                    val newId = repository.insertPdf(newEntity)
                    targetEntity = newEntity.copy(id = newId.toInt())
                }

                _activePdf.value = targetEntity
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(targetEntity)
                }
            }
        }
    }

    // Perform Tool Processing locally or via WorkManager based on Battery Saver settings
    fun executeTool(toolId: String, titlesOrPaths: List<String>, extraParam: String = "", forceWorkManager: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val currentBattery = BatteryOptimizationManager.getBatteryInfo(context)
            _batteryInfo.value = currentBattery

            val shouldDefer = forceWorkManager || BatteryOptimizationManager.shouldDeferToWorkManager(context)

            if (shouldDefer) {
                val toolName = "Queued in WorkManager (Battery Saver Active)"
                _processingState.value = ProcessingUiState.Processing(toolName, 0.10f)

                val workId = BatteryOptimizationManager.enqueueWorkManagerTask(context, toolId, titlesOrPaths, extraParam)
                _activeWorkId.value = workId

                WorkManager.getInstance(context).getWorkInfoByIdFlow(workId).collect { workInfo ->
                    if (workInfo != null) {
                        when (workInfo.state) {
                            WorkInfo.State.ENQUEUED -> {
                                _processingState.value = ProcessingUiState.Processing("Task Enqueued in WorkManager (Battery Saver)", 0.20f)
                            }
                            WorkInfo.State.RUNNING -> {
                                val progress = workInfo.progress.getFloat("progress", 0.50f)
                                val status = workInfo.progress.getString("status") ?: "Processing safely in background..."
                                _processingState.value = ProcessingUiState.Processing(status, progress)
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                val outTitle = workInfo.outputData.getString(PdfWorker.OUTPUT_TITLE) ?: "Processed_PDF.pdf"
                                val outPath = workInfo.outputData.getString(PdfWorker.OUTPUT_PATH) ?: ""
                                val outSize = workInfo.outputData.getString(PdfWorker.OUTPUT_SIZE) ?: "100 KB"
                                val outPages = workInfo.outputData.getInt(PdfWorker.OUTPUT_PAGES, 1)

                                val createdEntity = repository.getPdfByPath(outPath)

                                _processingState.value = ProcessingUiState.Success(
                                    title = outTitle,
                                    path = outPath,
                                    sizeFormatted = outSize,
                                    pageCount = outPages,
                                    createdEntity = createdEntity
                                )
                            }
                            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                                val err = workInfo.outputData.getString(PdfWorker.OUTPUT_ERROR) ?: "Background WorkManager task failed"
                                _processingState.value = ProcessingUiState.Error(err)
                            }
                            else -> {}
                        }
                    }
                }
                return@launch
            }

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

            val batterySaverOn = _isBatterySaverEnabled.value

            _processingState.value = ProcessingUiState.Processing(
                if (batterySaverOn) "$toolName (Eco CPU Mode)" else toolName,
                0.15f
            )

            // Step progression for UI feedback with battery-aware yield
            for (i in 2..8) {
                delay(if (batterySaverOn) 180 else 120)
                if (batterySaverOn) {
                    kotlinx.coroutines.yield()
                }
                _processingState.value = ProcessingUiState.Processing(
                    if (batterySaverOn) "$toolName (Eco CPU Mode)" else toolName,
                    i * 0.11f
                )
            }

            try {
                val context = getApplication<Application>()

                if (toolId == "split") {
                    val firstPath = titlesOrPaths.firstOrNull() ?: ""
                    val pdfResults = PdfEngine.splitPdfMultiple(context, firstPath, extraParam)

                    val savedEntities = mutableListOf<PdfEntity>()
                    pdfResults.forEach { resultInfo ->
                        val sizeKB = (resultInfo.sizeBytes / 1024).coerceAtLeast(45)
                        val sizeFormatted = if (sizeKB > 1024) "${String.format("%.1f", sizeKB / 1024.0)} MB" else "$sizeKB KB"

                        val newEntity = PdfEntity(
                            title = resultInfo.file.name,
                            path = resultInfo.file.absolutePath,
                            sizeFormatted = sizeFormatted,
                            sizeBytes = resultInfo.sizeBytes,
                            pageCount = resultInfo.pageCount,
                            dateModifiedFormatted = "Just now",
                            timestamp = System.currentTimeMillis(),
                            category = "SPLIT"
                        )
                        val id = repository.insertPdf(newEntity)
                        savedEntities.add(newEntity.copy(id = id.toInt()))
                    }

                    val primaryEntity = savedEntities.firstOrNull() ?: savedEntities.last()
                    _activePdf.value = primaryEntity

                    val summaryTitle = if (savedEntities.size > 1) {
                        "Split into ${savedEntities.size} Separate PDFs"
                    } else {
                        primaryEntity.title
                    }

                    _processingState.value = ProcessingUiState.Success(
                        title = summaryTitle,
                        path = primaryEntity.path,
                        sizeFormatted = "${savedEntities.sumOf { (it.sizeBytes / 1024).toInt() }} KB Total",
                        pageCount = savedEntities.sumOf { it.pageCount },
                        createdEntity = primaryEntity
                    )
                    return@launch
                }

                val resultInfo: com.example.util.LocalPdfInfo
                val displayTitle: String

                when (toolId) {
                    "merge" -> {
                        resultInfo = PdfEngine.mergePdfs(context, titlesOrPaths)
                        displayTitle = "Merged_Document_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
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
                        val pdfResults = PdfEngine.splitPdfMultiple(context, firstPath, extraParam, forceSeparate = false)
                        resultInfo = pdfResults.first()
                        displayTitle = "Trimmed_Doc_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    }
                    "rearrange" -> {
                        val firstPath = titlesOrPaths.firstOrNull() ?: ""
                        val pdfResults = PdfEngine.splitPdfMultiple(context, firstPath, extraParam, forceSeparate = false)
                        resultInfo = pdfResults.first()
                        displayTitle = "Reordered_Doc_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    }
                    "password" -> {
                        val firstPath = titlesOrPaths.firstOrNull() ?: ""
                        resultInfo = PdfEngine.protectPdf(context, firstPath, extraParam)
                        displayTitle = "Protected_Doc_${System.currentTimeMillis().toString().takeLast(4)}.pdf"
                    }
                    "pdf_to_image" -> {
                        val firstPath = titlesOrPaths.firstOrNull() ?: ""
                        resultInfo = PdfEngine.exportPdfToImages(context, firstPath)
                        displayTitle = resultInfo.file.name
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

    fun updateSuccessStateTitle(newTitle: String, newPath: String) {
        val currentState = _processingState.value
        if (currentState is ProcessingUiState.Success) {
            val updatedEntity = currentState.createdEntity?.copy(title = newTitle, path = newPath)
            if (updatedEntity != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.insertPdf(updatedEntity)
                }
                _activePdf.value = updatedEntity
            }
            _processingState.value = currentState.copy(
                title = newTitle,
                path = newPath,
                createdEntity = updatedEntity
            )
        }
    }

    fun saveScannedPdf(
        bitmaps: List<Bitmap>,
        customTitle: String? = null,
        onSuccess: (PdfEntity) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _processingState.value = ProcessingUiState.Processing("Generating Scanned PDF...", 0.2f)
            try {
                val context = getApplication<Application>()
                _processingState.value = ProcessingUiState.Processing("Compiling document pages...", 0.6f)
                val resultInfo = PdfEngine.convertImagesToPdf(context, bitmaps)
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
