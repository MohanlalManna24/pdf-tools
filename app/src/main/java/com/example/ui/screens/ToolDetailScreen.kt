package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AddMoreFilesCard
import com.example.ui.components.SelectedFileListItem
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.WarmBorderLight
import com.example.ui.theme.WarmCardBgLight
import com.example.util.PdfEngine

data class SelectedFileModel(
    val name: String,
    val sizeText: String,
    val pageCountText: String,
    val localPath: String,
    val uri: Uri? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolDetailScreen(
    toolId: String,
    onBack: () -> Unit,
    onExecuteTool: (titlesOrPaths: List<String>, extraParam: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val toolTitle = when(toolId) {
        "merge" -> "Merge PDF"
        "split" -> "Split PDF"
        "compress" -> "Compress PDF"
        "image_to_pdf" -> "Image to PDF"
        "pdf_to_image" -> "PDF to Image"
        "rotate" -> "Rotate PDF"
        "delete" -> "Delete Pages"
        "rearrange" -> "Rearrange Pages"
        "watermark" -> "Watermark PDF"
        "password" -> "Password Protection"
        else -> "PDF Tool"
    }

    val toolDescription = when(toolId) {
        "merge" -> "Combine multiple PDFs into one unified document. Drag and drop to reorder."
        "split" -> "Extract specific page ranges into standalone PDF files."
        "compress" -> "Reduce file size while preserving high visual document clarity."
        "image_to_pdf" -> "Convert gallery images or camera scans into a high quality PDF."
        "pdf_to_image" -> "Export each page of your PDF as high resolution PNG/JPEG images."
        "rotate" -> "Orient document pages by 90°, 180°, or 270° clockwise."
        "delete" -> "Select and permanently strip unnecessary pages from your file."
        "rearrange" -> "Drag tiles to re-order page sequence before saving."
        "watermark" -> "Overlay custom security text or stamp across all pages."
        "password" -> "Encrypt document with password protection or strip locks."
        else -> "Process your PDF files quickly and securely offline."
    }

    // Interactive files list - starts empty for real user files
    val selectedFiles = remember { mutableStateListOf<SelectedFileModel>() }

    // Document Picker Launcher
    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            val tempFile = PdfEngine.getFileFromUri(context, uri)
            if (tempFile != null) {
                val pages = PdfEngine.getPdfPageCount(tempFile)
                val sizeKb = (tempFile.length() / 1024).coerceAtLeast(1)
                val sizeFormatted = if (sizeKb > 1024) "${String.format("%.1f", sizeKb / 1024.0)} MB" else "$sizeKb KB"
                selectedFiles.add(
                    SelectedFileModel(
                        name = tempFile.name,
                        sizeText = sizeFormatted,
                        pageCountText = "$pages pages",
                        localPath = tempFile.absolutePath,
                        uri = uri
                    )
                )
            }
        }
    }

    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var passwordText by remember { mutableStateOf("") }
    var compressionPreset by remember { mutableStateOf("Recommended (Balanced)") }
    var rotationAngle by remember { mutableStateOf("90°") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFFAF8F5),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RedPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PDF Tools", fontWeight = FontWeight.Bold, color = RedPrimary, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("tool_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = RedPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { documentPicker.launch("application/pdf") }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search / Import",
                            tint = Color(0xFF1C1B1F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAF8F5))
            )
        },
        floatingActionButton = {
            Button(
                onClick = {
                    if (selectedFiles.isEmpty()) {
                        val targetMime = if (toolId == "image_to_pdf") "image/*" else "application/pdf"
                        documentPicker.launch(targetMime)
                    } else {
                        val filePaths = selectedFiles.map { it.localPath }
                        val param = when(toolId) {
                            "watermark" -> watermarkText
                            "password" -> passwordText
                            "compress" -> compressionPreset
                            "rotate" -> rotationAngle
                            else -> ""
                        }
                        onExecuteTool(filePaths, param)
                    }
                },
                modifier = Modifier
                    .padding(16.dp)
                    .height(54.dp)
                    .testTag("tool_action_btn"),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = when(toolId) {
                            "merge" -> Icons.Filled.MergeType
                            "compress" -> Icons.Filled.Compress
                            "rotate" -> Icons.Filled.RotateRight
                            "watermark" -> Icons.Filled.WaterDrop
                            "password" -> Icons.Filled.Lock
                            else -> Icons.Filled.Check
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when(toolId) {
                            "merge" -> "Merge Now"
                            "split" -> "Split PDF Now"
                            "compress" -> "Compress Now"
                            "image_to_pdf" -> "Convert to PDF"
                            "pdf_to_image" -> "Extract Images"
                            "rotate" -> "Rotate Pages"
                            "delete" -> "Delete Selected"
                            "watermark" -> "Apply Watermark"
                            "password" -> "Protect PDF"
                            else -> "Process Document"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title & Description
            item {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Text(
                        text = toolTitle,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1B1F)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = toolDescription,
                        fontSize = 14.sp,
                        color = Color(0xFF605D62),
                        lineHeight = 20.sp
                    )
                }
            }

            // Options depending on tool type
            if (toolId == "watermark") {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmCardBgLight),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarmBorderLight))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Watermark Text", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = watermarkText,
                                onValueChange = { watermarkText = it },
                                modifier = Modifier.fillMaxWidth().testTag("watermark_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary)
                            )
                        }
                    }
                }
            } else if (toolId == "password") {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmCardBgLight),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarmBorderLight))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Set Password", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = passwordText,
                                onValueChange = { passwordText = it },
                                modifier = Modifier.fillMaxWidth().testTag("password_input"),
                                placeholder = { Text("Enter secret password...") },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary)
                            )
                        }
                    }
                }
            } else if (toolId == "compress") {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmCardBgLight),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarmBorderLight))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Compression Level", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            listOf("Recommended (Balanced)", "High Compression (Small Size)", "Low Compression (Max Quality)").forEach { preset ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { compressionPreset = preset }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (compressionPreset == preset) RedPrimary else Color.Transparent)
                                            .border(2.dp, if (compressionPreset == preset) RedPrimary else Color.Gray, RoundedCornerShape(10.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(preset, fontSize = 14.sp, color = Color(0xFF1C1B1F))
                                }
                            }
                        }
                    }
                }
            } else if (toolId == "rotate") {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmCardBgLight),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarmBorderLight))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Rotation Angle", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf("90°", "180°", "270°").forEach { angle ->
                                    val isSelected = rotationAngle == angle
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) RedPrimary else Color.White)
                                            .border(1.dp, if (isSelected) RedPrimary else WarmBorderLight, RoundedCornerShape(12.dp))
                                            .clickable { rotationAngle = angle },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = angle,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF1C1B1F)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selected Files Container Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmCardBgLight),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarmBorderLight))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedFiles.size} Files Selected",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F)
                            )
                            Text(
                                text = "Clear All",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RedPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { selectedFiles.clear() }
                                    .padding(4.dp)
                            )
                        }

                        HorizontalDivider(color = WarmBorderLight, thickness = 1.dp)

                        selectedFiles.forEachIndexed { index, file ->
                            SelectedFileListItem(
                                pdfName = file.name,
                                sizeText = file.sizeText,
                                pageCountText = file.pageCountText,
                                onRemove = { selectedFiles.removeAt(index) }
                            )
                            if (index < selectedFiles.size - 1) {
                                HorizontalDivider(color = WarmBorderLight, thickness = 1.dp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // "+ ADD MORE FILES" Button
                        AddMoreFilesCard(
                            onClick = {
                                val targetMime = if (toolId == "image_to_pdf") "image/*" else "application/pdf"
                                documentPicker.launch(targetMime)
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
