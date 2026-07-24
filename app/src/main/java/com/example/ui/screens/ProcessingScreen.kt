package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RedPrimary

@Composable
fun ProcessingScreen(
    toolName: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin_loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Radial Gradient Background matching Image 6
    val bgBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFFFFEBEE),
            Color(0xFFFFF7F7),
            Color(0xFFFAF8F5)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Animated Progress Circle (Matches Image 6)
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Dashed Arc Circle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 5.dp.toPx()
                    drawCircle(
                        color = Color(0xFFEF9A9A).copy(alpha = 0.5f),
                        style = Stroke(
                            width = strokeW,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 16f), 0f)
                        )
                    )
                }

                // Inner Spinning Red Arc
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotation)
                ) {
                    val strokeW = 6.dp.toPx()
                    drawArc(
                        color = Color(0xFFD31A28),
                        startAngle = 0f,
                        sweepAngle = 120f,
                        useCenter = false,
                        style = Stroke(width = strokeW)
                    )
                }

                // Center Sync Icon
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier
                        .size(44.dp)
                        .rotate(-rotation)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = toolName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1B1F),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "This happens entirely on your device.",
                fontSize = 15.sp,
                color = Color(0xFF605D62),
                textAlign = TextAlign.Center
            )

            if (toolName.contains("WorkManager") || toolName.contains("Eco CPU")) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⚡ Battery Saver Active • WorkManager Throttled",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Cancel Pill Outlined Button (Matches Image 6)
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(48.dp)
                    .testTag("processing_cancel_btn"),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, RedPrimary)
            ) {
                Text(
                    text = "Cancel",
                    color = RedPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
