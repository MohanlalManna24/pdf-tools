package com.example.ui.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeminiService
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.WarmBorderLight
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantBottomSheet(
    documentContextText: String = "",
    onDismiss: () -> Unit,
    onCoverGenerated: ((Bitmap) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedMode by remember { mutableStateOf("chat") } // "chat", "image"
    var modelSelection by remember { mutableStateOf("gemini-3.5-flash") } // "gemini-3.1-pro-preview", "gemini-3.5-flash", "gemini-3.1-flash-lite-preview"

    // Chat / Text state
    var userPrompt by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("") }
    var isGeneratingText by remember { mutableStateOf(false) }

    // Image Generator State
    var imagePrompt by remember { mutableStateOf("Modern professional cover page art for PDF report, minimalist style") }
    var selectedAspectRatio by remember { mutableStateOf("1:1") } // 1:1, 2:3, 3:2, 3:4, 4:3, 9:16, 16:9, 21:9
    var selectedImageSize by remember { mutableStateOf("1K") } // 1K, 2K, 4K
    var selectedImageModel by remember { mutableStateOf("gemini-3-pro-image-preview") } // "gemini-3-pro-image-preview", "gemini-3.1-flash-image-preview"
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingImage by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFAF8F5),
        dragHandle = null
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(RedPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Gemini AI Studio",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F)
                        )
                        Text(
                            text = "Intelligent document helper & creative engine",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Tab Selection (AI Text vs AI Image)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFEFECE6))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedMode == "chat") RedPrimary else Color.Transparent)
                        .clickable { selectedMode = "chat" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Psychology,
                            contentDescription = null,
                            tint = if (selectedMode == "chat") Color.White else Color.DarkGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Document AI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedMode == "chat") Color.White else Color.DarkGray
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedMode == "image") RedPrimary else Color.Transparent)
                        .clickable { selectedMode = "image" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            tint = if (selectedMode == "image") Color.White else Color.DarkGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Image Generator",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedMode == "image") Color.White else Color.DarkGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedMode == "chat") {
                // MODEL SELECTOR ROW
                Text("Select Gemini Intelligence Model", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1C1B1F))
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val models = listOf(
                        Triple("gemini-3.5-flash", "Gemini 3.5 Flash", "General Q&A & Summaries"),
                        Triple("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "Deep Reasoning & Contracts"),
                        Triple("gemini-3.1-flash-lite-preview", "Flash-Lite", "Instant Fast Actions")
                    )
                    items(models) { (modelId, name, desc) ->
                        val isSelected = modelSelection == modelId
                        Card(
                            modifier = Modifier
                                .width(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) RedPrimary else WarmBorderLight,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { modelSelection = modelId },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFFFEBEE) else Color.White
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isSelected) RedPrimary else Color.Black)
                                Text(desc, fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Preset Action Chips
                Text("Quick Action Presets", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1C1B1F))
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val presets = listOf(
                        "Summarize Document",
                        "Extract Key Action Items",
                        "Translate to Spanish",
                        "Translate to French",
                        "Check Formatting & Errors"
                    )
                    items(presets) { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .border(1.dp, RedPrimary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                .clickable {
                                    userPrompt = when (preset) {
                                        "Summarize Document" -> "Provide a comprehensive executive summary of this document."
                                        "Extract Key Action Items" -> "List all key action items, deadlines, and numerical figures in bullet points."
                                        "Translate to Spanish" -> "Translate the document content accurately into Spanish."
                                        "Translate to French" -> "Translate the document content accurately into French."
                                        else -> "Format and check this text for any grammatical or structure issues."
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(preset, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RedPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Prompt Input Box
                OutlinedTextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    placeholder = { Text("Ask Gemini anything about this document...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (userPrompt.isNotBlank() && !isGeneratingText) {
                                    isGeneratingText = true
                                    aiResponse = ""
                                    coroutineScope.launch {
                                        val sys = if (documentContextText.isNotBlank())
                                            "You are an expert AI document analyzer. Here is the document content to analyze:\n$documentContextText"
                                        else "You are an expert AI document analyzer assistant."

                                        val res = GeminiService.generateText(
                                            prompt = userPrompt,
                                            systemInstruction = sys,
                                            modelName = modelSelection
                                        )
                                        isGeneratingText = false
                                        res.fold(
                                            onSuccess = { aiResponse = it },
                                            onFailure = {
                                                aiResponse = "Error: ${it.localizedMessage}"
                                                Toast.makeText(context, "Gemini Error: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Filled.Send, contentDescription = "Send", tint = RedPrimary)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary)
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (isGeneratingText) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = RedPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Gemini is analyzing document...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RedPrimary)
                    }
                }

                if (aiResponse.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarmBorderLight))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gemini Output", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RedPrimary)
                                }

                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(aiResponse))
                                        Toast.makeText(context, "Copied response to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = WarmBorderLight)

                            Text(
                                text = aiResponse,
                                fontSize = 13.sp,
                                color = Color(0xFF1C1B1F),
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            } else {
                // AI IMAGE GENERATOR MODE
                Text("AI Cover Page & Asset Generator", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1C1B1F))
                Text("Create high resolution graphics & cover artwork using Gemini Image Preview", fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(12.dp))

                Text("Prompt Description", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = imagePrompt,
                    onValueChange = { imagePrompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Aspect Ratio Selector (Mandatory Affordance)
                Text("Aspect Ratio", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                val aspectRatios = listOf("1:1", "2:3", "3:2", "3:4", "4:3", "9:16", "16:9", "21:9")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(aspectRatios) { ratio ->
                        val isSel = selectedAspectRatio == ratio
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) RedPrimary else Color.White)
                                .border(1.dp, if (isSel) RedPrimary else WarmBorderLight, RoundedCornerShape(10.dp))
                                .clickable { selectedAspectRatio = ratio }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(ratio, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else Color.DarkGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Resolution / Size Selector (Mandatory Affordance)
                Text("Image Size", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1K", "2K", "4K").forEach { sz ->
                        val isSel = selectedImageSize == sz
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) RedPrimary else Color.White)
                                .border(1.dp, if (isSel) RedPrimary else WarmBorderLight, RoundedCornerShape(10.dp))
                                .clickable { selectedImageSize = sz }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(sz, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else Color.DarkGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Image Model Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "gemini-3-pro-image-preview" to "Studio Pro Quality",
                        "gemini-3.1-flash-image-preview" to "Flash Fast Render"
                    ).forEach { (mId, mLabel) ->
                        val isSel = selectedImageModel == mId
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) Color(0xFFFFEBEE) else Color.White)
                                .border(1.dp, if (isSel) RedPrimary else WarmBorderLight, RoundedCornerShape(10.dp))
                                .clickable { selectedImageModel = mId }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(mLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) RedPrimary else Color.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (imagePrompt.isNotBlank() && !isGeneratingImage) {
                            isGeneratingImage = true
                            coroutineScope.launch {
                                val res = GeminiService.generateImage(
                                    prompt = imagePrompt,
                                    aspectRatio = selectedAspectRatio,
                                    size = selectedImageSize,
                                    modelName = selectedImageModel
                                )
                                isGeneratingImage = false
                                res.fold(
                                    onSuccess = { generatedBitmap = it },
                                    onFailure = {
                                        Toast.makeText(context, "Image Generation Error: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    if (isGeneratingImage) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating Artwork...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GENERATE AI ARTWORK", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (generatedBitmap != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarmBorderLight))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Image(
                                bitmap = generatedBitmap!!.asImageBitmap(),
                                contentDescription = "Generated Cover",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = {
                                        generatedBitmap?.let { bmp ->
                                            onCoverGenerated?.invoke(bmp)
                                            Toast.makeText(context, "Applied AI Cover to Document!", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Apply as Cover", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
