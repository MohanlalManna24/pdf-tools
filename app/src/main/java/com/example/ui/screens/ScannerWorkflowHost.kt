package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.viewmodel.MainViewModel

enum class ScannerStep {
    CAPTURE,
    EDIT
}

@Composable
fun ScannerWorkflowHost(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(ScannerStep.CAPTURE) }
    val capturedPages = remember { mutableStateListOf<Bitmap>() }

    when (currentStep) {
        ScannerStep.CAPTURE -> {
            ScannerScreen(
                onClose = onClose,
                onProceedToEdit = { bitmaps ->
                    capturedPages.clear()
                    capturedPages.addAll(bitmaps)
                    currentStep = ScannerStep.EDIT
                }
            )
        }

        ScannerStep.EDIT -> {
            EditScanScreen(
                initialPages = capturedPages,
                onBackToScan = { updatedPages ->
                    capturedPages.clear()
                    capturedPages.addAll(updatedPages)
                    currentStep = ScannerStep.CAPTURE
                },
                onDone = { finalBitmaps ->
                    viewModel.saveScannedPdf(
                        bitmaps = finalBitmaps,
                        onSuccess = {
                            onComplete()
                        }
                    )
                }
            )
        }
    }
}
