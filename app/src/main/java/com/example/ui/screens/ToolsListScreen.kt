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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReadMore
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.WarmBorderLight
import com.example.ui.theme.WarmCardBgLight

data class FullToolItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val bgColor: Color,
    val iconTint: Color,
    val id: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsListScreen(
    onSelectTool: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allTools = listOf(
        FullToolItem("Merge PDF", "Combine multiple PDFs into one", Icons.Filled.MergeType, ChipPinkBg, ChipPinkIcon, "merge"),
        FullToolItem("Split PDF", "Extract pages into new files", Icons.Filled.CallSplit, ChipCyanBg, ChipCyanIcon, "split"),
        FullToolItem("Compress PDF", "Reduce document file size", Icons.Filled.Compress, ChipGreenBg, ChipGreenIcon, "compress"),
        FullToolItem("Image to PDF", "Convert photos into PDF", Icons.Filled.Image, ChipOrangeBg, ChipOrangeIcon, "image_to_pdf"),
        FullToolItem("PDF to Image", "Export pages as PNG/JPG", Icons.Filled.PictureAsPdf, ChipAmberBg, ChipAmberIcon, "pdf_to_image"),
        FullToolItem("PDF Reader", "Read & annotate documents", Icons.Filled.ReadMore, ChipBlueBg, ChipBlueIcon, "reader"),
        FullToolItem("Rotate PDF", "Turn pages clockwise", Icons.Filled.RotateRight, Color(0xFFEFEBE9), Color(0xFF5D4037), "rotate"),
        FullToolItem("Delete Pages", "Remove unwanted pages", Icons.Filled.Delete, Color(0xFFFFEBEE), Color(0xFFC62828), "delete"),
        FullToolItem("Rearrange Pages", "Reorder page structure", Icons.Filled.Reorder, Color(0xFFE8EAF6), Color(0xFF283593), "rearrange"),
        FullToolItem("Watermark", "Add confidential text mark", Icons.Filled.WaterDrop, Color(0xFFFFF3E0), Color(0xFFE65100), "watermark"),
        FullToolItem("Password Protect", "Encrypt or lock files", Icons.Filled.Lock, Color(0xFFECEFF1), Color(0xFF37474F), "password"),
        FullToolItem("Scanner", "Scan paper docs with camera", Icons.Filled.DocumentScanner, ChipPurpleBg, ChipPurpleIcon, "scanner"),
        FullToolItem("OCR Extractor", "Recognize text from images", Icons.Filled.AutoAwesome, ChipDeepRedBg, ChipDeepRedIcon, "ocr")
    )

    val categories = listOf("All", "Convert & Edit", "Organize", "Security & AI")
    var selectedCategory by remember { mutableStateOf("All") }
    var toolSearchQuery by remember { mutableStateOf("") }

    val filteredTools = remember(selectedCategory, toolSearchQuery) {
        val categoryFiltered = when (selectedCategory) {
            "Convert & Edit" -> allTools.filter { it.id in listOf("merge", "split", "compress", "image_to_pdf", "pdf_to_image", "rotate") }
            "Organize" -> allTools.filter { it.id in listOf("reader", "delete", "rearrange", "scanner") }
            "Security & AI" -> allTools.filter { it.id in listOf("watermark", "password", "ocr") }
            else -> allTools
        }
        if (toolSearchQuery.isBlank()) {
            categoryFiltered
        } else {
            val q = toolSearchQuery.trim()
            categoryFiltered.filter {
                it.title.contains(q, ignoreCase = true) ||
                it.description.contains(q, ignoreCase = true) ||
                it.id.contains(q, ignoreCase = true)
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
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
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
                        Text("PDF Toolset", fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F), fontSize = 20.sp)
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
            // Search Input Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = toolSearchQuery,
                    onValueChange = { toolSearchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tools_search_input"),
                    placeholder = { Text("Search tools (e.g. merge, compress, scan)...", color = Color(0xFF8E8E93), fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = RedPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailingIcon = {
                        if (toolSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { toolSearchQuery = "" }) {
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

            // Category Filter Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) RedPrimary else Color(0xFFFFEBEE))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else RedPrimary
                        )
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTools) { tool ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("tools_list_${tool.id}")
                            .clickable { onSelectTool(tool.id) },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmCardBgLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarmBorderLight))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(tool.bgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tool.icon,
                                    contentDescription = tool.title,
                                    tint = tool.iconTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = tool.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1B1F)
                                )
                                Text(
                                    text = tool.description,
                                    fontSize = 11.sp,
                                    color = Color(0xFF605D62),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
