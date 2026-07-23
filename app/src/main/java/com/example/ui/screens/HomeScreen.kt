package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReadMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PdfEntity
import com.example.ui.components.EssentialToolCard
import com.example.ui.components.FavoriteFileRow
import com.example.ui.components.RecentFileCard
import com.example.ui.theme.ChipAmberBg
import com.example.ui.theme.ChipAmberIcon
import com.example.ui.theme.ChipBlueBg
import com.example.ui.theme.ChipBlueIcon
import com.example.ui.theme.ChipCyanBg
import com.example.ui.theme.ChipCyanIcon
import com.example.ui.theme.ChipDeepRedBg
import com.example.ui.theme.ChipDeepRedIcon
import com.example.ui.theme.ChipGreenBg
import com.example.ui.theme.ChipGreenIcon
import com.example.ui.theme.ChipOrangeBg
import com.example.ui.theme.ChipOrangeIcon
import com.example.ui.theme.ChipPinkBg
import com.example.ui.theme.ChipPinkIcon
import com.example.ui.theme.ChipPurpleBg
import com.example.ui.theme.ChipPurpleIcon
import com.example.ui.theme.RedPrimary
import com.example.ui.viewmodel.MainViewModel

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.util.PdfEngine
import java.io.File

private data class ToolItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val bgColor: Color,
    val iconTint: Color,
    val id: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onSelectTool: (String) -> Unit,
    onOpenPdf: (PdfEntity) -> Unit,
    onViewAllRecent: () -> Unit,
    onOpenProfile: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val recentFiles by viewModel.recentFiles.collectAsState()
    val favoriteFiles by viewModel.favoriteFiles.collectAsState()
    val allFiles by viewModel.allFiles.collectAsState()

    val allHomeTools = remember {
        listOf(
            ToolItem("Merge PDF", Icons.Filled.MergeType, ChipPinkBg, ChipPinkIcon, "merge"),
            ToolItem("Split PDF", Icons.Filled.CallSplit, ChipCyanBg, ChipCyanIcon, "split"),
            ToolItem("Compress PDF", Icons.Filled.Compress, ChipGreenBg, ChipGreenIcon, "compress"),
            ToolItem("Image to PDF", Icons.Filled.Image, ChipOrangeBg, ChipOrangeIcon, "image_to_pdf"),
            ToolItem("PDF to Image", Icons.Filled.PictureAsPdf, ChipAmberBg, ChipAmberIcon, "pdf_to_image"),
            ToolItem("PDF Reader", Icons.Filled.ReadMore, ChipBlueBg, ChipBlueIcon, "reader"),
            ToolItem("Scanner", Icons.Filled.DocumentScanner, ChipPurpleBg, ChipPurpleIcon, "scanner"),
            ToolItem("OCR", Icons.Filled.AutoAwesome, ChipDeepRedBg, ChipDeepRedIcon, "ocr")
        )
    }

    val isSearchActive = searchQuery.isNotBlank()
    val matchingTools = remember(searchQuery, allHomeTools) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim()
            allHomeTools.filter {
                it.title.contains(q, ignoreCase = true) || it.id.contains(q, ignoreCase = true)
            }
        }
    }

    val matchingFiles = remember(searchQuery, allFiles) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim()
            allFiles.filter {
                it.title.contains(q, ignoreCase = true) ||
                it.category.contains(q, ignoreCase = true) ||
                (it.extractedText?.contains(q, ignoreCase = true) == true)
            }
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
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "PDF Tools",
                            fontWeight = FontWeight.Bold,
                            color = RedPrimary,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onOpenProfile?.invoke() ?: onViewAllRecent() },
                        modifier = Modifier.testTag("home_profile_icon_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Profile",
                            tint = RedPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAF8F5))
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Search Bar Input
            item {
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
                            .testTag("home_search_input"),
                        placeholder = { Text("Search PDF files & tools...", color = Color(0xFF8E8E93), fontSize = 14.sp) },
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
            }

            if (isSearchActive) {
                // Search Results View
                if (matchingTools.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(
                                text = "Matching Tools (${matchingTools.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            for (i in matchingTools.indices step 2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val t1 = matchingTools[i]
                                    EssentialToolCard(
                                        title = t1.title,
                                        icon = t1.icon,
                                        iconBgColor = t1.bgColor,
                                        iconTint = t1.iconTint,
                                        onClick = { onSelectTool(t1.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (i + 1 < matchingTools.size) {
                                        val t2 = matchingTools[i + 1]
                                        EssentialToolCard(
                                            title = t2.title,
                                            icon = t2.icon,
                                            iconBgColor = t2.bgColor,
                                            iconTint = t2.iconTint,
                                            onClick = { onSelectTool(t2.id) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                if (matchingFiles.isNotEmpty()) {
                    item {
                        Text(
                            text = "Matching Documents (${matchingFiles.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    items(items = matchingFiles, key = { doc -> "search_file_${doc.id}" }) { doc ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            FavoriteFileRow(
                                pdf = doc,
                                onClick = { onOpenPdf(doc) },
                                onToggleFavorite = { viewModel.toggleFavorite(doc.id, doc.isFavorite) },
                                onRemove = {
                                    viewModel.deleteFile(doc)
                                    Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                                },
                                onSaveToDevice = {
                                    val pdfFile = File(doc.path)
                                    if (pdfFile.exists()) {
                                        val saved = PdfEngine.savePdfToDownloads(context, pdfFile, doc.title)
                                        if (saved) {
                                            Toast.makeText(context, "Saved to Downloads: ${doc.title}", Toast.LENGTH_LONG).show()
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

                if (matchingTools.isEmpty() && matchingFiles.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F6)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0E0DC))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = RedPrimary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No tools or files match \"$searchQuery\"",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1C1B1F)
                                )
                            }
                        }
                    }
                }
            } else {
                // Standard Home View
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFD31A28),
                                        Color(0xFFE53935),
                                        Color(0xFFC62828)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Shield,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Offline PDF Studio",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "100% Secure & Private Local Processing",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onSelectTool("reader") },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = RedPrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                OutlinedButton(
                                    onClick = { onSelectTool("scanner") },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DocumentScanner,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scan Doc", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Recent Files Section
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Files",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F)
                        )
                        Text(
                            text = "View all",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RedPrimary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onViewAllRecent() }
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (recentFiles.isEmpty()) {
                        androidx.compose.material3.Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFFFFF8F6)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0E0DC))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PictureAsPdf,
                                    contentDescription = null,
                                    tint = RedPrimary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No documents imported yet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1C1B1F)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Use PDF Tools below to scan, merge, or convert files",
                                    fontSize = 12.sp,
                                    color = Color(0xFF757575)
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentFiles) { file ->
                                RecentFileCard(
                                    pdf = file,
                                    onClick = { onOpenPdf(file) },
                                    onRemove = {
                                        viewModel.deleteFile(file)
                                        Toast.makeText(context, "Removed from recents", Toast.LENGTH_SHORT).show()
                                    },
                                    onSaveToDevice = {
                                        val pdfFile = File(file.path)
                                        if (pdfFile.exists()) {
                                            val saved = PdfEngine.savePdfToDownloads(context, pdfFile, file.title)
                                            if (saved) {
                                                Toast.makeText(context, "Saved to Downloads: ${file.title}", Toast.LENGTH_LONG).show()
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

            // Essential Tools Section
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)) {
                    Text(
                        text = "Essential Tools",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1B1F),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 2-Column Tool Grid
                    val tools = listOf(
                        ToolItem("Merge PDF", Icons.Filled.MergeType, ChipPinkBg, ChipPinkIcon, "merge"),
                        ToolItem("Split PDF", Icons.Filled.CallSplit, ChipCyanBg, ChipCyanIcon, "split"),
                        ToolItem("Compress PDF", Icons.Filled.Compress, ChipGreenBg, ChipGreenIcon, "compress"),
                        ToolItem("Image to PDF", Icons.Filled.Image, ChipOrangeBg, ChipOrangeIcon, "image_to_pdf"),
                        ToolItem("PDF to Image", Icons.Filled.PictureAsPdf, ChipAmberBg, ChipAmberIcon, "pdf_to_image"),
                        ToolItem("PDF Reader", Icons.Filled.ReadMore, ChipBlueBg, ChipBlueIcon, "reader"),
                        ToolItem("Scanner", Icons.Filled.DocumentScanner, ChipPurpleBg, ChipPurpleIcon, "scanner"),
                        ToolItem("OCR", Icons.Filled.AutoAwesome, ChipDeepRedBg, ChipDeepRedIcon, "ocr")
                    )

                    for (i in tools.indices step 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val tool1 = tools[i]
                            EssentialToolCard(
                                title = tool1.title,
                                icon = tool1.icon,
                                iconBgColor = tool1.bgColor,
                                iconTint = tool1.iconTint,
                                onClick = { onSelectTool(tool1.id) },
                                modifier = Modifier.weight(1f)
                            )

                            if (i + 1 < tools.size) {
                                val tool2 = tools[i + 1]
                                EssentialToolCard(
                                    title = tool2.title,
                                    icon = tool2.icon,
                                    iconBgColor = tool2.bgColor,
                                    iconTint = tool2.iconTint,
                                    onClick = { onSelectTool(tool2.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Favorite Files Section
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(
                        text = "Favorite Files",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1B1F),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    if (favoriteFiles.isEmpty()) {
                        Text(
                            text = "No favorite files starred yet",
                            fontSize = 13.sp,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            }

            items(favoriteFiles) { fav ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    FavoriteFileRow(
                        pdf = fav,
                        onClick = { onOpenPdf(fav) },
                        onToggleFavorite = { viewModel.toggleFavorite(fav.id, fav.isFavorite) },
                        onRemove = {
                            viewModel.deleteFile(fav)
                            Toast.makeText(context, "Removed from recents", Toast.LENGTH_SHORT).show()
                        },
                        onSaveToDevice = {
                            val pdfFile = File(fav.path)
                            if (pdfFile.exists()) {
                                val saved = PdfEngine.savePdfToDownloads(context, pdfFile, fav.title)
                                if (saved) {
                                    Toast.makeText(context, "Saved to Downloads: ${fav.title}", Toast.LENGTH_LONG).show()
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
