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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.db.PdfEntity
import com.example.ui.components.AiAssistantBottomSheet
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.PdfDetailsDialog
import com.example.ui.components.RenamePdfDialog
import com.example.ui.theme.GoldStar
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.WarmBorderLight
import com.example.util.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    pdf: PdfEntity?,
    allPdfs: List<PdfEntity> = emptyList(),
    onSelectPdf: ((PdfEntity) -> Unit)? = null,
    onBack: () -> Unit,
    onOpenLocalPdf: ((Uri) -> Unit)? = null,
    onRenamePdf: ((PdfEntity, String) -> Unit)? = null,
    onDeletePdf: ((PdfEntity) -> Unit)? = null,
    onToggleFavorite: ((PdfEntity) -> Unit)? = null,
    onOpenTool: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onOpenLocalPdf?.invoke(uri)
        }
    }

    // When NO pdf is active or provided, render document selection library
    if (pdf == null) {
        PdfDocumentSelectorScreen(
            allPdfs = allPdfs,
            onSelectPdf = { onSelectPdf?.invoke(it) },
            onBrowseDevice = { filePickerLauncher.launch(arrayOf("application/pdf", "*/*")) },
            onBack = onBack,
            onToggleFavorite = onToggleFavorite,
            onDeletePdf = onDeletePdf
        )
        return
    }

    val file = File(pdf.path)
    val fileExists = file.exists() && file.length() > 0

    if (!fileExists) {
        PdfFileNotFoundScreen(
            pdf = pdf,
            onBrowseDevice = { filePickerLauncher.launch(arrayOf("application/pdf", "*/*")) },
            onDeleteFromLibrary = { onDeletePdf?.invoke(pdf) },
            onBack = onBack
        )
        return
    }

    val documentTitle = pdf.title
    val sizeText = pdf.sizeFormatted
    val totalPages = if (fileExists) PdfEngine.getPdfPageCount(file).coerceAtLeast(1) else pdf.pageCount.coerceAtLeast(1)

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val currentPage by remember {
        derivedStateOf { (listState.firstVisibleItemIndex + 1).coerceAtMost(totalPages) }
    }

    var isBookmarked by remember { mutableStateOf(false) }
    var isNightMode by remember { mutableStateOf(false) }
    var showSearchField by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDocSwitcherMenu by remember { mutableStateOf(false) }
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

    // Colors
    val bgColor = if (isNightMode) Color(0xFF121212) else Color(0xFFFAF8F5)
    val topBarColor = if (isNightMode) Color(0xFF1E1E1E) else Color(0xFFFAF8F5)
    val textColor = if (isNightMode) Color(0xFFEEEEEE) else Color(0xFF1C1B1F)
    val cardBgColor = if (isNightMode) Color(0xFF222222) else Color.White

    // Page bitmaps cache
    val renderedPages = remember(pdf.path) { mutableStateMapOf<Int, Bitmap?>() }

    LaunchedEffect(pdf.path) {
        renderedPages.clear()
        withContext(Dispatchers.IO) {
            val count = PdfEngine.getPdfPageCount(file)
            for (p in 0 until count) {
                val bitmap = PdfEngine.renderPageToBitmap(file, p, 900)
                withContext(Dispatchers.Main) {
                    renderedPages[p] = bitmap
                }
            }
        }
    }

    if (showAiBottomSheet) {
        AiAssistantBottomSheet(
            documentContextText = pdf.extractedText ?: "Document title: $documentTitle, Page count: $totalPages",
            onDismiss = { showAiBottomSheet = false }
        )
    }

    if (showRenameDialog) {
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

    if (showDeleteDialog) {
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
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$sizeText • $totalPages Pages",
                                fontSize = 11.sp,
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
                        if (allPdfs.size > 1) {
                            IconButton(onClick = { showDocSwitcherMenu = true }) {
                                Icon(
                                    imageVector = Icons.Filled.SwapHoriz,
                                    contentDescription = "Switch Document",
                                    tint = textColor
                                )
                            }

                            DropdownMenu(
                                expanded = showDocSwitcherMenu,
                                onDismissRequest = { showDocSwitcherMenu = false }
                            ) {
                                Text(
                                    text = "Switch Document",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                HorizontalDivider()
                                allPdfs.forEach { doc ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = doc.title,
                                                fontWeight = if (doc.id == pdf.id) FontWeight.Bold else FontWeight.Normal,
                                                color = if (doc.id == pdf.id) RedPrimary else Color.Unspecified
                                            )
                                        },
                                        onClick = {
                                            showDocSwitcherMenu = false
                                            if (doc.id != pdf.id) {
                                                onSelectPdf?.invoke(doc)
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.PictureAsPdf,
                                                contentDescription = null,
                                                tint = if (doc.id == pdf.id) RedPrimary else Color.Gray
                                            )
                                        }
                                    )
                                }
                            }
                        }

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

                            if (onToggleFavorite != null) {
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

                            if (onRenamePdf != null) {
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
                                    val targetFile = File(pdf.path)
                                    if (targetFile.exists()) {
                                        val saved = PdfEngine.savePdfToDownloads(context, targetFile, documentTitle)
                                        if (saved) {
                                            Toast.makeText(context, "Saved to Downloads: $documentTitle", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) }
                            )

                            if (onDeletePdf != null) {
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
                                text = { Text("Delete Pages from File") },
                                onClick = {
                                    showMenu = false
                                    onOpenTool?.invoke("delete")
                                },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = RedPrimary) }
                            )

                            DropdownMenuItem(
                                text = { Text("Rearrange Page Sequence") },
                                onClick = {
                                    showMenu = false
                                    onOpenTool?.invoke("rearrange")
                                },
                                leadingIcon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = Color.DarkGray) }
                            )

                            DropdownMenuItem(
                                text = { Text("Jump to Page") },
                                onClick = {
                                    showMenu = false
                                    showJumpDialog = true
                                },
                                leadingIcon = { Icon(Icons.Filled.FormatListNumbered, contentDescription = null) }
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
                        .height(56.dp)
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
                                fontSize = 13.sp,
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
                    val renderedBitmap = renderedPages[pageIndex]

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
                        shape = RoundedCornerShape(4.dp),
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
                            // Loading page placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(cardBgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = RedPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Rendering Page ${pageIndex + 1} of $totalPages...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Zoom Controls (+ / - / Reset)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                AnimatedVisibility(visible = scale > 1.05f) {
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
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.RestartAlt,
                                contentDescription = "Reset Zoom",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reset (${String.format("%.1f", scale)}x)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = {
                            scale = (scale - 0.5f).coerceAtLeast(1f)
                            if (scale == 1f) {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        },
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, WarmBorderLight)
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Zoom Out", tint = Color.DarkGray)
                        }
                    }

                    Surface(
                        onClick = {
                            scale = (scale + 0.5f).coerceAtMost(4.5f)
                        },
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, WarmBorderLight)
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Zoom In", tint = RedPrimary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PdfDocumentSelectorScreen(
    allPdfs: List<PdfEntity>,
    onSelectPdf: (PdfEntity) -> Unit,
    onBrowseDevice: () -> Unit,
    onBack: () -> Unit,
    onToggleFavorite: ((PdfEntity) -> Unit)?,
    onDeletePdf: ((PdfEntity) -> Unit)?
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredPdfs = remember(allPdfs, searchQuery) {
        if (searchQuery.isBlank()) allPdfs
        else allPdfs.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Reader", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onBrowseDevice) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Browse Device Files", tint = RedPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAF8F5))
            )
        },
        containerColor = Color(0xFFFAF8F5)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RedPrimary)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Select Document to Read",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Browse files on device or choose from library",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onBrowseDevice,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = RedPrimary)
                        ) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open PDF File from Storage", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search Filter
            if (allPdfs.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search document library...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Document Library (${filteredPdfs.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Tap to Read", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                items(filteredPdfs, key = { it.id }) { pdfItem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPdf(pdfItem) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, WarmBorderLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFEBEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PictureAsPdf,
                                    contentDescription = null,
                                    tint = RedPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pdfItem.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${pdfItem.sizeFormatted} • ${pdfItem.pageCount} Pages • ${pdfItem.dateModifiedFormatted}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            if (onToggleFavorite != null) {
                                IconButton(onClick = { onToggleFavorite(pdfItem) }) {
                                    Icon(
                                        imageVector = if (pdfItem.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = "Favorite",
                                        tint = if (pdfItem.isFavorite) GoldStar else Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, WarmBorderLight)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No Documents in Library", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Import a PDF file from your device storage to start reading.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PdfFileNotFoundScreen(
    pdf: PdfEntity,
    onBrowseDevice: () -> Unit,
    onDeleteFromLibrary: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Not Found", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFFFAF8F5)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = RedPrimary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Document File Missing", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Could not locate file: ${pdf.title}\nPath: ${pdf.path}",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onBrowseDevice,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
            ) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browse Storage for File")
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onDeleteFromLibrary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remove from Document Library", color = Color.Gray)
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
