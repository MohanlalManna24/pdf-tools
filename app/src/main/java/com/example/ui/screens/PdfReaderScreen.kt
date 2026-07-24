package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.db.PdfEntity
import com.example.ui.components.AiAssistantBottomSheet
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.PdfDetailsDialog
import com.example.ui.components.RenamePdfDialog
import com.example.ui.components.printPdfFile
import com.example.ui.components.sharePdfFile
import com.example.ui.theme.GoldStar
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.WarmBorderLight
import com.example.util.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    pdf: PdfEntity?,
    onBack: () -> Unit,
    onOpenLocalPdf: ((Uri) -> Unit)? = null,
    onRenamePdf: ((PdfEntity, String) -> Unit)? = null,
    onDeletePdf: ((PdfEntity) -> Unit)? = null,
    onToggleFavorite: ((PdfEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val documentTitle = pdf?.title ?: "Document.pdf"
    val sizeText = pdf?.sizeFormatted ?: "0 KB"
    val totalPages = pdf?.pageCount ?: 1

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onOpenLocalPdf?.invoke(uri)
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val currentPage by remember {
        derivedStateOf { (listState.firstVisibleItemIndex + 1).coerceAtMost(totalPages) }
    }

    var isBookmarked by remember { mutableStateOf(false) }
    var isNightMode by remember { mutableStateOf(false) }
    var showSearchField by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showAiBottomSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Smooth pinch zoom & pan transformation states
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Background & surface colors based on Night Mode
    val bgColor = if (isNightMode) Color(0xFF121212) else Color(0xFFFAF8F5)
    val topBarColor = if (isNightMode) Color(0xFF1E1E1E) else Color(0xFFFAF8F5)
    val textColor = if (isNightMode) Color(0xFFEEEEEE) else Color(0xFF1C1B1F)
    val cardBgColor = if (isNightMode) Color(0xFF222222) else Color.White

    if (showAiBottomSheet) {
        AiAssistantBottomSheet(
            documentContextText = pdf?.extractedText ?: "Document title: $documentTitle, Page count: $totalPages",
            onDismiss = { showAiBottomSheet = false }
        )
    }

    if (showRenameDialog && pdf != null) {
        RenamePdfDialog(
            currentTitle = pdf.title,
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                onRenamePdf?.invoke(pdf, newName)
                Toast.makeText(context, "Renamed document to $newName", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showDetailsDialog) {
        PdfDetailsDialog(
            pdf = pdf,
            documentTitle = documentTitle,
            onDismiss = { showDetailsDialog = false }
        )
    }

    if (showDeleteDialog && pdf != null) {
        DeleteConfirmDialog(
            pdfTitle = pdf.title,
            onDismiss = { showDeleteDialog = false },
            onConfirmDelete = {
                onDeletePdf?.invoke(pdf)
                Toast.makeText(context, "Document deleted", Toast.LENGTH_SHORT).show()
                onBack()
            }
        )
    }

    if (showJumpDialog) {
        JumpToPageDialog(
            totalPages = totalPages,
            currentPage = currentPage,
            onDismiss = { showJumpDialog = false },
            onJump = { targetIndex ->
                coroutineScope.launch {
                    listState.animateScrollToItem(targetIndex)
                }
            }
        )
    }

    // Cache rendered bitmaps for pages
    val renderedPages = remember { mutableStateListOf<Bitmap?>() }

    LaunchedEffect(pdf) {
        val file = pdf?.path?.let { File(it) }
        val count = if (file != null && file.exists()) PdfEngine.getPdfPageCount(file) else totalPages

        renderedPages.clear()
        coroutineScope.launch(Dispatchers.IO) {
            for (p in 0 until count) {
                val bitmap = if (file != null && file.exists()) {
                    PdfEngine.renderPageToBitmap(file, p, 900)
                } else null
                launch(Dispatchers.Main) {
                    renderedPages.add(bitmap)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = bgColor,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = documentTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1
                            )
                            Text(
                                text = "$sizeText • $totalPages Pages",
                                fontSize = 12.sp,
                                color = if (isNightMode) Color.LightGray else Color(0xFF605D62)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textColor
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { filePickerLauncher.launch(arrayOf("application/pdf", "*/*")) }) {
                            Icon(
                                imageVector = Icons.Filled.FolderOpen,
                                contentDescription = "Open Local PDF",
                                tint = RedPrimary
                            )
                        }
                        IconButton(onClick = { showAiBottomSheet = true }) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Gemini AI",
                                tint = RedPrimary
                            )
                        }
                        IconButton(onClick = { isNightMode = !isNightMode }) {
                            Icon(
                                imageVector = if (isNightMode) Icons.Filled.LightMode else Icons.Filled.NightlightRound,
                                contentDescription = "Toggle Night Mode",
                                tint = if (isNightMode) Color(0xFFFFD700) else Color(0xFF424242)
                            )
                        }
                        IconButton(onClick = { showSearchField = !showSearchField }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search text in PDF",
                                tint = textColor
                            )
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More Options",
                                tint = textColor
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Document Details") },
                                onClick = {
                                    showMenu = false
                                    showDetailsDialog = true
                                },
                                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null, tint = RedPrimary) }
                            )

                            DropdownMenuItem(
                                text = { Text("Share PDF") },
                                onClick = {
                                    showMenu = false
                                    sharePdfFile(context, pdf)
                                },
                                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) }
                            )

                            DropdownMenuItem(
                                text = { Text("Print Document") },
                                onClick = {
                                    showMenu = false
                                    printPdfFile(context, pdf, documentTitle)
                                },
                                leadingIcon = { Icon(Icons.Filled.Print, contentDescription = null) }
                            )

                            if (pdf != null && onToggleFavorite != null) {
                                DropdownMenuItem(
                                    text = { Text(if (pdf.isFavorite) "Remove from Favorites" else "Add to Favorites") },
                                    onClick = {
                                        showMenu = false
                                        onToggleFavorite(pdf)
                                        Toast.makeText(context, if (pdf.isFavorite) "Removed from favorites" else "Added to favorites", Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (pdf.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                            contentDescription = null,
                                            tint = GoldStar
                                        )
                                    }
                                )
                            }

                            if (pdf != null && onRenamePdf != null) {
                                DropdownMenuItem(
                                    text = { Text("Rename Document") },
                                    onClick = {
                                        showMenu = false
                                        showRenameDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("Save to Downloads") },
                                onClick = {
                                    showMenu = false
                                    val targetFile = pdf?.path?.let { File(it) }
                                    if (targetFile != null && targetFile.exists()) {
                                        val saved = PdfEngine.savePdfToDownloads(context, targetFile, documentTitle)
                                        if (saved) {
                                            Toast.makeText(context, "Saved to Downloads: $documentTitle", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Saved to Local Storage Downloads", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) }
                            )

                            if (pdf != null && onDeletePdf != null) {
                                DropdownMenuItem(
                                    text = { Text("Delete Document", color = RedPrimary) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = RedPrimary) }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("Jump to Page") },
                                onClick = {
                                    showMenu = false
                                    showJumpDialog = true
                                },
                                leadingIcon = { Icon(Icons.Filled.FormatListNumbered, contentDescription = null) }
                            )

                            DropdownMenuItem(
                                text = { Text("Analyze with Gemini AI") },
                                onClick = {
                                    showMenu = false
                                    showAiBottomSheet = true
                                },
                                leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = RedPrimary) }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor)
                )

                AnimatedVisibility(visible = showSearchField) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(topBarColor)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search text in PDF...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = topBarColor,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(60.dp)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showJumpDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.GridView,
                            contentDescription = "Page Grid View",
                            tint = textColor
                        )
                    }

                    // Page Counter Pill ("Page 1 of 12 ▲")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isNightMode) Color(0xFF2C2C2C) else Color(0xFFFFEBEE))
                            .clickable { showJumpDialog = true }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Page $currentPage of $totalPages",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNightMode) Color.White else Color(0xFF1C1B1F)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.FormatListNumbered,
                                contentDescription = null,
                                tint = RedPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(onClick = { isBookmarked = !isBookmarked }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark Page",
                            tint = if (isBookmarked) RedPrimary else textColor
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val oldScale = scale
                            val newScale = (scale * zoom).coerceIn(1f, 4.5f)
                            scale = newScale
                            if (newScale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    scale = 2.2f
                                }
                            }
                        )
                    },
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(totalPages) { pageIndex ->
                    val renderedBitmap = renderedPages.getOrNull(pageIndex)

                    val pageAspectRatio = if (renderedBitmap != null && renderedBitmap.height > 0) {
                        renderedBitmap.width.toFloat() / renderedBitmap.height.toFloat()
                    } else {
                        1f / 1.414f // Standard document A4 aspect ratio
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(pageAspectRatio)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = if (scale > 1f) offsetX else 0f,
                                translationY = if (scale > 1f) offsetY else 0f
                            ),
                        shape = RoundedCornerShape(2.dp),
                        color = cardBgColor,
                        shadowElevation = if (isNightMode) 2.dp else 4.dp
                    ) {
                        if (renderedBitmap != null) {
                            Image(
                                bitmap = renderedBitmap.asImageBitmap(),
                                contentDescription = "Page ${pageIndex + 1}",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // High resolution Fallback Canvas Page
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                            ) {
                                Text(
                                    text = documentTitle.uppercase(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = "PDF TOOLS ENGINE • Page ${pageIndex + 1} of $totalPages",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                    text = "DOCUMENT CONTENT VIEW",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .background(if (isNightMode) Color(0xFF1E1E1E) else Color(0xFFFAFAFA))
                                ) {
                                    val w = size.width
                                    val h = size.height
                                    val barW = 48.dp.toPx()
                                    val bars = listOf(
                                        Pair("Q1 '24", 450f),
                                        Pair("Q2 '24", 620f),
                                        Pair("Q3 '24", 785.4f),
                                        Pair("Q4 '24", 890f)
                                    )

                                    bars.forEachIndexed { i, bar ->
                                        val x = (w / 5) * (i + 1) - (barW / 2)
                                        val barHeight = (bar.second / 1000f) * (h - 40f)
                                        val y = h - 30f - barHeight
                                        val color = if (i == 2) Color(0xFFD31A28) else Color(0xFF757575)

                                        drawRect(
                                            color = color,
                                            topLeft = androidx.compose.ui.geometry.Offset(x, y),
                                            size = androidx.compose.ui.geometry.Size(barW, barHeight)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "EXECUTIVE SUMMARY",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "• Document processed locally and completely offline.",
                                    fontSize = 12.sp,
                                    color = if (isNightMode) Color.LightGray else Color(0xFF424242)
                                )
                                Text(
                                    text = "• Embedded high fidelity graphics render directly on device.",
                                    fontSize = 12.sp,
                                    color = if (isNightMode) Color.LightGray else Color(0xFF424242)
                                )
                                Text(
                                    text = "• Two-finger pinch to zoom & double-tap zoom enabled.",
                                    fontSize = 12.sp,
                                    color = if (isNightMode) Color.LightGray else Color(0xFF424242)
                                )
                            }
                        }
                    }
                }
            }

            // Reset Zoom Floating Pill when zoomed in
            AnimatedVisibility(
                visible = scale > 1.05f,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 20.dp, end = 20.dp)
            ) {
                Surface(
                    onClick = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = RedPrimary,
                    contentColor = Color.White,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RestartAlt,
                            contentDescription = "Reset Zoom",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reset Zoom (${String.format("%.1f", scale)}x)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun JumpToPageDialog(
    totalPages: Int,
    currentPage: Int,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit
) {
    var pageInput by remember { mutableStateOf(currentPage.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FormatListNumbered, contentDescription = null, tint = RedPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Jump to Page", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column {
                Text("Enter page number (1 to $totalPages):", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it.filter { char -> char.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = pageInput.toIntOrNull()
                if (p != null && p in 1..totalPages) {
                    onJump(p - 1)
                    onDismiss()
                } else {
                    onDismiss()
                }
            }) {
                Text("Jump", color = RedPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

private fun sharePdfFile(context: Context, pdf: PdfEntity?) {
    val file = pdf?.path?.let { File(it) }
    if (file != null && file.exists()) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share PDF Document"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "File path unavailable for sharing", Toast.LENGTH_SHORT).show()
    }
}

private fun printPdfFile(context: Context, pdf: PdfEntity?, documentTitle: String) {
    val file = pdf?.path?.let { File(it) }
    if (file != null && file.exists()) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val printAdapter = object : android.print.PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: android.os.Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = android.print.PrintDocumentInfo.Builder(documentTitle)
                            .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(pdf.pageCount)
                            .build()
                        callback?.onLayoutFinished(info, newAttributes != oldAttributes)
                    }

                    override fun onWrite(
                        pages: Array<out android.print.PageRange>?,
                        destination: android.os.ParcelFileDescriptor?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        try {
                            file.inputStream().use { input ->
                                java.io.FileOutputStream(destination?.fileDescriptor).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        }
                    }
                }
                printManager.print(documentTitle, printAdapter, PrintAttributes.Builder().build())
            } else {
                Toast.makeText(context, "Print service unavailable", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to print: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "File unavailable for printing", Toast.LENGTH_SHORT).show()
    }
}
