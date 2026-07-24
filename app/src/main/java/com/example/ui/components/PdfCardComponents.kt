package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReadMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PdfEntity
import com.example.ui.theme.GoldStar
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.WarmBorderLight
import com.example.ui.theme.WarmCardBgLight
import com.example.util.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Renders an actual PDF page preview thumbnail or realistic document preview.
 */
@Composable
fun PdfThumbnailView(
    pdfPath: String,
    pdfTitle: String = "PDF Document",
    modifier: Modifier = Modifier
) {
    var thumbnailBitmap by remember(pdfPath, pdfTitle) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pdfPath, pdfTitle) {
        thumbnailBitmap = withContext(Dispatchers.IO) {
            val file = File(pdfPath)
            if (file.exists() && file.length() > 0) {
                PdfEngine.renderPageToBitmap(file, pageIndex = 0, width = 300)
            } else {
                generateDocumentThumbnailPreview(pdfTitle)
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = thumbnailBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "PDF Page Preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
        }
    }
}

/**
 * Generates a realistic document page preview thumbnail showing header, title,
 * image block, text lines, and layout so users can easily see what's inside.
 */
private fun generateDocumentThumbnailPreview(title: String): Bitmap {
    val width = 240
    val height = 320
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Paper White background
    canvas.drawColor(AndroidColor.WHITE)

    // Subtle paper edge border
    val borderPaint = Paint().apply {
        color = AndroidColor.parseColor("#E5E0DC")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    canvas.drawRect(1f, 1f, width - 1f, height - 1f, borderPaint)

    // Top Header Banner with PDF Badge
    val bannerPaint = Paint().apply {
        color = AndroidColor.parseColor("#D31A28")
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, width.toFloat(), 34f, bannerPaint)

    val badgeTextPaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 13f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.drawText("PDF DOC", 12f, 22f, badgeTextPaint)

    // Document Title at top
    val titlePaint = Paint().apply {
        color = AndroidColor.parseColor("#1C1B1F")
        textSize = 13f
        isFakeBoldText = true
        isAntiAlias = true
    }
    val cleanTitle = if (title.length > 18) title.substring(0, 16) + "..." else title
    canvas.drawText(cleanTitle, 12f, 58f, titlePaint)

    // Simulated Image Block inside PDF (photo preview)
    val imageBgPaint = Paint().apply {
        color = AndroidColor.parseColor("#FFF0F0")
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(12f, 72f, width - 12f, 152f, 8f, 8f, imageBgPaint)

    val imageBorderPaint = Paint().apply {
        color = AndroidColor.parseColor("#FFCDD2")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    canvas.drawRoundRect(12f, 72f, width - 12f, 152f, 8f, 8f, imageBorderPaint)

    // Sun / Mountain photo drawing inside the image box
    val sunPaint = Paint().apply {
        color = AndroidColor.parseColor("#E53935")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(38f, 96f, 9f, sunPaint)

    val mountainPath = android.graphics.Path().apply {
        moveTo(22f, 142f)
        lineTo(48f, 112f)
        lineTo(68f, 132f)
        lineTo(88f, 108f)
        lineTo(width - 22f, 142f)
        close()
    }
    val mountainPaint = Paint().apply {
        color = AndroidColor.parseColor("#FF8A80")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawPath(mountainPath, mountainPaint)

    // Simulated Paragraph Text Lines
    val linePaint = Paint().apply {
        color = AndroidColor.parseColor("#8E8E93")
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    var y = 172f
    val lineLineWidths = floatArrayOf(width - 24f, width - 48f, width - 32f, width - 64f, width - 24f, width - 38f, width - 56f)
    for (lineWidth in lineLineWidths) {
        if (y + 10f > height - 18f) break
        canvas.drawLine(12f, y, 12f + lineWidth, y, linePaint)
        y += 14f
    }

    // Page 1 footer label
    val pageNumPaint = Paint().apply {
        color = AndroidColor.parseColor("#B0BEC5")
        textSize = 10f
        isAntiAlias = true
    }
    canvas.drawText("Page 1", width - 46f, height - 10f, pageNumPaint)

    return bitmap
}

/**
 * Dialog for renaming a PDF file.
 */
@Composable
fun RenamePdfDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    val initialName = remember(currentTitle) {
        if (currentTitle.endsWith(".pdf", ignoreCase = true)) {
            currentTitle.substring(0, currentTitle.length - 4)
        } else {
            currentTitle
        }
    }
    var newTitle by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Rename Document",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1C1B1F)
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter a new name for this PDF file:",
                    fontSize = 13.sp,
                    color = Color(0xFF605D62)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    label = { Text("File Name") },
                    trailingIcon = { Text(".pdf", color = Color.Gray, modifier = Modifier.padding(end = 8.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary,
                        focusedLabelColor = RedPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newTitle.isNotBlank()) {
                        onRename(newTitle.trim())
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Rename", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF757575))
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun DeleteConfirmDialog(
    pdfTitle: String,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = RedPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Delete Document?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1C1B1F)
                )
            }
        },
        text = {
            Text(
                text = "Are you sure you want to permanently delete \"$pdfTitle\"? This action cannot be undone.",
                fontSize = 13.sp,
                color = Color(0xFF605D62)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmDelete()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF757575))
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun PdfDetailsDialog(
    pdf: PdfEntity?,
    documentTitle: String = pdf?.title ?: "Document",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val file = pdf?.path?.let { File(it) }
    val lastModifiedDate = if (file != null && file.exists()) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(file.lastModified()))
    } else if (pdf?.timestamp != null) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(pdf.timestamp))
    } else {
        "Recent Document"
    }

    val exactPath = file?.absolutePath ?: pdf?.path ?: "Internal Device Storage"
    val sizeText = pdf?.sizeFormatted ?: if (file != null && file.exists()) "${file.length() / 1024} KB" else "0 KB"
    val exactBytes = if (file != null && file.exists()) "${file.length()} bytes" else "${pdf?.sizeBytes ?: 0} bytes"
    val pagesText = "${pdf?.pageCount ?: 1} pages"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("PDF Document Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow(label = "Title", value = documentTitle)
                HorizontalDivider(color = WarmBorderLight)
                DetailRow(label = "File Size", value = "$sizeText ($exactBytes)")
                DetailRow(label = "Page Count", value = pagesText)
                DetailRow(label = "Modified Date", value = lastModifiedDate)
                DetailRow(label = "Category", value = pdf?.category ?: "Local PDF")
                HorizontalDivider(color = WarmBorderLight)
                DetailRow(
                    label = "Storage Path",
                    value = exactPath,
                    isCopyable = true,
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("PDF Path", exactPath)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Path copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = RedPrimary, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isCopyable: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    Column {
        Text(text = label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                fontSize = 13.sp,
                color = Color(0xFF1C1B1F),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (isCopyable && onCopy != null) {
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy Path",
                        tint = RedPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

fun sharePdfFile(context: Context, pdf: PdfEntity?) {
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

fun printPdfFile(context: Context, pdf: PdfEntity?, documentTitle: String) {
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

/**
 * Recent file card for horizontal scrolling carousel on Home screen.
 * Displays actual page preview thumbnail.
 */
@Composable
fun RecentFileCard(
    pdf: PdfEntity,
    onClick: () -> Unit,
    onRename: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onSaveToDevice: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDetailsDialog) {
        PdfDetailsDialog(
            pdf = pdf,
            documentTitle = pdf.title,
            onDismiss = { showDetailsDialog = false }
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            pdfTitle = pdf.title,
            onDismiss = { showDeleteDialog = false },
            onConfirmDelete = { onRemove?.invoke() }
        )
    }

    Card(
        modifier = modifier
            .width(140.dp)
            .height(156.dp)
            .testTag("recent_file_card_${pdf.id}")
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCardBgLight),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarmBorderLight))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // PDF Preview Box with options button overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                PdfThumbnailView(
                    pdfPath = pdf.path,
                    pdfTitle = pdf.title,
                    modifier = Modifier.fillMaxSize()
                )

                // Options menu button at top right overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Options",
                            tint = Color(0xFF1C1B1F),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open PDF", fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                onClick()
                            },
                            leadingIcon = { Icon(Icons.Filled.ReadMore, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Document Details", fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                showDetailsDialog = true
                            },
                            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = RedPrimary) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share PDF", fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                sharePdfFile(context, pdf)
                            },
                            leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Print Document", fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                printPdfFile(context, pdf, pdf.title)
                            },
                            leadingIcon = { Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        if (onToggleFavorite != null) {
                            DropdownMenuItem(
                                text = { Text(if (pdf.isFavorite) "Remove Favorite" else "Add Favorite", fontSize = 13.sp) },
                                onClick = {
                                    showMenu = false
                                    onToggleFavorite()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (pdf.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = null,
                                        tint = GoldStar,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                        if (onRename != null) {
                            DropdownMenuItem(
                                text = { Text("Rename File", fontSize = 13.sp) },
                                onClick = {
                                    showMenu = false
                                    onRename()
                                },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                        if (onSaveToDevice != null) {
                            DropdownMenuItem(
                                text = { Text("Save to Local Device", fontSize = 13.sp) },
                                onClick = {
                                    showMenu = false
                                    onSaveToDevice()
                                },
                                leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                        if (onRemove != null) {
                            DropdownMenuItem(
                                text = { Text("Delete Document", fontSize = 13.sp, color = RedPrimary) },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = pdf.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1B1F),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${pdf.sizeFormatted} • ${pdf.dateModifiedFormatted}",
                fontSize = 11.sp,
                color = Color(0xFF605D62),
                maxLines = 1
            )
        }
    }
}

/**
 * Grid card for Essential Tools on Home screen.
 * Matches Image 1 layout.
 */
@Composable
fun EssentialToolCard(
    title: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(115.dp)
            .testTag("tool_card_${title.lowercase().replace(" ", "_")}")
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCardBgLight),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarmBorderLight))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1B1F),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Favorite file row item for vertical list.
 * Matches Image 1 Favorite Files section.
 */
@Composable
fun FavoriteFileRow(
    pdf: PdfEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onSaveToDevice: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDetailsDialog) {
        PdfDetailsDialog(
            pdf = pdf,
            documentTitle = pdf.title,
            onDismiss = { showDetailsDialog = false }
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            pdfTitle = pdf.title,
            onDismiss = { showDeleteDialog = false },
            onConfirmDelete = { onRemove?.invoke() }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("fav_file_row_${pdf.id}")
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCardBgLight),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarmBorderLight))
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1B1F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${pdf.sizeFormatted} • ${pdf.dateModifiedFormatted}",
                    fontSize = 12.sp,
                    color = Color(0xFF605D62)
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (pdf.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
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
                        text = { Text("Open PDF", fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onClick()
                        },
                        leadingIcon = { Icon(Icons.Filled.ReadMore, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Document Details", fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            showDetailsDialog = true
                        },
                        leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = RedPrimary) }
                    )
                    DropdownMenuItem(
                        text = { Text("Share PDF", fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            sharePdfFile(context, pdf)
                        },
                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Print Document", fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            printPdfFile(context, pdf, pdf.title)
                        },
                        leadingIcon = { Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (pdf.isFavorite) "Remove Favorite" else "Add Favorite", fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onToggleFavorite()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (pdf.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = GoldStar,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    if (onRename != null) {
                        DropdownMenuItem(
                            text = { Text("Rename File", fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                onRename()
                            },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                    if (onSaveToDevice != null) {
                        DropdownMenuItem(
                            text = { Text("Save to Local Device", fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                onSaveToDevice()
                            },
                            leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                    if (onRemove != null) {
                        DropdownMenuItem(
                            text = { Text("Delete Document", fontSize = 13.sp, color = RedPrimary) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * File item with drag handles and close button, matching Image 2 (Merge PDF file list).
 */
@Composable
fun SelectedFileListItem(
    pdfName: String,
    sizeText: String,
    pageCountText: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = "Drag to reorder",
            tint = Color(0xFFB0BEC5),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

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
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pdfName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1B1F),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$sizeText • $pageCountText",
                fontSize = 12.sp,
                color = Color(0xFF605D62)
            )
        }

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove file",
                tint = Color(0xFF757575)
            )
        }
    }
}

/**
 * Dashed "+ ADD MORE FILES" card matching Image 2.
 */
@Composable
fun AddMoreFilesCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strokeColor = RedPrimary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .drawWithContent {
                drawContent()
                val stroke = Stroke(
                    width = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                )
                drawRoundRect(
                    color = strokeColor,
                    style = stroke,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                )
            }
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+ ADD MORE FILES",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = RedPrimary
        )
    }
}
