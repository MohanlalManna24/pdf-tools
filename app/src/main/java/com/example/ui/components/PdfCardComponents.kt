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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
 * Recent file card for horizontal scrolling carousel on Home screen.
 * Displays actual page preview thumbnail.
 */
@Composable
fun RecentFileCard(
    pdf: PdfEntity,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
    onSaveToDevice: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

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
                                text = { Text("Remove from Recents", fontSize = 13.sp, color = RedPrimary) },
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
    onRemove: (() -> Unit)? = null,
    onSaveToDevice: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

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
                            text = { Text("Remove from Recents", fontSize = 13.sp, color = RedPrimary) },
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
