package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RedPrimary

enum class NavTab(val title: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    TOOLS("Tools", Icons.Filled.Build, Icons.Outlined.Build),
    SCANNER("Scan", Icons.Filled.DocumentScanner, Icons.Outlined.DocumentScanner),
    FILES("Files", Icons.Filled.Folder, Icons.Outlined.Folder),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

/**
 * Custom Shape that creates a smooth concave cradle notch cutout in the top-center
 * of the bottom navigation bar for a circular floating action button.
 */
class CradleBottomBarShape(
    private val circleRadius: Dp = 26.dp,
    private val circleGap: Dp = 5.dp,
    private val cradleDepth: Dp = 22.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val radiusPx = with(density) { circleRadius.toPx() }
            val gapPx = with(density) { circleGap.toPx() }
            val depthPx = with(density) { cradleDepth.toPx() }

            val centerX = size.width / 2f
            val cutoutRadius = radiusPx + gapPx

            moveTo(0f, 0f)

            val leftStart = centerX - cutoutRadius - with(density) { 12.dp.toPx() }
            lineTo(leftStart, 0f)

            cubicTo(
                leftStart + with(density) { 10.dp.toPx() }, 0f,
                centerX - cutoutRadius * 0.6f, depthPx,
                centerX, depthPx
            )

            val rightEnd = centerX + cutoutRadius + with(density) { 12.dp.toPx() }
            cubicTo(
                centerX + cutoutRadius * 0.6f, depthPx,
                rightEnd - with(density) { 10.dp.toPx() }, 0f,
                rightEnd, 0f
            )

            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun PdfBottomNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Base Full-Width White Navigation Bar Surface with Cradle Cutout Notch
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            shape = CradleBottomBarShape(
                circleRadius = 26.dp,
                circleGap = 5.dp,
                cradleDepth = 22.dp
            ),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Bottom
                ) {
                    NavTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) RedPrimary else Color(0xFF757575),
                            label = "tab_fg"
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("nav_tab_${tab.name.lowercase()}")
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onTabSelected(tab) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            if (tab == NavTab.SCANNER) {
                                // Reserved height space for center circular floating button
                                Spacer(modifier = Modifier.height(22.dp))
                            } else {
                                Icon(
                                    imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                    contentDescription = tab.title,
                                    tint = contentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Floating Center Circular Scanner Button
        val isScannerSelected = selectedTab == NavTab.SCANNER
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)
                .size(52.dp)
                .shadow(
                    elevation = if (isScannerSelected) 8.dp else 4.dp,
                    shape = CircleShape,
                    clip = false
                )
                .testTag("nav_tab_scanner_button")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onTabSelected(NavTab.SCANNER) },
            shape = CircleShape,
            color = if (isScannerSelected) RedPrimary else Color.White,
            border = androidx.compose.foundation.BorderStroke(
                width = if (isScannerSelected) 3.dp else 2.dp,
                color = if (isScannerSelected) Color.White else Color(0xFFE0E0E0)
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.DocumentScanner,
                    contentDescription = "Scanner",
                    tint = if (isScannerSelected) Color.White else RedPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}



