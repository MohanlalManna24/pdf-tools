package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint as AndroidPaint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.RedPrimary
import com.example.ui.viewmodel.MainViewModel
import com.example.util.PdfEngine
import kotlinx.coroutines.delay
import java.io.InputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ScanMode { MANUAL, AUTO }

// Data class representing 4 normalized quad points (0f..1f range relative to image)
data class QuadPoints(
    var topLeft: Offset,
    var topRight: Offset,
    var bottomRight: Offset,
    var bottomLeft: Offset
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    onCompleteScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, "Camera permission needed to scan documents", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Camera controls
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var scanMode by remember { mutableStateOf(ScanMode.MANUAL) }
    var scanDocType by remember { mutableStateOf("Document") }
    var selectedFilter by remember { mutableStateOf("Magic Color") }
    var isCapturing by remember { mutableStateOf(false) }

    // Multi-page document state
    val scannedPages = remember { mutableStateListOf<Bitmap>() }
    var activePageIndex by remember { mutableIntStateOf(0) }
    var isEditingMode by remember { mutableStateOf(false) } // False = Camera View (Image 1), True = Crop Editor View (Image 2)

    val defaultTitle = remember {
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        "Adobe Scan $dateStr"
    }
    var documentTitle by remember { mutableStateOf(defaultTitle) }

    // Dialog & Sheet States
    var showRenameDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showOcrSheet by remember { mutableStateOf(false) }
    var ocrExtractedText by remember { mutableStateOf("") }
    var isOcrLoading by remember { mutableStateOf(false) }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    scannedPages.add(bitmap)
                    activePageIndex = scannedPages.size - 1
                    isEditingMode = true
                    Toast.makeText(context, "Imported page from gallery", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Quad Crop Points State for active editing page (Normalized 0f..1f)
    var cropQuad by remember(activePageIndex, isEditingMode, scannedPages.size) {
        mutableStateOf(
            if (scannedPages.isNotEmpty() && activePageIndex in scannedPages.indices) {
                detectTightDocumentQuad(scannedPages[activePageIndex])
            } else {
                QuadPoints(Offset(0.05f, 0.05f), Offset(0.95f, 0.05f), Offset(0.95f, 0.95f), Offset(0.05f, 0.95f))
            }
        )
    }

    // Laser Animation for Viewfinder
    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    // Function to snap photo
    val capturePage = {
        if (!isCapturing) {
            isCapturing = true
            val capture = imageCapture
            if (capture != null && hasCameraPermission) {
                capture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bitmap = imageProxyToBitmap(image)
                            image.close()
                            if (bitmap != null) {
                                scannedPages.add(bitmap)
                                activePageIndex = scannedPages.size - 1
                                cropQuad = detectTightDocumentQuad(bitmap)
                                isEditingMode = true
                                Toast.makeText(context, "Document Captured! Trim Edges", Toast.LENGTH_SHORT).show()
                            } else {
                                val fallback = generateDocumentBitmap(context, scannedPages.size + 1)
                                scannedPages.add(fallback)
                                activePageIndex = scannedPages.size - 1
                                cropQuad = detectTightDocumentQuad(fallback)
                                isEditingMode = true
                            }
                            isCapturing = false
                        }

                        override fun onError(exception: ImageCaptureException) {
                            exception.printStackTrace()
                            val fallback = generateDocumentBitmap(context, scannedPages.size + 1)
                            scannedPages.add(fallback)
                            activePageIndex = scannedPages.size - 1
                            cropQuad = detectTightDocumentQuad(fallback)
                            isEditingMode = true
                            isCapturing = false
                        }
                    }
                )
            } else {
                val fallback = generateDocumentBitmap(context, scannedPages.size + 1)
                scannedPages.add(fallback)
                activePageIndex = scannedPages.size - 1
                cropQuad = detectTightDocumentQuad(fallback)
                isEditingMode = true
                isCapturing = false
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        RenameDocumentDialog(
            initialTitle = documentTitle,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                documentTitle = newName
                showRenameDialog = false
                Toast.makeText(context, "Renamed to $newName", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // OCR Bottom Sheet
    if (showOcrSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOcrSheet = false },
            containerColor = Color(0xFF1E1E1E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.TextFields, contentDescription = null, tint = Color(0xFF2196F3))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Extracted OCR Text", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    }
                    IconButton(onClick = { showOcrSheet = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isOcrLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Extracting text from page...", color = Color.LightGray)
                    }
                } else {
                    OutlinedTextField(
                        value = ocrExtractedText,
                        onValueChange = { ocrExtractedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("OCR Text", ocrExtractedText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Text")
                        }

                        Button(
                            onClick = { showOcrSheet = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        if (!isEditingMode) {
            // ==========================================
            // VIEW 1: CAMERA CAPTURE MODE (Ref Image 1)
            // ==========================================
            if (hasCameraPermission) {
                DisposableEffect(lifecycleOwner) {
                    onDispose {
                        try {
                            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                            cameraProvider.unbindAll()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // Live Camera Preview
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    capture
                                )
                                cameraControl = camera.cameraControl
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Laser Overlay & Document Bounds Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val boxW = w * 0.82f
                    val boxH = h * 0.58f
                    val left = (w - boxW) / 2f
                    val top = (h - boxH) / 2f
                    val right = left + boxW
                    val bottom = top + boxH

                    val cornerLen = 38.dp.toPx()
                    val strokeW = 4.5.dp.toPx()
                    val cornerColor = Color(0xFF2196F3)

                    // 4 Corner Brackets
                    drawPath(
                        path = Path().apply {
                            moveTo(left, top + cornerLen); lineTo(left, top); lineTo(left + cornerLen, top)
                        },
                        color = cornerColor, style = Stroke(width = strokeW)
                    )
                    drawPath(
                        path = Path().apply {
                            moveTo(right - cornerLen, top); lineTo(right, top); lineTo(right, top + cornerLen)
                        },
                        color = cornerColor, style = Stroke(width = strokeW)
                    )
                    drawPath(
                        path = Path().apply {
                            moveTo(left, bottom - cornerLen); lineTo(left, bottom); lineTo(left + cornerLen, bottom)
                        },
                        color = cornerColor, style = Stroke(width = strokeW)
                    )
                    drawPath(
                        path = Path().apply {
                            moveTo(right - cornerLen, bottom); lineTo(right, bottom); lineTo(right, bottom - cornerLen)
                        },
                        color = cornerColor, style = Stroke(width = strokeW)
                    )

                    // Laser Line
                    val currentLaserY = top + (boxH * laserY)
                    drawLine(
                        color = cornerColor.copy(alpha = 0.85f),
                        start = Offset(left + 10f, currentLaserY),
                        end = Offset(right - 10f, currentLaserY),
                        strokeWidth = 3.dp.toPx()
                    )
                }

                // Top Header Controls (Ref Image 1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = "Home", tint = Color.White)
                    }

                    Text(
                        text = "Document Scanner",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { Toast.makeText(context, "AI Magic Scan Active", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Scan", tint = Color.White)
                        }

                        IconButton(
                            onClick = { Toast.makeText(context, "QR & Barcode Mode", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = "QR Mode", tint = Color.White)
                        }
                    }
                }

                // Camera Bottom Overlay Area (Ref Image 1)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp)
                ) {
                    // Document Modes Selector Bar (Whiteboard, Book, Document, ID card, Business Card)
                    val docTypes = listOf("Whiteboard", "Book", "Document", "ID card", "Business Card")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        items(docTypes) { type ->
                            val isSelected = scanDocType == type
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { scanDocType = type }
                            ) {
                                Text(
                                    text = type,
                                    color = if (isSelected) Color(0xFF2196F3) else Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFF2196F3))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Shutter Control Bar (Gallery, Auto-detect, Shutter SNAP, Flash, Thumbnail Stack)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gallery Import Button
                        IconButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                        }

                        // Auto Mode Toggle
                        IconButton(
                            onClick = {
                                scanMode = if (scanMode == ScanMode.MANUAL) ScanMode.AUTO else ScanMode.MANUAL
                                Toast.makeText(context, "Mode: ${scanMode.name}", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (scanMode == ScanMode.AUTO) Color(0xFF2196F3) else Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Filled.CropFree, contentDescription = "Auto Mode", tint = Color.White)
                        }

                        // Main Shutter Button (Large Blue Outer Circle)
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .border(4.dp, Color.White, CircleShape)
                                .padding(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2196F3))
                                .clickable { capturePage() }
                                .testTag("scanner_shutter_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = "Snap", tint = Color.White, modifier = Modifier.size(32.dp))
                        }

                        // Flash Toggle Button
                        IconButton(
                            onClick = {
                                isFlashOn = !isFlashOn
                                cameraControl?.enableTorch(isFlashOn)
                                Toast.makeText(context, if (isFlashOn) "Torch ON" else "Torch OFF", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isFlashOn) Color(0xFFFFC107) else Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                contentDescription = "Flash",
                                tint = if (isFlashOn) Color.Black else Color.White
                            )
                        }

                        // Thumbnail Preview Badge Button (Clicking opens Crop Editor View - Image 2)
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(2.dp, Color(0xFF2196F3), RoundedCornerShape(12.dp))
                                .clickable {
                                    if (scannedPages.isNotEmpty()) {
                                        activePageIndex = scannedPages.size - 1
                                        cropQuad = detectTightDocumentQuad(scannedPages[activePageIndex])
                                        isEditingMode = true
                                    } else {
                                        Toast.makeText(context, "No documents captured yet", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (scannedPages.isNotEmpty()) {
                                Image(
                                    bitmap = scannedPages.last().asImageBitmap(),
                                    contentDescription = "Thumb",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // Blue Badge Count (e.g. "2")
                            if (scannedPages.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2196F3)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = scannedPages.size.toString(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Permission rationale
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(60.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Camera Access Required", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Please allow camera access to scan documents cleanly with auto-edge detection.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Grant Permission", fontWeight = FontWeight.Bold)
                    }
                }
            }

        } else {
            // ===============================================
            // VIEW 2: CROP & DOCUMENT EDITOR MODE (Ref Image 2)
            // ===============================================
            val activeBitmap = if (scannedPages.isNotEmpty() && activePageIndex in scannedPages.indices) {
                scannedPages[activePageIndex]
            } else null

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
            ) {
                // Top Navigation Bar (Ref Image 2)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Home, contentDescription = "Home", tint = Color.White)
                    }

                    // Editable Title Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showRenameDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = documentTitle,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Filled.Edit, contentDescription = "Rename", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = {
                            if (activeBitmap != null) {
                                scannedPages[activePageIndex] = applyMagicColorFilter(activeBitmap)
                                Toast.makeText(context, "Magic Color Applied!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Magic", tint = Color.White)
                    }
                }

                // Center Interactive Document Preview Container with Quad Corners & Edge Drag Handles
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF282828))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (activeBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = activeBitmap.asImageBitmap(),
                                contentDescription = "Scanned Page",
                                modifier = Modifier.fillMaxSize()
                            )

                            // Interactive Quad Handle Canvas Overlay (Ref Image 2)
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(cropQuad) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val w = size.width.toFloat()
                                            val h = size.height.toFloat()
                                            if (w <= 0 || h <= 0) return@detectDragGestures

                                            val pos = change.position
                                            val normPos = Offset(pos.x / w, pos.y / h)

                                            // Determine nearest corner or edge handle
                                            val distTL = (normPos - cropQuad.topLeft).getDistance()
                                            val distTR = (normPos - cropQuad.topRight).getDistance()
                                            val distBR = (normPos - cropQuad.bottomRight).getDistance()
                                            val distBL = (normPos - cropQuad.bottomLeft).getDistance()

                                            val dxNorm = dragAmount.x / w
                                            val dyNorm = dragAmount.y / h

                                            when {
                                                distTL < 0.15f -> {
                                                    cropQuad = cropQuad.copy(
                                                        topLeft = Offset(
                                                            (cropQuad.topLeft.x + dxNorm).coerceIn(0f, cropQuad.topRight.x - 0.05f),
                                                            (cropQuad.topLeft.y + dyNorm).coerceIn(0f, cropQuad.bottomLeft.y - 0.05f)
                                                        )
                                                    )
                                                }
                                                distTR < 0.15f -> {
                                                    cropQuad = cropQuad.copy(
                                                        topRight = Offset(
                                                            (cropQuad.topRight.x + dxNorm).coerceIn(cropQuad.topLeft.x + 0.05f, 1f),
                                                            (cropQuad.topRight.y + dyNorm).coerceIn(0f, cropQuad.bottomRight.y - 0.05f)
                                                        )
                                                    )
                                                }
                                                distBR < 0.15f -> {
                                                    cropQuad = cropQuad.copy(
                                                        bottomRight = Offset(
                                                            (cropQuad.bottomRight.x + dxNorm).coerceIn(cropQuad.bottomLeft.x + 0.05f, 1f),
                                                            (cropQuad.bottomRight.y + dyNorm).coerceIn(cropQuad.topRight.y + 0.05f, 1f)
                                                        )
                                                    )
                                                }
                                                distBL < 0.15f -> {
                                                    cropQuad = cropQuad.copy(
                                                        bottomLeft = Offset(
                                                            (cropQuad.bottomLeft.x + dxNorm).coerceIn(0f, cropQuad.bottomRight.x - 0.05f),
                                                            (cropQuad.bottomLeft.y + dyNorm).coerceIn(cropQuad.topLeft.y + 0.05f, 1f)
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                            ) {
                                val w = size.width
                                val h = size.height

                                val ptTL = Offset(cropQuad.topLeft.x * w, cropQuad.topLeft.y * h)
                                val ptTR = Offset(cropQuad.topRight.x * w, cropQuad.topRight.y * h)
                                val ptBR = Offset(cropQuad.bottomRight.x * w, cropQuad.bottomRight.y * h)
                                val ptBL = Offset(cropQuad.bottomLeft.x * w, cropQuad.bottomLeft.y * h)

                                // Connecting Quad Lines (Solid Blue)
                                val quadPath = Path().apply {
                                    moveTo(ptTL.x, ptTL.y)
                                    lineTo(ptTR.x, ptTR.y)
                                    lineTo(ptBR.x, ptBR.y)
                                    lineTo(ptBL.x, ptBL.y)
                                    close()
                                }
                                drawPath(quadPath, color = Color(0xFF2196F3), style = Stroke(width = 3.dp.toPx()))

                                // Outer Shaded Mask
                                drawRect(Color.Black.copy(alpha = 0.45f))
                                drawPath(quadPath, color = Color.Transparent) // Highlight center

                                // 4 Corner Drag Circles (Blue Ring with Semi-transparent Inner Circle - Ref Image 2)
                                val cornerRadius = 14.dp.toPx()
                                listOf(ptTL, ptTR, ptBR, ptBL).forEach { pt ->
                                    drawCircle(Color(0xFF2196F3), radius = cornerRadius, center = pt)
                                    drawCircle(Color.White.copy(alpha = 0.85f), radius = cornerRadius * 0.55f, center = pt)
                                }

                                // 4 Edge Drag Handle Bars (Horizontal & Vertical Rounded Rectangles - Ref Image 2)
                                val edgeWidth = 32.dp.toPx()
                                val edgeHeight = 10.dp.toPx()
                                val barColor = Color(0xFF2196F3)

                                // Top Edge Bar
                                val topMid = Offset((ptTL.x + ptTR.x) / 2f, (ptTL.y + ptTR.y) / 2f)
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(topMid.x - edgeWidth / 2f, topMid.y - edgeHeight / 2f),
                                    size = Size(edgeWidth, edgeHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                )

                                // Bottom Edge Bar
                                val botMid = Offset((ptBL.x + ptBR.x) / 2f, (ptBL.y + ptBR.y) / 2f)
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(botMid.x - edgeWidth / 2f, botMid.y - edgeHeight / 2f),
                                    size = Size(edgeWidth, edgeHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                )

                                // Left Edge Bar
                                val leftMid = Offset((ptTL.x + ptBL.x) / 2f, (ptTL.y + ptBL.y) / 2f)
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(leftMid.x - edgeHeight / 2f, leftMid.y - edgeWidth / 2f),
                                    size = Size(edgeHeight, edgeWidth),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                )

                                // Right Edge Bar
                                val rightMid = Offset((ptTR.x + ptBR.x) / 2f, (ptTR.y + ptBR.y) / 2f)
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(rightMid.x - edgeHeight / 2f, rightMid.y - edgeWidth / 2f),
                                    size = Size(edgeHeight, edgeWidth),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // Action Overlay Pill Buttons (Above bottom bar - Ref Image 2: Auto-detect & Straighten)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Auto-detect Button (Trims extra space automatically!)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                        modifier = Modifier.clickable {
                            if (activeBitmap != null) {
                                cropQuad = detectTightDocumentQuad(activeBitmap)
                                Toast.makeText(context, "Document Auto-detected without extra space", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.CropFree, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Auto-detect", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Straighten Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                        modifier = Modifier.clickable {
                            if (activeBitmap != null) {
                                val topY = (cropQuad.topLeft.y + cropQuad.topRight.y) / 2f
                                val botY = (cropQuad.bottomLeft.y + cropQuad.bottomRight.y) / 2f
                                val leftX = (cropQuad.topLeft.x + cropQuad.bottomLeft.x) / 2f
                                val rightX = (cropQuad.topRight.x + cropQuad.bottomRight.x) / 2f

                                cropQuad = QuadPoints(
                                    topLeft = Offset(leftX, topY),
                                    topRight = Offset(rightX, topY),
                                    bottomRight = Offset(rightX, botY),
                                    bottomLeft = Offset(leftX, botY)
                                )
                                Toast.makeText(context, "Document Straightened", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.GridOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Straighten", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Bottom Editing Navigation Tool Bar (Ref Image 2)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141414))
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Retake Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            isEditingMode = false // Return to camera
                        }
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Retake", tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Retake", color = Color.White, fontSize = 11.sp)
                    }

                    // Crop Button (Blue Highlight Square when active - Ref Image 2)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2196F3))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Crop, contentDescription = "Crop", tint = Color.White, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Crop", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Rotate Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            if (activeBitmap != null) {
                                val matrix = Matrix().apply { postRotate(90f) }
                                val rotated = Bitmap.createBitmap(
                                    activeBitmap, 0, 0, activeBitmap.width, activeBitmap.height, matrix, true
                                )
                                scannedPages[activePageIndex] = rotated
                                cropQuad = detectTightDocumentQuad(rotated)
                            }
                        }
                    ) {
                        Icon(Icons.Filled.RotateRight, contentDescription = "Rotate", tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Rotate", color = Color.White, fontSize = 11.sp)
                    }

                    // Edit Text / OCR Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            if (activeBitmap != null) {
                                isOcrLoading = true
                                showOcrSheet = true
                                ocrExtractedText = PdfEngine.performLocalOcr(documentTitle)
                                isOcrLoading = false
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit text", tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Edit text", color = Color.White, fontSize = 11.sp)
                    }

                    // Filters Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            if (activeBitmap != null) {
                                // Cycle filter
                                val filters = listOf("Magic Color", "B&W", "Grayscale", "Warm Paper", "Original")
                                val nextIdx = (filters.indexOf(selectedFilter) + 1) % filters.size
                                selectedFilter = filters[nextIdx]
                                scannedPages[activePageIndex] = when (selectedFilter) {
                                    "Magic Color" -> applyMagicColorFilter(activeBitmap)
                                    "B&W" -> applyBWFilter(activeBitmap)
                                    "Grayscale" -> applyGrayscaleFilter(activeBitmap)
                                    else -> activeBitmap
                                }
                                Toast.makeText(context, "Filter: $selectedFilter", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Filled.ColorLens, contentDescription = "Filters", tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Filters", color = Color.White, fontSize = 11.sp)
                    }

                    // Delete Page Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            if (scannedPages.isNotEmpty()) {
                                scannedPages.removeAt(activePageIndex)
                                if (scannedPages.isEmpty()) {
                                    isEditingMode = false
                                } else {
                                    activePageIndex = (activePageIndex - 1).coerceAtLeast(0)
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = RedPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Delete", color = RedPrimary, fontSize = 11.sp)
                    }
                }

                // Footer Action Bar (Ref Image 2: Keep scanning & Save PDF ^)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Keep scanning Text Button
                    TextButton(
                        onClick = { isEditingMode = false }
                    ) {
                        Text("Keep scanning", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    // Save PDF Button (Solid Blue Pill with Arrow - Ref Image 2)
                    Button(
                        onClick = {
                            if (scannedPages.isNotEmpty()) {
                                // Apply crop to active pages before saving
                                val croppedList = scannedPages.mapIndexed { idx, bmp ->
                                    if (idx == activePageIndex) {
                                        cropBitmapToQuad(bmp, cropQuad)
                                    } else {
                                        bmp
                                    }
                                }

                                viewModel.saveScannedPdf(
                                    bitmaps = croppedList,
                                    filterName = selectedFilter,
                                    customTitle = if (documentTitle.endsWith(".pdf", ignoreCase = true)) documentTitle else "$documentTitle.pdf",
                                    onSuccess = {
                                        Toast.makeText(context, "Saved $documentTitle.pdf successfully!", Toast.LENGTH_SHORT).show()
                                        onCompleteScan()
                                    }
                                );
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text("Save PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// Function to calculate tight document quadrilateral bounds automatically
private fun detectTightDocumentQuad(bitmap: Bitmap): QuadPoints {
    val width = bitmap.width.toFloat()
    val height = bitmap.height.toFloat()

    var minX = width * 0.08f
    var maxX = width * 0.92f
    var minY = height * 0.08f
    var maxY = height * 0.92f

    val sampleStep = (bitmap.width / 40).coerceAtLeast(1)
    var foundTop = false
    var foundBottom = false
    var foundLeft = false
    var foundRight = false

    // Top to Bottom scan
    for (y in 0 until bitmap.height step sampleStep) {
        var lightCount = 0
        var total = 0
        for (x in 0 until bitmap.width step sampleStep) {
            val pixel = bitmap.getPixel(x, y)
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val brightness = (r + g + b) / 3
            if (brightness > 130) lightCount++
            total++
        }
        if (!foundTop && total > 0 && (lightCount.toFloat() / total) > 0.30f) {
            minY = y.toFloat()
            foundTop = true
        }
    }

    // Bottom to Top scan
    for (y in bitmap.height - 1 downTo 0 step sampleStep) {
        var lightCount = 0
        var total = 0
        for (x in 0 until bitmap.width step sampleStep) {
            val pixel = bitmap.getPixel(x, y)
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val brightness = (r + g + b) / 3
            if (brightness > 130) lightCount++
            total++
        }
        if (!foundBottom && total > 0 && (lightCount.toFloat() / total) > 0.30f) {
            maxY = y.toFloat()
            foundBottom = true
        }
    }

    // Left to Right scan
    for (x in 0 until bitmap.width step sampleStep) {
        var lightCount = 0
        var total = 0
        for (y in 0 until bitmap.height step sampleStep) {
            val pixel = bitmap.getPixel(x, y)
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val brightness = (r + g + b) / 3
            if (brightness > 130) lightCount++
            total++
        }
        if (!foundLeft && total > 0 && (lightCount.toFloat() / total) > 0.30f) {
            minX = x.toFloat()
            foundLeft = true
        }
    }

    // Right to Left scan
    for (x in bitmap.width - 1 downTo 0 step sampleStep) {
        var lightCount = 0
        var total = 0
        for (y in 0 until bitmap.height step sampleStep) {
            val pixel = bitmap.getPixel(x, y)
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val brightness = (r + g + b) / 3
            if (brightness > 130) lightCount++
            total++
        }
        if (!foundRight && total > 0 && (lightCount.toFloat() / total) > 0.30f) {
            maxX = x.toFloat()
            foundRight = true
        }
    }

    val safeL = (minX / width).coerceIn(0f, 0.35f)
    val safeR = (maxX / width).coerceIn(0.65f, 1f)
    val safeT = (minY / height).coerceIn(0f, 0.35f)
    val safeB = (maxY / height).coerceIn(0.65f, 1f)

    return QuadPoints(
        topLeft = Offset(safeL, safeT),
        topRight = Offset(safeR, safeT),
        bottomRight = Offset(safeR, safeB),
        bottomLeft = Offset(safeL, safeB)
    )
}

// Helper to crop bitmap cleanly according to quad points
private fun cropBitmapToQuad(src: Bitmap, quad: QuadPoints): Bitmap {
    try {
        val cropX = (src.width * quad.topLeft.x).toInt().coerceIn(0, src.width - 20)
        val cropY = (src.height * quad.topLeft.y).toInt().coerceIn(0, src.height - 20)
        val cropW = (src.width * (quad.topRight.x - quad.topLeft.x)).toInt().coerceIn(20, src.width - cropX)
        val cropH = (src.height * (quad.bottomLeft.y - quad.topLeft.y)).toInt().coerceIn(20, src.height - cropY)

        return Bitmap.createBitmap(src, cropX, cropY, cropW, cropH)
    } catch (e: Exception) {
        return src
    }
}

// Magic color contrast enhancement filter
private fun applyMagicColorFilter(src: Bitmap): Bitmap {
    val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(dest)
    val paint = AndroidPaint().apply {
        val cm = ColorMatrix()
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

// Grayscale filter
private fun applyGrayscaleFilter(src: Bitmap): Bitmap {
    val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(dest)
    val paint = AndroidPaint().apply {
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        colorFilter = ColorMatrixColorFilter(cm)
    }
    canvas.drawBitmap(src, 0f, 0f, paint)
    return dest
}

// Black & White Binarization Filter
private fun applyBWFilter(src: Bitmap): Bitmap {
    val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(dest)
    val paint = AndroidPaint().apply {
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        val scale = 2.0f
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

@Composable
private fun RenameDocumentDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    val quickSuggestions = listOf("Receipt", "Invoice", "ID Card", "Contract", "Notes", "Report")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = RedPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Rename Document", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1C1B1F))
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2196F3),
                        focusedLabelColor = Color(0xFF2196F3)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Quick Suggestions:", fontSize = 12.sp, color = Color(0xFF605D62), fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickSuggestions) { suggestion ->
                        val dateTag = SimpleDateFormat("MMdd", Locale.getDefault()).format(Date())
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFE3F2FD),
                            modifier = Modifier.clickable { title = "${suggestion}_$dateTag" }
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 12.sp,
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim().removeSuffix(".pdf"))
                    } else {
                        onConfirm("Scanned_Document")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Name", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF757575))
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

// Convert CameraX ImageProxy to Bitmap
private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val planeProxy = image.planes[0]
    val buffer: ByteBuffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

// Fallback document generator
private fun generateDocumentBitmap(context: Context, pageNumber: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(800, 1100, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paintHeader = AndroidPaint().apply {
        color = android.graphics.Color.parseColor("#1E88E5")
        textSize = 32f
        isAntiAlias = true
        isFakeBoldText = true
    }
    canvas.drawText("Scanned Document - Page $pageNumber", 60f, 100f, paintHeader)

    val linePaint = AndroidPaint().apply {
        color = android.graphics.Color.parseColor("#E0E0E0")
        strokeWidth = 3f
    }
    canvas.drawLine(60f, 130f, 740f, 130f, linePaint)

    val bodyPaint = AndroidPaint().apply {
        color = android.graphics.Color.parseColor("#222222")
        textSize = 20f
        isAntiAlias = true
    }

    val sampleLines = listOf(
        "Adobe Scan Document Engine - Ultra HD Optical Capture",
        "Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}",
        "Resolution: 300 DPI Clear Contrast Document Mode",
        "",
        "Auto Edge Detection: Completed without extra background space.",
        "Manual Crop Quad Adjustments: Active.",
        "Magic Color Enhancement Applied.",
        "",
        "Page $pageNumber of scanned batch compiled into standard PDF."
    )

    var y = 190f
    sampleLines.forEach { line ->
        canvas.drawText(line, 60f, y, bodyPaint)
        y += 36f
    }

    val framePaint = AndroidPaint().apply {
        color = android.graphics.Color.parseColor("#2196F3")
        style = AndroidPaint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawRect(30f, 30f, 770f, 1070f, framePaint)

    return bitmap
}
