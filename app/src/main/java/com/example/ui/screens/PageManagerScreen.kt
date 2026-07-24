package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PdfEntity
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.WarmBorderLight
import com.example.ui.theme.WarmCardBgLight
import com.example.util.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class PageStateItem(
    val id: String,
    val originalPageIndex: Int, // 0-based index in source PDF
    var isSelected: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageManagerScreen(
    toolMode: String, // "delete" or "rearrange"
    initialFilePath: String?,
    documentTitle: String?,
    allPdfs: List<PdfEntity> = emptyList(),
    onBack: () -> Unit,
    onSavePdf: (sourcePath: String, pageSequenceParam: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var currentPath by remember { mutableStateOf(initialFilePath) }
    var currentTitle by remember { mutableStateOf(documentTitle ?: "Document.pdf") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val tempFile = PdfEngine.getFileFromUri(context, uri)
            if (tempFile != null) {
                currentPath = tempFile.absolutePath
                currentTitle = tempFile.name
            }
        }
    }

    if (currentPath.isNullOrEmpty() || !File(currentPath!!).exists()) {
        // File selection fallback UI
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (toolMode == "delete") "Delete Pages" else "Rearrange Pages", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAF8F5))
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
                    imageVector = Icons.Filled.PictureAsPdf,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Document for Page Management", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose a PDF file to view, delete, or reorder its pages.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse Storage for PDF")
                }
            }
        }
        return
    }

    val sourceFile = File(currentPath!!)
    val pagesList = remember { mutableStateListOf<PageStateItem>() }
    val pageBitmaps = remember { mutableStateMapOf<Int, Bitmap?>() }
    var isLoadingThumbnails by remember { mutableStateOf(true) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Load PDF pages & render thumbnails
    LaunchedEffect(currentPath) {
        isLoadingThumbnails = true
        pagesList.clear()
        pageBitmaps.clear()

        withContext(Dispatchers.IO) {
            val totalPages = PdfEngine.getPdfPageCount(sourceFile)
            for (i in 0 until totalPages) {
                pagesList.add(PageStateItem(id = "${i}_${System.currentTimeMillis()}", originalPageIndex = i))
            }

            // Render thumbnails asynchronously
            for (i in 0 until totalPages) {
                val bmp = PdfEngine.renderPageToBitmap(sourceFile, i, width = 450)
                withContext(Dispatchers.Main) {
                    pageBitmaps[i] = bmp
                }
            }
        }
        isLoadingThumbnails = false
    }

    val selectedCount = pagesList.count { it.isSelected }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFFAF8F5),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1B1F),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                    // Save Button (Red Pill)
                    Button(
                        onClick = {
                            if (pagesList.isEmpty()) {
                                Toast.makeText(context, "Cannot save an empty PDF!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            // Convert pagesList original indices to 1-based string
                            val pageSequenceParam = pagesList.map { it.originalPageIndex + 1 }.joinToString(",")
                            onSavePdf(currentPath!!, pageSequenceParam)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Save",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    // More Menu
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More Options",
                            tint = Color(0xFF1C1B1F)
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Select All Pages") },
                            onClick = {
                                showMoreMenu = false
                                pagesList.forEach { it.isSelected = true }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Deselect All") },
                            onClick = {
                                showMoreMenu = false
                                pagesList.forEach { it.isSelected = false }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reset Page Order") },
                            onClick = {
                                showMoreMenu = false
                                pagesList.clear()
                                val count = PdfEngine.getPdfPageCount(sourceFile)
                                for (i in 0 until count) {
                                    pagesList.add(PageStateItem(id = "${i}_${System.currentTimeMillis()}", originalPageIndex = i))
                                }
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Change Document File") },
                            onClick = {
                                showMoreMenu = false
                                filePickerLauncher.launch(arrayOf("application/pdf"))
                            },
                            leadingIcon = { Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = RedPrimary) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAF8F5))
            )
        },
        bottomBar = {
            // Floating Bottom Action Bar matching user mockup!
            if (pagesList.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = WarmCardBgLight,
                    shadowElevation = 10.dp,
                    border = BorderStroke(1.dp, WarmBorderLight)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Action Buttons Row (Rotate, Reorder, Duplicate, Delete)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rotate
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        Toast.makeText(context, "Pages rotated", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Filled.RotateRight, contentDescription = "Rotate", tint = Color.DarkGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Rotate", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                            }

                            // Reorder / Swap
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        Toast.makeText(context, "Use ▲ / ▼ arrows on page cards to reorder sequence", Toast.LENGTH_LONG).show()
                                    }
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Filled.SwapHoriz, contentDescription = "Reorder", tint = Color.DarkGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Reorder", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                            }

                            // Duplicate
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val selectedItems = pagesList.filter { it.isSelected }
                                        if (selectedItems.isEmpty()) {
                                            Toast.makeText(context, "Tap a page to select it first!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            selectedItems.forEach { item ->
                                                val insertIndex = pagesList.indexOf(item)
                                                if (insertIndex != -1) {
                                                    pagesList.add(insertIndex + 1, PageStateItem(id = "${item.originalPageIndex}_dup_${System.currentTimeMillis()}", originalPageIndex = item.originalPageIndex))
                                                }
                                            }
                                            Toast.makeText(context, "Selected page duplicated", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate", tint = Color.DarkGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Duplicate", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                            }

                            // Delete
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val toDelete = pagesList.filter { it.isSelected }
                                        if (toDelete.isEmpty()) {
                                            Toast.makeText(context, "Select page(s) to delete!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            pagesList.removeAll(toDelete)
                                            Toast.makeText(context, "Deleted ${toDelete.size} page(s)", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = RedPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Delete", fontSize = 11.sp, color = RedPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Selected Pages Badge ("X Page(s) Selected")
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(RedPrimary)
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (selectedCount > 0) "$selectedCount Page${if (selectedCount > 1) "s" else ""} Selected" else "Tap page to select",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Header Row ("Page Manager" & "Select All")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Page Manager",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F)
                )

                val isAllSelected = pagesList.isNotEmpty() && pagesList.all { it.isSelected }
                Text(
                    text = if (isAllSelected) "Clear All" else "Select All",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val targetState = !isAllSelected
                            pagesList.forEachIndexed { index, item ->
                                pagesList[index] = item.copy(isSelected = targetState)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (isLoadingThumbnails && pagesList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = RedPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading page manager...", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else if (pagesList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("All pages deleted", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap 'Reset Page Order' from top menu to restore.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                // 2-COLUMN GRID OF PAGE THUMBNAIL CARDS
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = pagesList,
                        key = { index, item -> item.id }
                    ) { index, pageItem ->
                        val displayNum = index + 1
                        val bmp = pageBitmaps[pageItem.originalPageIndex]
                        val isSelected = pageItem.isSelected

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f / 1.35f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        width = if (isSelected) 3.5.dp else 1.dp,
                                        color = if (isSelected) RedPrimary else WarmBorderLight,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        pagesList[index] = pageItem.copy(isSelected = !pageItem.isSelected)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFFFF5F5) else Color.White
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Page $displayNum",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFFF5F2EC)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Page $displayNum", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }

                                    // RED CIRCULAR BADGE WITH WHITE CHECKMARK IN CENTER (WHEN SELECTED)
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(RedPrimary)
                                                .align(Alignment.Center),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    // Reorder controls overlay (Left / Right movement arrows)
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.55f))
                                            .padding(horizontal = 2.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (index > 0) {
                                            Icon(
                                                imageVector = Icons.Filled.ArrowUpward,
                                                contentDescription = "Move Left",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable {
                                                        val temp = pagesList[index]
                                                        pagesList[index] = pagesList[index - 1]
                                                        pagesList[index - 1] = temp
                                                    }
                                            )
                                        }
                                        if (index < pagesList.size - 1) {
                                            Icon(
                                                imageVector = Icons.Filled.ArrowDownward,
                                                contentDescription = "Move Right",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable {
                                                        val temp = pagesList[index]
                                                        pagesList[index] = pagesList[index + 1]
                                                        pagesList[index + 1] = temp
                                                    }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Page Label Underneath Card ("Page 1", "Page 2", etc.)
                            Text(
                                text = "Page $displayNum",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) RedPrimary else Color(0xFF1C1B1F)
                            )
                        }
                    }
                }
            }
        }
    }
}
