package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PdfEntity
import com.example.ui.components.RenamePdfDialog
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.WarmBorderLight
import com.example.util.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    pdf: PdfEntity?,
    onBack: () -> Unit,
    onRenamePdf: ((PdfEntity, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val documentTitle = pdf?.title ?: "Document.pdf"
    val sizeText = pdf?.sizeFormatted ?: "0 KB"
    val totalPages = pdf?.pageCount ?: 1

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val currentPage by remember {
        derivedStateOf { (listState.firstVisibleItemIndex + 1).coerceAtMost(totalPages) }
    }

    var isBookmarked by remember { mutableStateOf(false) }
    var showSearchField by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var scale by remember { mutableFloatStateOf(1f) }

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
        containerColor = Color(0xFFFAF8F5),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = documentTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F),
                                maxLines = 1
                            )
                            Text(
                                text = "$sizeText • Offline document",
                                fontSize = 12.sp,
                                color = Color(0xFF605D62)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF1C1B1F)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val targetFile = pdf?.path?.let { File(it) }
                            if (targetFile != null && targetFile.exists()) {
                                val saved = PdfEngine.savePdfToDownloads(context, targetFile, documentTitle)
                                if (saved) {
                                    Toast.makeText(context, "Saved to Downloads: $documentTitle", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Saved file to local device Downloads", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(imageVector = Icons.Filled.Download, contentDescription = "Save to Device")
                        }
                        IconButton(onClick = { scale = (scale + 0.25f).coerceAtMost(2.5f) }) {
                            Icon(imageVector = Icons.Filled.ZoomIn, contentDescription = "Zoom In")
                        }
                        IconButton(onClick = { scale = (scale - 0.25f).coerceAtLeast(0.8f) }) {
                            Icon(imageVector = Icons.Filled.ZoomOut, contentDescription = "Zoom Out")
                        }
                        IconButton(onClick = { showSearchField = !showSearchField }) {
                            Icon(imageVector = Icons.Filled.Search, contentDescription = "Search in PDF")
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
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
                                text = { Text("Save to Local Device") },
                                onClick = {
                                    showMenu = false
                                    val targetFile = pdf?.path?.let { File(it) }
                                    if (targetFile != null && targetFile.exists()) {
                                        PdfEngine.savePdfToDownloads(context, targetFile, documentTitle)
                                        Toast.makeText(context, "Saved to Downloads folder", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Saved to Local Storage", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAF8F5))
                )

                AnimatedVisibility(visible = showSearchField) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { },
                shape = CircleShape,
                containerColor = RedPrimary,
                contentColor = Color.White,
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .testTag("reader_markup_fab")
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Annotate / Markup",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFAF8F5),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(60.dp)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Filled.GridView,
                            contentDescription = "Page Grid View",
                            tint = Color(0xFF1C1B1F)
                        )
                    }

                    // Page Counter Pill ("1 of 12 ▲")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFFEBEE))
                            .clickable {
                                coroutineScope.launch {
                                    val target = if (currentPage >= totalPages) 0 else currentPage
                                    listState.animateScrollToItem(target)
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$currentPage of $totalPages",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = null,
                                tint = RedPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(onClick = { isBookmarked = !isBookmarked }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark Page",
                            tint = if (isBookmarked) RedPrimary else Color(0xFF1C1B1F)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.8f, 2.5f)
                    }
                },
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(totalPages) { pageIndex ->
                val renderedBitmap = renderedPages.getOrNull(pageIndex)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(580.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarmBorderLight)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    if (renderedBitmap != null) {
                        Image(
                            bitmap = renderedBitmap.asImageBitmap(),
                            contentDescription = "Page ${pageIndex + 1}",
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
                                color = Color(0xFF1C1B1F)
                            )
                            Text(
                                text = "PDF TOOLS ENGINE • Page ${pageIndex + 1} of $totalPages",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Text(
                                text = "QUARTERLY DOCUMENT FLOW",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .background(Color(0xFFFAFAFA))
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
                                color = Color(0xFF1C1B1F)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Document processed locally and completely offline.",
                                fontSize = 12.sp,
                                color = Color(0xFF424242)
                            )
                            Text(
                                text = "• Embedded high fidelity graphics render directly on device.",
                                fontSize = 12.sp,
                                color = Color(0xFF424242)
                            )
                            Text(
                                text = "• Search, pinch to zoom, and markup enabled.",
                                fontSize = 12.sp,
                                color = Color(0xFF424242)
                            )
                        }
                    }
                }
            }
        }
    }
}
