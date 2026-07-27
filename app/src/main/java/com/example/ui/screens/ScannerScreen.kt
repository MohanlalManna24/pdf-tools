package com.example.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.util.PdfEngine
import com.example.util.ScanImageProcessor
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

private val ElectricBlue = Color(0xFF2563EB)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    onClose: () -> Unit,
    onProceedToEdit: (List<Bitmap>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val capturedPages = remember { mutableStateListOf<Bitmap>() }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var isAutoMode by remember { mutableStateOf(true) }
    var isCapturing by remember { mutableStateOf(false) }
    var shutterFlashVisible by remember { mutableStateOf(false) }

    var selectedScanMode by remember { mutableStateOf("DOCUMENT") }
    val scanModes = listOf("WHITEBOARD", "FORM", "DOCUMENT", "BUSINESS CARD", "ID CARD")

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    // Multi-image gallery picker contract
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                uris.forEach { uri ->
                    val file = PdfEngine.getFileFromUri(context, uri)
                    if (file != null) {
                        val bmp = ScanImageProcessor.decodeOrientedBitmap(file)
                        if (bmp != null) {
                            withContext(Dispatchers.Main) {
                                capturedPages.add(bmp)
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val providerFuture = ProcessCameraProvider.getInstance(context)
                if (providerFuture.isDone) {
                    providerFuture.get().unbindAll()
                }
            } catch (e: Exception) {
                Log.e("ScannerScreen", "Camera cleanup failed", e)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("scanner_screen")
    ) {
        // --- 1. CAMERA PREVIEW / FALLBACK VIEW ---
        if (cameraPermissionState.status.isGranted) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
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
                                .setFlashMode(flashMode)
                                .build()

                            imageCapture = capture

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            Log.e("ScannerScreen", "Camera binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Simulated / Preview camera background when camera permission isn't granted or unavailable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1C1917)),
                contentAlignment = Alignment.Center
            ) {
                val sampleBmp = remember { ScanImageProcessor.createSampleScanBitmap(capturedPages.size + 1) }
                Image(
                    bitmap = sampleBmp.asImageBitmap(),
                    contentDescription = "Document Scan Preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    contentScale = ContentScale.Fit,
                    alpha = 0.85f
                )
            }
        }

        // --- 2. DOCUMENT BOUNDING FRAME OVERLAY ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 90.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
            ) {
                // Electric Blue Corner Brackets
                ElectricBlueCornerBrackets()

                // Center floating badge "Capturing... hold steady"
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(ElectricBlue, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isCapturing) "Capturing... hold steady" else "Position document in frame",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- 3. TOP TOOLBAR (Home, Flash Auto, Settings) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Home Icon Button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("scanner_close_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Home",
                    tint = Color.White
                )
            }

            // Right: Flash Auto & Settings Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                        imageCapture?.flashMode = flashMode
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    val icon = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Filled.FlashAuto
                        else -> Icons.Filled.FlashOff
                    }
                    Icon(imageVector = icon, contentDescription = "Flash Mode", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = { /* Settings action */ },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }

        // --- 4. BOTTOM CONTROLS PANEL ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 4A. MODE SELECTOR CAROUSEL (WHITEBOARD, FORM, DOCUMENT, BUSINESS CARD, etc.) ---
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(scanModes) { mode ->
                    val isSelected = mode == selectedScanMode
                    Text(
                        text = mode,
                        color = if (isSelected) ElectricBlue else Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .clickable { selectedScanMode = mode }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    )
                }
            }

            // --- 4B. BOTTOM ACTION BAR (Gallery, Auto, Shutter, Flash, Preview Badge) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Gallery Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF262626), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 2. Auto Capture Toggle Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { isAutoMode = !isAutoMode }
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoFixHigh,
                        contentDescription = "Auto Mode",
                        tint = if (isAutoMode) ElectricBlue else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "AUTO",
                        color = if (isAutoMode) ElectricBlue else Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // 3. Main Center Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clickable {
                            if (isCapturing) return@clickable
                            isCapturing = true
                            shutterFlashVisible = true

                            scope.launch {
                                val cap = imageCapture
                                if (cameraPermissionState.status.isGranted && cap != null) {
                                    val executor = Executors.newSingleThreadExecutor()
                                    cap.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val plane = image.planes[0]
                                            val buffer = plane.buffer
                                            val bytes = ByteArray(buffer.remaining())
                                            buffer.get(bytes)
                                            var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            if (bitmap != null) {
                                                val rotation = image.imageInfo.rotationDegrees
                                                if (rotation != 0) {
                                                    bitmap = ScanImageProcessor.rotateBitmap(bitmap, rotation.toFloat())
                                                }
                                                capturedPages.add(bitmap)
                                            }
                                            image.close()
                                            isCapturing = false
                                            shutterFlashVisible = false
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e("ScannerScreen", "Photo capture failed", exception)
                                            capturedPages.add(ScanImageProcessor.createSampleScanBitmap(capturedPages.size + 1))
                                            isCapturing = false
                                            shutterFlashVisible = false
                                        }
                                    })
                                } else {
                                    capturedPages.add(ScanImageProcessor.createSampleScanBitmap(capturedPages.size + 1))
                                    isCapturing = false
                                    shutterFlashVisible = false
                                }
                            }
                        }
                        .testTag("scan_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Blue Ring
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(3.5.dp, ElectricBlue, CircleShape)
                    )
                    // Inner White Shutter Core
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .background(Color.White, CircleShape)
                    )
                }

                // 4. Flash Toggle Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                        imageCapture?.flashMode = flashMode
                    }
                ) {
                    val icon = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Filled.FlashAuto
                        else -> Icons.Filled.FlashOff
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Flash",
                        tint = if (flashMode != ImageCapture.FLASH_MODE_OFF) ElectricBlue else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "FLASH",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // 5. Stacked Thumbnail Preview with Blue Count Badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("scanned_pages_thumbnail")
                        .clickable {
                            if (capturedPages.isNotEmpty()) {
                                onProceedToEdit(capturedPages.toList())
                            } else {
                                onProceedToEdit(listOf(ScanImageProcessor.createSampleScanBitmap(1)))
                            }
                        }
                ) {
                    if (capturedPages.isNotEmpty()) {
                        Image(
                            bitmap = capturedPages.last().asImageBitmap(),
                            contentDescription = "Last Scan Thumbnail",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF262626), RoundedCornerShape(10.dp))
                                .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Image,
                                contentDescription = "Scans",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Electric Blue Circle Badge Count at Bottom Right
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .background(ElectricBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = capturedPages.size.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Shutter Flash Animation Overlay
        AnimatedVisibility(
            visible = shutterFlashVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.7f))
            )
        }
    }
}

@Composable
fun ElectricBlueCornerBrackets(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 4.dp
        val bracketSize = 32.dp
        val blueColor = ElectricBlue

        // Top-Left Corner
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(bracketSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .background(blueColor)
            )
            Box(
                modifier = Modifier
                    .width(strokeWidth)
                    .fillMaxHeight()
                    .background(blueColor)
            )
        }

        // Top-Right Corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(bracketSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .background(blueColor)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(strokeWidth)
                    .fillMaxHeight()
                    .background(blueColor)
            )
        }

        // Bottom-Left Corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(bracketSize)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .background(blueColor)
            )
            Box(
                modifier = Modifier
                    .width(strokeWidth)
                    .fillMaxHeight()
                    .background(blueColor)
            )
        }

        // Bottom-Right Corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(bracketSize)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .background(blueColor)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(strokeWidth)
                    .fillMaxHeight()
                    .background(blueColor)
            )
        }
    }
}
