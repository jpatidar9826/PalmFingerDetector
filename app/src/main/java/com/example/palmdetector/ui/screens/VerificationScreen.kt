package com.example.palmdetector.ui.screens

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.palmdetector.ui.components.SecondaryButton
import com.example.palmdetector.ui.theme.ErrorRed
import com.example.palmdetector.ui.theme.SuccessGreen
import com.example.palmdetector.ui.theme.WarningYellow
import com.example.palmdetector.utils.HandLandmarkerHelper
import com.example.palmdetector.viewmodel.BiometricViewModel
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.Executors

@Composable
fun VerificationScreen(
    viewModel: BiometricViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current


    val message by viewModel.verificationMessage.collectAsState()
    val status by viewModel.verificationStatus.collectAsState()


    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }


    val handLandmarkerHelper = remember {
        HandLandmarkerHelper(context, object : HandLandmarkerHelper.LandmarkerListener {
            override fun onError(error: String) {
                Log.e("VerifyScreen", "MediaPipe Error: $error")
            }

            override fun onResults(result: HandLandmarkerResult) {
                viewModel.verifyUser(result)
            }
        })
    }


    LaunchedEffect(Unit) {
        viewModel.resetVerification()
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {


        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                handLandmarkerHelper.detectLiveStream(imageProxy, false)
                                imageProxy.close()
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalyzer
                        )
                    } catch (exc: Exception) {
                        Log.e("VerifyScreen", "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        val overlayColor = when (status) {
            BiometricViewModel.VerifyStatus.IDLE -> Color.White
            BiometricViewModel.VerifyStatus.PROCESSING -> WarningYellow
            BiometricViewModel.VerifyStatus.SUCCESS -> SuccessGreen
            BiometricViewModel.VerifyStatus.FAIL -> ErrorRed
            else -> Color.White
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val ovalWidth = width * 0.8f
            val ovalHeight = height * 0.45f

            drawRect(
                color = overlayColor,
                topLeft = Offset(width * 0.1f, height * 0.2f),
                size = Size(width * 0.8f, height * 0.5f),
                style = Stroke(width = 8f)
            )
        }

        VerificationHeader()

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .background(
                        color = when (status) {
                            BiometricViewModel.VerifyStatus.SUCCESS -> SuccessGreen
                            BiometricViewModel.VerifyStatus.FAIL -> ErrorRed
                            else -> Color.Black.copy(alpha = 0.7f)
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (status == BiometricViewModel.VerifyStatus.SUCCESS || status == BiometricViewModel.VerifyStatus.FAIL) {
                SecondaryButton(
                    text = "SCAN AGAIN",
                    onClick = { viewModel.resetVerification() },
                    modifier = Modifier.fillMaxWidth(0.7f)
                )
            } else {
                Spacer(modifier = Modifier.height(56.dp))
            }
        }
    }
}


@Composable
fun VerificationHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "VERIFICATION MODE",
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}