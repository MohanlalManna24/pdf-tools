package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PdfEntity
import com.example.ui.components.PdfThumbnailView
import com.example.ui.components.RenamePdfDialog
import com.example.ui.theme.GoldStar
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.WarmBorderLight
import com.example.ui.theme.WarmCardBgLight
import com.example.ui.viewmodel.MainViewModel
import com.example.util.PdfEngine
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    viewModel: MainViewModel,
    onOpenPdf: (PdfEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allFiles by viewModel.allFiles.collectAsState()
    val favoriteFiles by viewModel.favoriteFiles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var selectedFilterTab by remember { mutableStateOf("All") } // "All", "Scanned", "Favorites"
    var pdfToRename by remember { mutableStateOf<PdfEntity?>(null) }

    val pdfForDialog = pdfToRename
    if (pdfForDialog != null) {
        RenamePdfDialog(
            currentTitle = pdfForDialog.title,
            onDismiss = { pdfToRename = null },
            onRename = { newName ->
                viewModel.renamePdf(pdfForDialog, newName)
                Toast.makeText(context, "Renamed document to $newName", Toast.LENGTH_SHORT).show()
            }
        )
    }

    val displayedFiles = remember(allFiles, favoriteFiles, selectedFilterTab, searchQuery) {
        val q = searchQuery.trim()
        val filteredFavs = if (q.isEmpty()) favoriteFiles else favoriteFiles.filter {
            it.title.contains(q, ignoreCase = true) ||
            it.category.contains(q, ignoreCase = true) ||
            (it.extractedText?.contains(q, ignoreCase = true) == true)
        }
        when (selectedFilterTab) {
            "Scanned" -> allFiles.filter { it.category.equals("SCANNER", ignoreCase = true) || it.title.contains("scan", ignoreCase = true) }
            "Favorites" -> filteredFavs
            else -> allFiles
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFFAF8F5),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RedPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Folder,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "My Documents",
                            fontWeight = FontWeight.Bold,
                            color = RedPrimary,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAF8F5))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Box Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("files_search_input"),
                    placeholder = { Text("Search files by title or category...", color = Color(0xFF8E8E93), fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = RedPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear search",
                                    tint = Color(0xFF757575)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFFF2F2),
                        unfocusedContainerColor = Color(0xFFFFF5F5),
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = Color(0xFFF2E0DD)
                    )
                )
            }

            // Category Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterTabs = listOf("All", "Scanned", "Favorites")
                items(filterTabs) { tabName ->
                    val isSelected = selectedFilterTab == tabName
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) RedPrimary else Color(0xFFF0EAE1))
                            .clickable { selectedFilterTab = tabName }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tabName,
                            color = if (isSelected) Color.White else Color(0xFF4A4646),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // File List View
            if (displayedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEBEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = null,
                                tint = RedPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No documents found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No results matching \"$searchQuery\"" else "Import or scan documents to see them here.",
                            fontSize = 13.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedFiles, key = { it.id }) { pdf ->
                        FileItemCard(
                            pdf = pdf,
                            onClick = { onOpenPdf(pdf) },
                            onToggleFavorite = { viewModel.toggleFavorite(pdf.id, pdf.isFavorite) },
                            onRename = { pdfToRename = pdf },
                            onRemove = {
                                viewModel.deleteFile(pdf)
                                Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                            },
                            onSaveToDevice = {
                                val pdfFile = File(pdf.path)
                                if (pdfFile.exists()) {
                                    val saved = PdfEngine.savePdfToDownloads(context, pdfFile, pdf.title)
                                    if (saved) {
                                        Toast.makeText(context, "Saved to Downloads: ${pdf.title}", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Saved file to local storage", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItemCard(
    pdf: PdfEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    onSaveToDevice: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCardBgLight),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarmBorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PdfThumbnailView(
                pdfPath = pdf.path,
                pdfTitle = pdf.title,
                modifier = Modifier.size(46.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pdf.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1C1B1F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${pdf.pageCount} ${if (pdf.pageCount == 1) "page" else "pages"} • ${pdf.sizeFormatted}",
                        fontSize = 12.sp,
                        color = Color(0xFF605D62)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RedPrimary.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = pdf.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedPrimary
                        )
                    }
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (pdf.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Favorite",
                    tint = if (pdf.isFavorite) GoldStar else Color(0xFF605D62)
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More Options",
                        tint = Color(0xFF605D62)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename Document", fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Save to Local Device", fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onSaveToDevice()
                        },
                        leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Document", fontSize = 13.sp, color = RedPrimary) },
                        onClick = {
                            showMenu = false
                            onRemove()
                        },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }
    }
}
