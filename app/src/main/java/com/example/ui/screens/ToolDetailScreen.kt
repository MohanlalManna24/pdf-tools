package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PdfEntity
import com.example.ui.components.AddMoreFilesCard
import com.example.ui.components.SelectedFileListItem
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.WarmBorderLight
import com.example.ui.theme.WarmCardBgLight
import com.example.util.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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
    activePdf: PdfEntity? = null,
    allPdfs: List<PdfEntity> = emptyList(),
    onBack: () -> Unit,
    onExecuteTool: (titlesOrPaths: List<String>, extraParam: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Delegate "delete" and "rearrange" tools directly to PageManagerScreen
    if (toolId == "delete" || toolId == "rearrange") {
        val initialPath = activePdf?.path
        val initialTitle = activePdf?.title
        PageManagerScreen(
            toolMode = toolId,
            initialFilePath = initialPath,
            documentTitle = initialTitle,
            allPdfs = allPdfs,
            onBack = onBack,
            onSavePdf = { sourcePath, pageSequenceParam ->
                onExecuteTool(listOf(sourcePath), pageSequenceParam)
            },
            modifier = modifier
        )
        return
    }

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
        "split" -> "Extract specific pages or custom ranges into a new standalone PDF file."
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

    // Interactive files list
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

    // Tool specific params
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var passwordText by remember { mutableStateOf("") }
    var compressionPreset by remember { mutableStateOf("Recommended (Balanced)") }
    var rotationAngle by remember { mutableStateOf("90°") }

    // Split PDF States
    var splitMode by remember { mutableStateOf("visual") } // "visual", "range", "presets"
    var splitRangeInput by remember { mutableStateOf("1-2") }
    var saveAsSeparateFiles by remember { mutableStateOf(true) }
    var selectedPageIndices = remember { mutableStateListOf<Int>() }
    var totalLoadedPages by remember { mutableIntStateOf(1) }
    var loadedThumbnails by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    // Render thumbnails when selected file changes for Split PDF
    LaunchedEffect(selectedFiles.firstOrNull()?.localPath) {
        val currentFile = selectedFiles.firstOrNull()
        if (currentFile != null && toolId == "split") {
            val path = currentFile.localPath
            val count = withContext(Dispatchers.IO) {
                PdfEngine.getPdfPageCount(File(path))
            }
            totalLoadedPages = count

            val thumbs = withContext(Dispatchers.IO) {
                (0 until count).mapNotNull { p ->
                    PdfEngine.renderPageToBitmap(File(path), p, width = 240)
                }
            }
            loadedThumbnails = thumbs

            // Default select page 1 and 2 or all pages
            selectedPageIndices.clear()
            if (count == 1) {
                selectedPageIndices.add(1)
            } else {
                selectedPageIndices.add(1)
                selectedPageIndices.add(2)
            }
            splitRangeInput = if (count >= 2) "1-2" else "1"
        } else {
            totalLoadedPages = 1
            loadedThumbnails = emptyList()
            selectedPageIndices.clear()
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
                            "split" -> {
                                val rawParam = if (splitMode == "visual") {
                                    if (selectedPageIndices.isEmpty()) "1" else selectedPageIndices.sorted().joinToString(",")
                                } else if (splitMode == "range") {
                                    splitRangeInput
                                } else {
                                    "1-$totalLoadedPages"
                                }
                                if (saveAsSeparateFiles) "SEPARATE::$rawParam" else "COMBINED::$rawParam"
                            }
                            "delete" -> {
                                val remaining = (1..totalLoadedPages).filterNot { selectedPageIndices.contains(it) }
                                if (remaining.isEmpty()) "1" else remaining.joinToString(",")
                            }
                            "rearrange" -> {
                                if (selectedPageIndices.isEmpty()) (1..totalLoadedPages).joinToString(",")
                                else selectedPageIndices.joinToString(",")
                            }
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
                            "split" -> {
                                if (selectedFiles.isEmpty()) "Select PDF File"
                                else if (splitMode == "visual") "Split (${selectedPageIndices.size} Pages)"
                                else "Split PDF Now"
                            }
                            "merge" -> "Merge Now"
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

            // DEDICATED SPLIT / DELETE / REARRANGE PDF SECTION
            if (toolId == "split" || toolId == "delete" || toolId == "rearrange") {
                item {
                    if (selectedFiles.isEmpty()) {
                        // Dropzone Empty Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { documentPicker.launch("application/pdf") },
                            colors = CardDefaults.cardColors(containerColor = WarmCardBgLight),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarmBorderLight))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(RedPrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderOpen,
                                        contentDescription = null,
                                        tint = RedPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Select PDF Document to Split",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Color(0xFF1C1B1F)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap to pick a PDF file from device storage",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = { documentPicker.launch("application/pdf") },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Choose PDF File", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Selected File Card with Split Options
                        val activeFile = selectedFiles.first()
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = WarmCardBgLight),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarmBorderLight))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(RedPrimary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PictureAsPdf,
                                                contentDescription = null,
                                                tint = RedPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = activeFile.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF1C1B1F)
                                            )
                                            Text(
                                                text = "${activeFile.pageCountText} • ${activeFile.sizeText}",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    TextButton(onClick = {
                                        selectedFiles.clear()
                                        documentPicker.launch("application/pdf")
                                    }) {
                                        Text("Change", color = RedPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = WarmBorderLight)

                                Text("Split Method", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(10.dp))

                                // Mode Toggle Pills
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEFECE6))
                                        .padding(4.dp)
                                ) {
                                    listOf(
                                        "visual" to "Tap Pages",
                                        "range" to "Page Range",
                                        "presets" to "Rule Preset"
                                    ).forEach { (modeKey, modeLabel) ->
                                        val isSelected = splitMode == modeKey
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) RedPrimary else Color.Transparent)
                                                .clickable { splitMode = modeKey }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = modeLabel,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isSelected) Color.White else Color.DarkGray
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Save as Separate Files Switch
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, WarmBorderLight)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Save as Separate PDFs",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = if (saveAsSeparateFiles) "Extracts into individual standalone PDF files" else "Combines extracted pages into 1 single PDF file",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        Switch(
                                            checked = saveAsSeparateFiles,
                                            onCheckedChange = { saveAsSeparateFiles = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = RedPrimary
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Mode 1: Visual Tap Selection Grid
                                if (splitMode == "visual") {
                                    // Preset Selection Bar
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Selected ${selectedPageIndices.size} of $totalLoadedPages pages",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.DarkGray
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "All",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RedPrimary,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(RedPrimary.copy(alpha = 0.1f))
                                                    .clickable {
                                                        selectedPageIndices.clear()
                                                        for (p in 1..totalLoadedPages) selectedPageIndices.add(p)
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                            Text(
                                                text = "Odd",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RedPrimary,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(RedPrimary.copy(alpha = 0.1f))
                                                    .clickable {
                                                        selectedPageIndices.clear()
                                                        for (p in 1..totalLoadedPages step 2) selectedPageIndices.add(p)
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                            Text(
                                                text = "Even",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RedPrimary,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(RedPrimary.copy(alpha = 0.1f))
                                                    .clickable {
                                                        selectedPageIndices.clear()
                                                        for (p in 2..totalLoadedPages step 2) selectedPageIndices.add(p)
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                            Text(
                                                text = "Clear",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Gray,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.LightGray.copy(alpha = 0.3f))
                                                    .clickable { selectedPageIndices.clear() }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Page Thumbnails Grid Carousel
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        items(totalLoadedPages) { index ->
                                            val pageNum = index + 1
                                            val isSelected = selectedPageIndices.contains(pageNum)
                                            val bmp = loadedThumbnails.getOrNull(index)

                                            Card(
                                                modifier = Modifier
                                                    .width(110.dp)
                                                    .height(150.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) RedPrimary else WarmBorderLight,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable {
                                                        if (isSelected) {
                                                            selectedPageIndices.remove(pageNum)
                                                        } else {
                                                            selectedPageIndices.add(pageNum)
                                                        }
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = Color.White)
                                            ) {
                                                Box(modifier = Modifier.fillMaxSize()) {
                                                    if (bmp != null) {
                                                        Image(
                                                            bitmap = bmp.asImageBitmap(),
                                                            contentDescription = "Page $pageNum",
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(4.dp)
                                                        )
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(Color(0xFFF0EDED)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text("Page $pageNum", fontSize = 12.sp, color = Color.Gray)
                                                        }
                                                    }

                                                    // Page Badge
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(6.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color.Black.copy(alpha = 0.7f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            .align(Alignment.TopStart)
                                                    ) {
                                                        Text("P.$pageNum", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    // Checkbox Icon
                                                    Icon(
                                                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                                        contentDescription = null,
                                                        tint = if (isSelected) RedPrimary else Color.Gray,
                                                        modifier = Modifier
                                                            .padding(6.dp)
                                                            .size(20.dp)
                                                            .align(Alignment.TopEnd)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else if (splitMode == "range") {
                                    // Mode 2: Page Range Input
                                    Text("Enter Page Range", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = splitRangeInput,
                                        onValueChange = { splitRangeInput = it },
                                        placeholder = { Text("e.g. 1-3, 5, 7-9") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Example: '1-3' extracts pages 1 to 3. '1, 4, 6' extracts specific pages.",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                } else {
                                    // Mode 3: Presets
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(
                                            "Odd Pages" to "Extract pages 1, 3, 5...",
                                            "Even Pages" to "Extract pages 2, 4, 6...",
                                            "All Pages" to "Extract all pages individually"
                                        ).forEach { (title, desc) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color.White)
                                                    .border(1.dp, WarmBorderLight, RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        if (title == "Odd Pages") {
                                                            selectedPageIndices.clear()
                                                            for (p in 1..totalLoadedPages step 2) selectedPageIndices.add(p)
                                                            splitMode = "visual"
                                                        } else if (title == "Even Pages") {
                                                            selectedPageIndices.clear()
                                                            for (p in 2..totalLoadedPages step 2) selectedPageIndices.add(p)
                                                            splitMode = "visual"
                                                        } else {
                                                            selectedPageIndices.clear()
                                                            for (p in 1..totalLoadedPages) selectedPageIndices.add(p)
                                                            splitMode = "visual"
                                                        }
                                                    }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(desc, fontSize = 11.sp, color = Color.Gray)
                                                }
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    tint = RedPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (toolId == "watermark") {
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

            // Selected Files Container Card (for tools other than Split, Delete, Rearrange)
            if (toolId != "split" && toolId != "delete" && toolId != "rearrange") {
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
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
