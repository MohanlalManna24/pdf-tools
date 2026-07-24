package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.components.NavTab
import com.example.ui.components.PdfBottomNavBar
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.ErrorScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OcrReviewScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PdfReaderScreen
import com.example.ui.screens.PrivacyScreen
import com.example.ui.screens.ProcessingScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SuccessScreen
import com.example.ui.screens.ToolDetailScreen
import com.example.ui.screens.ToolsListScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ProcessingUiState

sealed class AppScreen {
    object Onboarding : AppScreen()
    data class MainTab(val tab: NavTab) : AppScreen()
    data class ToolDetail(val toolId: String) : AppScreen()
    object Scanner : AppScreen()
    object OcrReview : AppScreen()
    object PdfReader : AppScreen()
    object Privacy : AppScreen()
    object About : AppScreen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val processingState by viewModel.processingState.collectAsState()
            val activePdf by viewModel.activePdf.collectAsState()

            var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.MainTab(NavTab.HOME)) }
            var currentTab by remember { mutableStateOf(NavTab.HOME) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                // Global Processing Overlay logic
                when (val state = processingState) {
                    is ProcessingUiState.Processing -> {
                        ProcessingScreen(
                            toolName = state.toolName,
                            onCancel = { viewModel.resetProcessingState() }
                        )
                    }
                    is ProcessingUiState.Success -> {
                        SuccessScreen(
                            title = state.title,
                            sizeFormatted = state.sizeFormatted,
                            pageCount = state.pageCount,
                            filePath = state.path,
                            onOpenPdf = {
                                viewModel.resetProcessingState()
                                currentScreen = AppScreen.PdfReader
                            },
                            onReturnHome = {
                                viewModel.resetProcessingState()
                                currentScreen = AppScreen.MainTab(NavTab.HOME)
                                currentTab = NavTab.HOME
                            }
                        )
                    }
                    is ProcessingUiState.Error -> {
                        ErrorScreen(
                            errorMessage = state.message,
                            onRetry = { viewModel.resetProcessingState() },
                            onReturnHome = {
                                viewModel.resetProcessingState()
                                currentScreen = AppScreen.MainTab(NavTab.HOME)
                            }
                        )
                    }
                    ProcessingUiState.Idle -> {
                        // Main Navigation Structure
                        val showBottomBar = currentScreen is AppScreen.MainTab && (currentScreen as AppScreen.MainTab).tab != NavTab.SCANNER

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color(0xFFFAF8F5),
                            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                            bottomBar = {
                                if (showBottomBar) {
                                    PdfBottomNavBar(
                                        selectedTab = currentTab,
                                        onTabSelected = { tab ->
                                            currentTab = tab
                                            currentScreen = AppScreen.MainTab(tab)
                                        }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = innerPadding.calculateBottomPadding())
                            ) {
                                AnimatedContent(
                                    targetState = currentScreen,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "screen_transition"
                                ) { targetScreen ->
                                    when (targetScreen) {
                                        is AppScreen.Onboarding -> {
                                            OnboardingScreen(
                                                onFinishOnboarding = {
                                                    currentTab = NavTab.HOME
                                                    currentScreen = AppScreen.MainTab(NavTab.HOME)
                                                }
                                            )
                                        }

                                        is AppScreen.MainTab -> {
                                            when (targetScreen.tab) {
                                                NavTab.HOME -> {
                                                    HomeScreen(
                                                        viewModel = viewModel,
                                                        onSelectTool = { toolId ->
                                                            when (toolId) {
                                                                "scanner" -> currentScreen = AppScreen.Scanner
                                                                "ocr" -> currentScreen = AppScreen.OcrReview
                                                                "reader" -> currentScreen = AppScreen.PdfReader
                                                                else -> currentScreen = AppScreen.ToolDetail(toolId)
                                                            }
                                                        },
                                                        onOpenPdf = { pdf ->
                                                            viewModel.openPdf(pdf)
                                                            currentScreen = AppScreen.PdfReader
                                                        },
                                                        onViewAllRecent = {
                                                            currentTab = NavTab.FILES
                                                            currentScreen = AppScreen.MainTab(NavTab.FILES)
                                                        },
                                                        onOpenProfile = {
                                                            currentTab = NavTab.SETTINGS
                                                            currentScreen = AppScreen.MainTab(NavTab.SETTINGS)
                                                        }
                                                    )
                                                }

                                                NavTab.TOOLS -> {
                                                    ToolsListScreen(
                                                        onSelectTool = { toolId ->
                                                            when (toolId) {
                                                                "scanner" -> currentScreen = AppScreen.Scanner
                                                                "ocr" -> currentScreen = AppScreen.OcrReview
                                                                "reader" -> currentScreen = AppScreen.PdfReader
                                                                else -> currentScreen = AppScreen.ToolDetail(toolId)
                                                            }
                                                        }
                                                    )
                                                }

                                                NavTab.SCANNER -> {
                                                    ScannerScreen(
                                                        viewModel = viewModel,
                                                        onClose = {
                                                            currentTab = NavTab.HOME
                                                            currentScreen = AppScreen.MainTab(NavTab.HOME)
                                                        },
                                                        onCompleteScan = {
                                                            currentScreen = AppScreen.PdfReader
                                                        }
                                                    )
                                                }

                                                NavTab.FILES -> {
                                                    FilesScreen(
                                                        viewModel = viewModel,
                                                        onOpenPdf = { pdf ->
                                                            viewModel.openPdf(pdf)
                                                            currentScreen = AppScreen.PdfReader
                                                        }
                                                    )
                                                }

                                                NavTab.SETTINGS -> {
                                                    SettingsScreen(
                                                        viewModel = viewModel,
                                                        onNavigatePrivacy = { currentScreen = AppScreen.Privacy },
                                                        onNavigateAbout = { currentScreen = AppScreen.About },
                                                        onNavigateBack = {
                                                            currentTab = NavTab.HOME
                                                            currentScreen = AppScreen.MainTab(NavTab.HOME)
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        is AppScreen.ToolDetail -> {
                                            ToolDetailScreen(
                                                toolId = targetScreen.toolId,
                                                onBack = { currentScreen = AppScreen.MainTab(currentTab) },
                                                onExecuteTool = { titles, param ->
                                                    viewModel.executeTool(targetScreen.toolId, titles, param)
                                                }
                                            )
                                        }

                                        is AppScreen.Scanner -> {
                                            ScannerScreen(
                                                viewModel = viewModel,
                                                onClose = { currentScreen = AppScreen.MainTab(currentTab) },
                                                onCompleteScan = {
                                                    currentScreen = AppScreen.PdfReader
                                                }
                                            )
                                        }

                                        is AppScreen.OcrReview -> {
                                            OcrReviewScreen(
                                                onBack = { currentScreen = AppScreen.MainTab(currentTab) },
                                                onSaveAsPdf = { extractedText ->
                                                    viewModel.executeTool("ocr", listOf("Scanned_Invoice.pdf"), extractedText)
                                                }
                                            )
                                        }

                                        is AppScreen.PdfReader -> {
                                            PdfReaderScreen(
                                                pdf = activePdf,
                                                onBack = { currentScreen = AppScreen.MainTab(currentTab) },
                                                onOpenLocalPdf = { uri -> viewModel.importUriToApp(uri) },
                                                onRenamePdf = { targetPdf, newName ->
                                                    viewModel.renamePdf(targetPdf, newName)
                                                },
                                                onDeletePdf = { targetPdf ->
                                                    viewModel.deleteFile(targetPdf)
                                                },
                                                onToggleFavorite = { targetPdf ->
                                                    viewModel.toggleFavorite(targetPdf)
                                                }
                                            )
                                        }

                                        is AppScreen.Privacy -> {
                                            PrivacyScreen(
                                                onBack = { currentScreen = AppScreen.MainTab(NavTab.SETTINGS) }
                                            )
                                        }

                                        is AppScreen.About -> {
                                            AboutScreen(
                                                onBack = { currentScreen = AppScreen.MainTab(NavTab.SETTINGS) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
