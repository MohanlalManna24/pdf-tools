package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableStateListOf
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
import com.example.cv.OpenCVManager

sealed class AppScreen {
    object Onboarding : AppScreen()
    data class MainTab(val tab: NavTab) : AppScreen()
    data class ToolDetail(val toolId: String) : AppScreen()
    object Scanner : AppScreen()
    object PdfReader : AppScreen()
    object Privacy : AppScreen()
    object About : AppScreen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize OpenCV SDK safely on app startup
        OpenCVManager.init(this)

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val processingState by viewModel.processingState.collectAsState()
            val activePdf by viewModel.activePdf.collectAsState()
            val allPdfs by viewModel.allFiles.collectAsState()

            val context = androidx.compose.ui.platform.LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("app_settings", MODE_PRIVATE) }
            val isFirstLaunch = remember { sharedPrefs.getBoolean("is_first_launch", true) }

            val initialScreen = remember {
                if (isFirstLaunch) AppScreen.Onboarding else AppScreen.MainTab(NavTab.HOME)
            }
            val navigationStack = remember { mutableStateListOf<AppScreen>(initialScreen) }
            val currentScreen = navigationStack.lastOrNull() ?: AppScreen.MainTab(NavTab.HOME)
            var currentTab by remember { mutableStateOf(NavTab.HOME) }

            fun navigateTo(screen: AppScreen) {
                if (screen is AppScreen.MainTab) {
                    currentTab = screen.tab
                    navigationStack.clear()
                    navigationStack.add(screen)
                } else {
                    navigationStack.add(screen)
                }
            }

            fun navigateBack() {
                if (navigationStack.size > 1) {
                    navigationStack.removeAt(navigationStack.lastIndex)
                    val top = navigationStack.lastOrNull()
                    if (top is AppScreen.MainTab) {
                        currentTab = top.tab
                    }
                } else if (currentTab != NavTab.HOME) {
                    currentTab = NavTab.HOME
                    navigationStack.clear()
                    navigationStack.add(AppScreen.MainTab(NavTab.HOME))
                }
            }

            BackHandler(
                enabled = navigationStack.size > 1 || (currentScreen is AppScreen.MainTab && (currentScreen as AppScreen.MainTab).tab != NavTab.HOME)
            ) {
                navigateBack()
            }

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
                                navigateTo(AppScreen.PdfReader)
                            },
                            onReturnHome = {
                                viewModel.resetProcessingState()
                                navigateTo(AppScreen.MainTab(NavTab.HOME))
                            },
                            onRenameFile = { newTitle, newPath ->
                                viewModel.updateSuccessStateTitle(newTitle, newPath)
                            }
                        )
                    }
                    is ProcessingUiState.Error -> {
                        ErrorScreen(
                            errorMessage = state.message,
                            onRetry = { viewModel.resetProcessingState() },
                            onReturnHome = {
                                viewModel.resetProcessingState()
                                navigateTo(AppScreen.MainTab(NavTab.HOME))
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
                                            navigateTo(AppScreen.MainTab(tab))
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
                                                    sharedPrefs.edit().putBoolean("is_first_launch", false).apply()
                                                    navigateTo(AppScreen.MainTab(NavTab.HOME))
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
                                                                "scanner" -> navigateTo(AppScreen.Scanner)
                                                                "reader" -> navigateTo(AppScreen.PdfReader)
                                                                else -> navigateTo(AppScreen.ToolDetail(toolId))
                                                            }
                                                        },
                                                        onOpenPdf = { pdf ->
                                                            viewModel.openPdf(pdf)
                                                            navigateTo(AppScreen.PdfReader)
                                                        },
                                                        onViewAllRecent = {
                                                            navigateTo(AppScreen.MainTab(NavTab.FILES))
                                                        },
                                                        onOpenProfile = {
                                                            navigateTo(AppScreen.MainTab(NavTab.SETTINGS))
                                                        }
                                                    )
                                                }

                                                NavTab.TOOLS -> {
                                                    ToolsListScreen(
                                                        onSelectTool = { toolId ->
                                                            when (toolId) {
                                                                "scanner" -> navigateTo(AppScreen.Scanner)
                                                                "reader" -> navigateTo(AppScreen.PdfReader)
                                                                else -> navigateTo(AppScreen.ToolDetail(toolId))
                                                            }
                                                        }
                                                    )
                                                }

                                                NavTab.SCANNER -> {
                                                    ScannerScreen(
                                                        viewModel = viewModel,
                                                        onClose = {
                                                            navigateTo(AppScreen.MainTab(NavTab.HOME))
                                                        },
                                                        onCompleteScan = {
                                                            navigateTo(AppScreen.PdfReader)
                                                        }
                                                    )
                                                }

                                                NavTab.FILES -> {
                                                    FilesScreen(
                                                        viewModel = viewModel,
                                                        onOpenPdf = { pdf ->
                                                            viewModel.openPdf(pdf)
                                                            navigateTo(AppScreen.PdfReader)
                                                        }
                                                    )
                                                }

                                                NavTab.SETTINGS -> {
                                                    SettingsScreen(
                                                        viewModel = viewModel,
                                                        onNavigatePrivacy = { navigateTo(AppScreen.Privacy) },
                                                        onNavigateAbout = { navigateTo(AppScreen.About) },
                                                        onNavigateBack = {
                                                            navigateTo(AppScreen.MainTab(NavTab.HOME))
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        is AppScreen.ToolDetail -> {
                                            ToolDetailScreen(
                                                toolId = targetScreen.toolId,
                                                activePdf = activePdf,
                                                allPdfs = allPdfs,
                                                onBack = { navigateBack() },
                                                onExecuteTool = { titles, param ->
                                                    viewModel.executeTool(targetScreen.toolId, titles, param)
                                                }
                                            )
                                        }

                                        is AppScreen.Scanner -> {
                                            ScannerScreen(
                                                viewModel = viewModel,
                                                onClose = { navigateBack() },
                                                onCompleteScan = {
                                                    navigateTo(AppScreen.PdfReader)
                                                }
                                            )
                                        }

                                        is AppScreen.PdfReader -> {
                                            val allPdfs by viewModel.allFiles.collectAsState()
                                            PdfReaderScreen(
                                                pdf = activePdf,
                                                allPdfs = allPdfs,
                                                onSelectPdf = { selectedPdf ->
                                                    viewModel.openPdf(selectedPdf)
                                                },
                                                onBack = { navigateBack() },
                                                onOpenLocalPdf = { uri ->
                                                    viewModel.importUriToApp(uri) { importedPdf ->
                                                        viewModel.openPdf(importedPdf)
                                                    }
                                                },
                                                onRenamePdf = { targetPdf, newName ->
                                                    viewModel.renamePdf(targetPdf, newName)
                                                },
                                                onDeletePdf = { targetPdf ->
                                                    viewModel.deleteFile(targetPdf)
                                                },
                                                onToggleFavorite = { targetPdf ->
                                                    viewModel.toggleFavorite(targetPdf)
                                                },
                                                onOpenTool = { toolId ->
                                                    navigateTo(AppScreen.ToolDetail(toolId))
                                                }
                                            )
                                        }

                                        is AppScreen.Privacy -> {
                                            PrivacyScreen(
                                                onBack = { navigateBack() }
                                            )
                                        }

                                        is AppScreen.About -> {
                                            AboutScreen(
                                                onBack = { navigateBack() }
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
