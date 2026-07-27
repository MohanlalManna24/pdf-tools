package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.WarmBorderLight
import com.example.ui.theme.WarmCardBgLight
import com.example.util.PdfEngine
import com.example.util.ScanFilterType
import com.example.util.ScanImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScanScreen(
    initialPages: List<Bitmap>,
    onBackToScan: (List<Bitmap>) -> Unit,
    onDone: (List<Bitmap>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pages = remember { mutableStateListOf<Bitmap>(*initialPages.toTypedArray()) }
    var activePageIndex by remember { mutableIntStateOf(0) }
    var applyToAllPages by remember { mutableStateOf(false) }

    // Editable Document Title State
    var documentTitle by remember { mutableStateOf("Scan_${java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())}.pdf") }
    var isRenameDialogOpen by remember { mutableStateOf(false) }

    // Per page brightness values (-100f to +100f)
    val pageBrightness = remember { mutableStateMapOf<Int, Float>() }
    // Per page filters
    val pageFilters = remember { mutableStateMapOf<Int, ScanFilterType>() }

    // Bottom sheet dialog states ("BRIGHTNESS", "FILTERS", "CROP", "MARKUP")
    var activeSheet by remember { mutableStateOf<String?>(null) }

    if (pages.isEmpty()) {
        pages.add(ScanImageProcessor.createSampleScanBitmap(1))
    }

    val currentRawBitmap = pages.getOrElse(activePageIndex) { pages.first() }
    val currentBrightness = pageBrightness[activePageIndex] ?: 0f
    val currentFilter = pageFilters[activePageIndex] ?: ScanFilterType.ORIGINAL

    // Processed bitmap preview rendered off-thread for maximum smoothness
    val currentProcessedBitmap by produceState(
        initialValue = currentRawBitmap,
        key1 = currentRawBitmap,
        key2 = currentBrightness,
        key3 = currentFilter
    ) {
        value = withContext(Dispatchers.Default) {
            var result = ScanImageProcessor.applyBrightness(currentRawBitmap, currentBrightness)
            result = ScanImageProcessor.applyFilter(result, currentFilter)
            result
        }
    }

    Scaffold(
        containerColor = Color(0xFFFAF8F5),
        topBar = {
            // --- TOP HEADER BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 10.dp, start = 8.dp, end = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Back button + Clickable File Name Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    IconButton(
                        onClick = { onBackToScan(pages.toList()) },
                        modifier = Modifier.testTag("edit_scan_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = RedPrimary
                        )
                    }

                    // Touch to Rename Document File Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isRenameDialogOpen = true }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = documentTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F),
                            maxLines = 1,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Rename File",
                            tint = RedPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Right Controls: Go Back (Undo) + Go Forward (Redo) + Counter + Save
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Goto Back (Previous Page) Icon
                    IconButton(
                        onClick = { if (activePageIndex > 0) activePageIndex-- },
                        enabled = activePageIndex > 0
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Go Back / Undo",
                            tint = if (activePageIndex > 0) RedPrimary else Color(0xFFC4C4C4)
                        )
                    }

                    // Goto Forward (Next Page) Icon
                    IconButton(
                        onClick = { if (activePageIndex < pages.lastIndex) activePageIndex++ },
                        enabled = activePageIndex < pages.lastIndex
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Go Forward / Redo",
                            tint = if (activePageIndex < pages.lastIndex) RedPrimary else Color(0xFFC4C4C4)
                        )
                    }

                    // Page Counter Pill Badge ("1 / 4")
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3EDF7), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${activePageIndex + 1}/${pages.size}",
                            color = Color(0xFF49454F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Save Red Capsule Button
                    Button(
                        onClick = {
                            val finalBitmaps = pages.mapIndexed { idx, raw ->
                                val b = if (applyToAllPages) currentBrightness else (pageBrightness[idx] ?: 0f)
                                val f = if (applyToAllPages) currentFilter else (pageFilters[idx] ?: ScanFilterType.ORIGINAL)
                                var processed = ScanImageProcessor.applyBrightness(raw, b)
                                processed = ScanImageProcessor.applyFilter(processed, f)
                                processed
                            }
                            onDone(finalBitmaps)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("edit_scan_done_button")
                    ) {
                        Text(
                            text = "Save",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
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
            // --- 1. MAIN DOCUMENT PREVIEW CANVAS WITH RED CORNER GUIDES & DASHED GRID ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = currentProcessedBitmap.asImageBitmap(),
                        contentDescription = "Active Scan Page Preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )

                    // Dashed Crop Grid Overlay
                    DashedCropGridOverlay()

                    // Red Corner Brackets
                    RedCornerBrackets(modifier = Modifier.padding(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- 2. "APPLY TO ALL PAGES" CARD (POSITIONED BELOW THE DOCUMENT PREVIEW) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEAEA)),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarmBorderLight),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(RedPrimary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Layers,
                                contentDescription = "Sync",
                                tint = RedPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "Apply to All Pages",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F)
                        )
                    }

                    Switch(
                        checked = applyToAllPages,
                        onCheckedChange = { applyToAllPages = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RedPrimary,
                            uncheckedThumbColor = Color(0xFF79747E),
                            uncheckedTrackColor = Color(0xFFE7E0EC)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // --- 3. PAGE THUMBNAIL CAROUSEL (HORIZONTAL STRIP) ---
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(pages) { index, pageBmp ->
                    val isSelected = index == activePageIndex
                    val b = if (applyToAllPages) currentBrightness else (pageBrightness[index] ?: 0f)
                    val f = if (applyToAllPages) currentFilter else (pageFilters[index] ?: ScanFilterType.ORIGINAL)
                    val thumbBmp = remember(pageBmp, b, f) {
                        var res = ScanImageProcessor.applyBrightness(pageBmp, b)
                        res = ScanImageProcessor.applyFilter(res, f)
                        res
                    }

                    Box(
                        modifier = Modifier
                            .width(62.dp)
                            .height(82.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) RedPrimary else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { activePageIndex = index }
                    ) {
                        Image(
                            bitmap = thumbBmp.asImageBitmap(),
                            contentDescription = "Thumbnail ${index + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Dark bottom bar page number badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .align(Alignment.BottomCenter)
                                .background(if (isSelected) RedPrimary else Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // End Item: "+ ADD" Button -> Re-opens ScannerScreen camera page to take more scans!
                item {
                    Box(
                        modifier = Modifier
                            .width(62.dp)
                            .height(82.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .drawDashedBorder(color = Color(0xFFBDBDBD), strokeWidth = 2.dp, radius = 10.dp)
                            .clickable { onBackToScan(pages.toList()) }
                            .testTag("add_page_thumbnail_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = "Add Page",
                                tint = Color(0xFF757575),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ADD",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF757575)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- 4. BOTTOM TOOLBAR (ROTATE, CROP, MARKUP, BRIGHTNESS, FILTERS, DELETE) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Rotate Tool
                ToolIconButton(
                    icon = Icons.Filled.RotateRight,
                    label = "Rotate",
                    onClick = {
                        pages[activePageIndex] = ScanImageProcessor.rotateBitmap(pages[activePageIndex], 90f)
                    }
                )

                // 2. Crop Tool
                ToolIconButton(
                    icon = Icons.Outlined.Crop,
                    label = "Crop",
                    onClick = { activeSheet = "CROP" }
                )

                // 3. Markup Tool
                ToolIconButton(
                    icon = Icons.Outlined.Edit,
                    label = "Markup",
                    onClick = { activeSheet = "MARKUP" }
                )

                // 4. Brightness Tool
                ToolIconButton(
                    icon = Icons.Outlined.WbSunny,
                    label = "Brightness",
                    onClick = { activeSheet = "BRIGHTNESS" }
                )

                // 5. Filters Tool
                ToolIconButton(
                    icon = Icons.Outlined.FilterList,
                    label = "Filter",
                    onClick = { activeSheet = "FILTERS" }
                )

                // 6. Delete Tool
                ToolIconButton(
                    icon = Icons.Outlined.Delete,
                    label = "Delete",
                    onClick = {
                        if (pages.size > 1) {
                            pages.removeAt(activePageIndex)
                            if (activePageIndex >= pages.size) {
                                activePageIndex = pages.lastIndex
                            }
                        } else {
                            onBackToScan(emptyList())
                        }
                    }
                )
            }
        }

        // --- BOTTOM SHEETS FOR TOOLS ---
        if (activeSheet == "CROP") {
            ModalBottomSheet(
                onDismissRequest = { activeSheet = null }
            ) {
                var hInset by remember { mutableFloatStateOf(0.05f) }
                var vInset by remember { mutableFloatStateOf(0.05f) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Smooth Document Crop",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1B1F)
                    )
                    Text(
                        text = "Adjust margin crop sliders or tap a preset",
                        fontSize = 12.sp,
                        color = Color(0xFF49454F)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Preset Crop Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { hInset = 0.05f; vInset = 0.05f },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("5% Auto", color = RedPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { hInset = 0.08f; vInset = 0.12f },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("A4 Ratio", color = RedPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { hInset = 0.12f; vInset = 0.12f },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("1:1 Square", color = RedPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Horizontal Margin: ${(hInset * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = hInset,
                        onValueChange = { hInset = it },
                        valueRange = 0f..0.30f,
                        colors = SliderDefaults.colors(thumbColor = RedPrimary, activeTrackColor = RedPrimary)
                    )

                    Text("Vertical Margin: ${(vInset * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = vInset,
                        onValueChange = { vInset = it },
                        valueRange = 0f..0.30f,
                        colors = SliderDefaults.colors(thumbColor = RedPrimary, activeTrackColor = RedPrimary)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            pages[activePageIndex] = ScanImageProcessor.cropBitmap(
                                source = pages[activePageIndex],
                                leftPercent = hInset,
                                topPercent = vInset,
                                rightPercent = hInset,
                                bottomPercent = vInset
                            )
                            activeSheet = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                    ) {
                        Text("Apply Smooth Crop", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (activeSheet == "MARKUP") {
            ModalBottomSheet(
                onDismissRequest = { activeSheet = null }
            ) {
                var selectedStamp by remember { mutableStateOf("APPROVED") }
                var selectedColor by remember { mutableStateOf("#DC2626") }

                val stamps = listOf(
                    "APPROVED" to "#16A34A",
                    "CONFIDENTIAL" to "#DC2626",
                    "SIGNED" to "#2563EB",
                    "NOTED" to "#D97706"
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Document Markup & Stamps",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1B1F)
                    )
                    Text(
                        text = "Select an official stamp to overlay on this page",
                        fontSize = 12.sp,
                        color = Color(0xFF49454F)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    stamps.forEach { (stamp, colorHex) ->
                        val isSelected = selectedStamp == stamp
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFFFFEAEA) else Color.Transparent)
                                .clickable {
                                    selectedStamp = stamp
                                    selectedColor = colorHex
                                }
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color(android.graphics.Color.parseColor(colorHex)), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stamp,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) RedPrimary else Color(0xFF1C1B1F)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = RedPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            pages[activePageIndex] = ScanImageProcessor.applyMarkupStamp(
                                source = pages[activePageIndex],
                                stampText = selectedStamp,
                                colorHex = selectedColor
                            )
                            activeSheet = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                    ) {
                        Text("Apply Stamp Markup", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- BOTTOM SHEETS FOR TOOLS ---
        if (activeSheet == "BRIGHTNESS") {
            ModalBottomSheet(
                onDismissRequest = { activeSheet = null }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Adjust Brightness",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1B1F)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    var sliderVal by remember { mutableFloatStateOf(currentBrightness) }

                    Slider(
                        value = sliderVal,
                        onValueChange = {
                            sliderVal = it
                            if (applyToAllPages) {
                                pages.indices.forEach { idx -> pageBrightness[idx] = it }
                            } else {
                                pageBrightness[activePageIndex] = it
                            }
                        },
                        valueRange = -80f..80f,
                        colors = SliderDefaults.colors(
                            thumbColor = RedPrimary,
                            activeTrackColor = RedPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { activeSheet = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                    ) {
                        Text("Apply Brightness", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (activeSheet == "FILTERS") {
            ModalBottomSheet(
                onDismissRequest = { activeSheet = null }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Document Filters",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1B1F)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val filterOptions = listOf(
                        ScanFilterType.ORIGINAL to "Original",
                        ScanFilterType.MAGIC_COLOR to "Magic Color",
                        ScanFilterType.GRAYSCALE to "Grayscale",
                        ScanFilterType.BLACK_WHITE to "B&W Document"
                    )

                    filterOptions.forEach { (type, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (applyToAllPages) {
                                        pages.indices.forEach { idx -> pageFilters[idx] = type }
                                    } else {
                                        pageFilters[activePageIndex] = type
                                    }
                                    activeSheet = null
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                fontSize = 16.sp,
                                fontWeight = if (currentFilter == type) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentFilter == type) RedPrimary else Color(0xFF1C1B1F)
                            )
                            if (currentFilter == type) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = RedPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- RENAME DOCUMENT DIALOG ---
        if (isRenameDialogOpen) {
            var tempName by remember { mutableStateOf(documentTitle) }
            AlertDialog(
                onDismissRequest = { isRenameDialogOpen = false },
                title = {
                    Text(
                        text = "Rename Document",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter a new name for your scanned document:",
                            fontSize = 13.sp,
                            color = Color(0xFF49454F)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            singleLine = true,
                            label = { Text("File Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (tempName.isNotBlank()) {
                                documentTitle = if (tempName.endsWith(".pdf", ignoreCase = true)) tempName else "$tempName.pdf"
                            }
                            isRenameDialogOpen = false
                        }
                    ) {
                        Text("Save", color = RedPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isRenameDialogOpen = false }) {
                        Text("Cancel", color = Color(0xFF757575))
                    }
                }
            )
        }
    }
}

@Composable
fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("tool_button_${label.lowercase()}")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF49454F),
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF49454F)
        )
    }
}

@Composable
fun DashedCropGridOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                val stroke = Stroke(width = 2.dp.toPx(), pathEffect = pathEffect)
                val color = RedPrimary.copy(alpha = 0.7f)

                // Outer crop dashed box
                drawRect(color = color, style = stroke)

                // Vertical 3-grid lines
                drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width / 3f, 0f), end = androidx.compose.ui.geometry.Offset(size.width / 3f, size.height), strokeWidth = 1.dp.toPx(), pathEffect = pathEffect)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(2f * size.width / 3f, 0f), end = androidx.compose.ui.geometry.Offset(2f * size.width / 3f, size.height), strokeWidth = 1.dp.toPx(), pathEffect = pathEffect)

                // Horizontal 3-grid lines
                drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, size.height / 3f), end = androidx.compose.ui.geometry.Offset(size.width, size.height / 3f), strokeWidth = 1.dp.toPx(), pathEffect = pathEffect)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, 2f * size.height / 3f), end = androidx.compose.ui.geometry.Offset(size.width, 2f * size.height / 3f), strokeWidth = 1.dp.toPx(), pathEffect = pathEffect)
            }
    )
}

fun Modifier.drawDashedBorder(color: Color, strokeWidth: androidx.compose.ui.unit.Dp, radius: androidx.compose.ui.unit.Dp) = this.drawWithContent {
    drawContent()
    val strokePx = strokeWidth.toPx()
    val radiusPx = radius.toPx()
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    drawRoundRect(
        color = color,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx, radiusPx),
        style = Stroke(width = strokePx, pathEffect = pathEffect)
    )
}

@Composable
fun RedCornerBrackets(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 5.dp
        val bracketSize = 28.dp
        val redColor = RedPrimary

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
                    .background(redColor)
            )
            Box(
                modifier = Modifier
                    .width(strokeWidth)
                    .fillMaxHeight()
                    .background(redColor)
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
                    .background(redColor)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(strokeWidth)
                    .fillMaxHeight()
                    .background(redColor)
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
                    .background(redColor)
            )
            Box(
                modifier = Modifier
                    .width(strokeWidth)
                    .fillMaxHeight()
                    .background(redColor)
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
                    .background(redColor)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(strokeWidth)
                    .fillMaxHeight()
                    .background(redColor)
            )
        }
    }
}

