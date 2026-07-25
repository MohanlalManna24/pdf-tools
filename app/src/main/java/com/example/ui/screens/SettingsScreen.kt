package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.WarmBorderLight
import com.example.ui.theme.WarmCardBgLight
import com.example.ui.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigatePrivacy: () -> Unit,
    onNavigateAbout: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var cacheSizeMb by remember { mutableStateOf("12.4 MB") }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showProDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }

    var editName by remember(userProfile, showAccountDialog) { mutableStateOf(userProfile.name) }
    var editEmail by remember(userProfile, showAccountDialog) { mutableStateOf(userProfile.email) }
    var editPhone by remember(userProfile, showAccountDialog) { mutableStateOf(userProfile.phone) }

    // Calculate initial cache size
    LaunchedEffect(Unit) {
        try {
            val cacheDir = context.cacheDir
            var totalBytes = 0L
            cacheDir.walkTopDown().forEach { file ->
                if (file.isFile) totalBytes += file.length()
            }
            if (totalBytes > 0) {
                val mb = totalBytes / (1024f * 1024f)
                cacheSizeMb = String.format("%.1f MB", if (mb < 0.1f) 0.1f else mb)
            }
        } catch (_: Exception) {
            cacheSizeMb = "12.4 MB"
        }
    }

    // Theme Selector Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(
                    text = "Choose Theme",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1C1B1F)
                )
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.toggleDarkTheme(false)
                                showThemeDialog = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isDarkTheme,
                            onClick = {
                                viewModel.toggleDarkTheme(false)
                                showThemeDialog = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = RedPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Light Theme", fontSize = 15.sp, color = Color(0xFF1C1B1F))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.toggleDarkTheme(true)
                                showThemeDialog = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isDarkTheme,
                            onClick = {
                                viewModel.toggleDarkTheme(true)
                                showThemeDialog = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = RedPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dark Theme", fontSize = 15.sp, color = Color(0xFF1C1B1F))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel", color = RedPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Send Feedback Dialog
    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = {
                Text(
                    text = "Send Feedback",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1C1B1F)
                )
            },
            text = {
                Column {
                    Text(
                        text = "We value your input! Let us know how we can improve PDF Tools:",
                        fontSize = 13.sp,
                        color = Color(0xFF605D62)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        placeholder = { Text("Write your thoughts or suggestions here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
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
                        showFeedbackDialog = false
                        Toast.makeText(context, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                        feedbackText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Submit", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) {
                    Text("Cancel", color = Color(0xFF757575))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Pro Plan Dialog
    if (showProDialog) {
        AlertDialog(
            onDismissRequest = { showProDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = RedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pro Features Active",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1C1B1F)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Unlimited PDF Merge & Split", fontSize = 14.sp, color = Color(0xFF49454F))
                    Text("• Fast OCR Text & Image Extraction", fontSize = 14.sp, color = Color(0xFF49454F))
                    Text("• Batch Camera Scanning & Auto-Crop", fontSize = 14.sp, color = Color(0xFF49454F))
                    Text("• 100% Secure & Private Local Processing", fontSize = 14.sp, color = Color(0xFF49454F))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProDialog = false
                        Toast.makeText(context, "Pro features are ready for offline use!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Edit Guest Account Dialog
    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = RedPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit Account Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1C1B1F)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Default account is Guest Account. You can update your name, email, and preferences below:",
                        fontSize = 12.sp,
                        color = Color(0xFF605D62)
                    )

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Account Name") },
                        placeholder = { Text("e.g. Guest Account") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            focusedLabelColor = RedPrimary
                        )
                    )

                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("e.g. guest@pdftools.local") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            focusedLabelColor = RedPrimary
                        )
                    )

                    // Account Type Display (Read-Only)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF5F5F7),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E5EA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Account Type",
                                    fontSize = 11.sp,
                                    color = Color(0xFF8E8E93)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = userProfile.accountType,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1B1F)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFE8E8)
                            ) {
                                Text(
                                    text = "Read-only",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RedPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number (Optional)") },
                        placeholder = { Text("e.g. +1 555 0192") },
                        singleLine = true,
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
                        viewModel.updateUserProfile(
                            name = editName,
                            email = editEmail,
                            phone = editPhone
                        )
                        showAccountDialog = false
                        Toast.makeText(context, "Account profile updated!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.resetToGuestAccount()
                            showAccountDialog = false
                            Toast.makeText(context, "Reset to default Guest Account", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Reset Default", color = Color(0xFFD32F2F), fontSize = 13.sp)
                    }
                    TextButton(onClick = { showAccountDialog = false }) {
                        Text("Cancel", color = Color(0xFF757575), fontSize = 13.sp)
                    }
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFFAF8F5),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1B1F),
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF1C1B1F)
                            )
                        }
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Upgrade to Pro Hero Banner Card (Matches Reference Image)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pro_upgrade_banner"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF0ED)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFFCDD2)))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Illustration: Desk with laptop displaying PDF editor
                    ProBannerWorkspaceHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Upgrade to Pro",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Unlock advanced PDF editing, unlimited batch processing, and an ad-free experience.",
                            fontSize = 13.sp,
                            color = Color(0xFF605D62),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showProDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = "Get Started",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // ACCOUNT PROFILE SECTION
            SettingsSectionHeader("ACCOUNT")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_profile_card")
                    .clickable { showAccountDialog = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarmBorderLight))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val initials = userProfile.name.trim().split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .joinToString("")
                        .uppercase()

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(RedPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (initials.isNotEmpty()) {
                            Text(
                                text = initials,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Profile",
                                tint = RedPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userProfile.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFE8E8)
                            ) {
                                Text(
                                    text = userProfile.accountType,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RedPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = userProfile.email,
                            fontSize = 13.sp,
                            color = Color(0xFF605D62)
                        )

                        if (userProfile.phone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = userProfile.phone,
                                fontSize = 12.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }

            // APP SETTINGS Section
            SettingsSectionHeader("APP SETTINGS")

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Theme
                SettingsRowItem(
                    title = "Theme",
                    trailingValue = if (isDarkTheme) "Dark" else "Light",
                    icon = Icons.Filled.Palette,
                    onClick = { showThemeDialog = true }
                )

                // Clear Cache
                SettingsRowItem(
                    title = "Clear Cache",
                    trailingValue = cacheSizeMb,
                    icon = Icons.Filled.CleaningServices,
                    onClick = {
                        try {
                            context.cacheDir.deleteRecursively()
                            context.cacheDir.mkdirs()
                        } catch (_: Exception) {}
                        cacheSizeMb = "0.0 KB"
                        Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // PRIVACY & SECURITY Section
            SettingsSectionHeader("PRIVACY & SECURITY")

            // Combined Card with Privacy Policy & Terms of Service
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF0ED)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFFCDD2)))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Privacy Policy Item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigatePrivacy() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFCDD2).copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = "Privacy Policy",
                                tint = RedPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Privacy Policy",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F)
                            )
                            Text(
                                text = "How we handle your data",
                                fontSize = 12.sp,
                                color = Color(0xFF605D62)
                            )
                        }

                        Icon(
                            imageVector = Icons.Filled.OpenInNew,
                            contentDescription = null,
                            tint = Color(0xFF757575),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(
                        color = Color(0xFFFFCDD2).copy(alpha = 0.6f),
                        thickness = 1.dp
                    )

                    // Terms of Service Item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigatePrivacy() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFCDD2).copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = "Terms of Service",
                                tint = RedPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Terms of Service",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F)
                            )
                            Text(
                                text = "Legal agreements",
                                fontSize = 12.sp,
                                color = Color(0xFF605D62)
                            )
                        }

                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF757575),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // SUPPORT Section
            SettingsSectionHeader("SUPPORT")

            SettingsRowItem(
                title = "Send Feedback",
                trailingValue = null,
                icon = Icons.Filled.RateReview,
                onClick = { showFeedbackDialog = true }
            )

            // ABOUT Section
            SettingsSectionHeader("ABOUT")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(RedPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = "PDF Tools Logo",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "PDF Tools",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Version 1.0",
                    fontSize = 13.sp,
                    color = Color(0xFF605D62)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "© 2026 PDF Tools, developed by Mohanlal.",
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1C1B1F),
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsRowItem(
    title: String,
    trailingValue: String?,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFDF0ED)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = RedPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1C1B1F),
            modifier = Modifier.weight(1f)
        )

        if (trailingValue != null) {
            Text(
                text = trailingValue,
                fontSize = 13.sp,
                color = Color(0xFF605D62),
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF757575),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Custom Canvas Header Illustration depicting a clean workspace with a laptop on a wooden desk
 * displaying a PDF document app on screen, matching the reference image hero artwork.
 */
@Composable
private fun ProBannerWorkspaceHeader(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Background Wall (Bright Off-White/Grey)
        drawRect(
            color = Color(0xFFEFECE8),
            size = Size(w, h)
        )

        // Window Frame on left background
        drawRoundRect(
            color = Color(0xFFE0DDD8),
            topLeft = Offset(w * 0.08f, h * 0.08f),
            size = Size(w * 0.22f, h * 0.55f),
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 3f)
        )
        // Window mullions
        drawLine(
            color = Color(0xFFE0DDD8),
            start = Offset(w * 0.19f, h * 0.08f),
            end = Offset(w * 0.19f, h * 0.63f),
            strokeWidth = 2f
        )
        drawLine(
            color = Color(0xFFE0DDD8),
            start = Offset(w * 0.08f, h * 0.35f),
            end = Offset(w * 0.30f, h * 0.35f),
            strokeWidth = 2f
        )

        // Bookshelf in background top right
        drawRect(
            color = Color(0xFFD6D1CA),
            topLeft = Offset(w * 0.65f, h * 0.08f),
            size = Size(w * 0.28f, h * 0.32f)
        )
        // Books on shelf
        drawRect(color = Color(0xFFC0392B), topLeft = Offset(w * 0.68f, h * 0.12f), size = Size(w * 0.03f, h * 0.22f))
        drawRect(color = Color(0xFF2980B9), topLeft = Offset(w * 0.72f, h * 0.15f), size = Size(w * 0.035f, h * 0.19f))
        drawRect(color = Color(0xFF27AE60), topLeft = Offset(w * 0.765f, h * 0.10f), size = Size(w * 0.03f, h * 0.24f))
        drawRect(color = Color(0xFFF39C12), topLeft = Offset(w * 0.81f, h * 0.14f), size = Size(w * 0.04f, h * 0.20f))

        // Wooden Desk Surface at bottom perspective
        val deskTopY = h * 0.50f
        val deskPath = Path().apply {
            moveTo(0f, deskTopY)
            lineTo(w, deskTopY)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path = deskPath, color = Color(0xFFD7CCC8)) // Warm Wood

        // Black Desk Mat underneath laptop & keyboard
        val matPath = Path().apply {
            moveTo(w * 0.12f, h * 0.58f)
            lineTo(w * 0.88f, h * 0.58f)
            lineTo(w * 0.95f, h)
            lineTo(w * 0.05f, h)
            close()
        }
        drawPath(path = matPath, color = Color(0xFF2B2B2B))

        // Succulent Plant in White Pot on right
        val plantX = w * 0.82f
        val plantY = h * 0.54f
        // White Pot
        drawCircle(color = Color.White, radius = 14f, center = Offset(plantX, plantY))
        // Green Leaves
        drawCircle(color = Color(0xFF4CAF50), radius = 10f, center = Offset(plantX - 4f, plantY - 12f))
        drawCircle(color = Color(0xFF81C784), radius = 12f, center = Offset(plantX + 4f, plantY - 14f))

        // Laptop Computer Centered
        val laptopLeft = w * 0.26f
        val laptopTop = h * 0.18f
        val laptopW = w * 0.44f
        val laptopH = h * 0.52f

        // Laptop Outer Lid Shadow & Aluminum Body
        drawRoundRect(
            color = Color(0xFFB0BEC5),
            topLeft = Offset(laptopLeft - 4f, laptopTop - 2f),
            size = Size(laptopW + 8f, laptopH + 4f),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Laptop Display Bezel (Dark)
        drawRoundRect(
            color = Color(0xFF1A1A1A),
            topLeft = Offset(laptopLeft, laptopTop),
            size = Size(laptopW, laptopH),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Laptop Screen Active Display (PDF App UI)
        val screenLeft = laptopLeft + 6f
        val screenTop = laptopTop + 6f
        val screenW = laptopW - 12f
        val screenH = laptopH - 12f

        drawRect(
            color = Color(0xFFFAF8F5),
            topLeft = Offset(screenLeft, screenTop),
            size = Size(screenW, screenH)
        )

        // Screen Sidebar / Top Navigation Bar
        drawRect(
            color = Color(0xFFD31A28),
            topLeft = Offset(screenLeft, screenTop),
            size = Size(screenW, 10f)
        )

        // PDF Document Page Preview inside Screen
        val docW = screenW * 0.58f
        val docH = screenH * 0.68f
        val docX = screenLeft + (screenW - docW) / 2f
        val docY = screenTop + 14f

        drawRoundRect(
            color = Color.White,
            topLeft = Offset(docX, docY),
            size = Size(docW, docH),
            cornerRadius = CornerRadius(3f, 3f)
        )
        // Red PDF Icon Badge on screen
        drawRect(
            color = Color(0xFFD31A28),
            topLeft = Offset(docX + 6f, docY + 5f),
            size = Size(10f, 10f)
        )
        // Simulated Text Lines on screen document
        drawLine(color = Color(0xFF9E9E9E), start = Offset(docX + 20f, docY + 8f), end = Offset(docX + docW - 8f, docY + 8f), strokeWidth = 2f)
        drawLine(color = Color(0xFFBDBDBD), start = Offset(docX + 6f, docY + 20f), end = Offset(docX + docW - 8f, docY + 20f), strokeWidth = 2f)
        drawLine(color = Color(0xFFBDBDBD), start = Offset(docX + 6f, docY + 26f), end = Offset(docX + docW - 18f, docY + 26f), strokeWidth = 2f)
        drawLine(color = Color(0xFFBDBDBD), start = Offset(docX + 6f, docY + 32f), end = Offset(docX + docW - 12f, docY + 32f), strokeWidth = 2f)

        // Red Action Button on Screen App
        drawRoundRect(
            color = Color(0xFFD31A28),
            topLeft = Offset(docX + 6f, docY + docH - 12f),
            size = Size(28f, 8f),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // Laptop Keyboard Base Surface
        val baseTop = laptopTop + laptopH
        val baseW = laptopW + w * 0.10f
        val baseLeft = w * 0.21f
        val baseH = h * 0.20f

        val baseKeyboardPath = Path().apply {
            moveTo(laptopLeft, baseTop)
            lineTo(laptopLeft + laptopW, baseTop)
            lineTo(baseLeft + baseW, baseTop + baseH)
            lineTo(baseLeft, baseTop + baseH)
            close()
        }
        drawPath(path = baseKeyboardPath, color = Color(0xFFCFD8DC))

        // Keyboard Keys Area
        val kbPath = Path().apply {
            moveTo(laptopLeft + 12f, baseTop + 4f)
            lineTo(laptopLeft + laptopW - 12f, baseTop + 4f)
            lineTo(baseLeft + baseW - 20f, baseTop + baseH - 18f)
            lineTo(baseLeft + 20f, baseTop + baseH - 18f)
            close()
        }
        drawPath(path = kbPath, color = Color(0xFF37474F))

        // White Wireless Keyboard on right desk
        val extKbX = w * 0.52f
        val extKbY = h * 0.74f
        drawRoundRect(
            color = Color(0xFFECEFF1),
            topLeft = Offset(extKbX, extKbY),
            size = Size(w * 0.24f, h * 0.12f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }
}

