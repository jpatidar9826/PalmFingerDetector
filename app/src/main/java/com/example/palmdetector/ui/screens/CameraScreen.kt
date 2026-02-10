package com.example.palmdetector.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.palmdetector.model.BiometricStep
import com.example.palmdetector.model.HandType
import com.example.palmdetector.model.SimplePoint
import com.example.palmdetector.ui.components.PrimaryButton
import com.example.palmdetector.ui.components.SecondaryButton
import com.example.palmdetector.ui.theme.SuccessGreen
import com.example.palmdetector.utils.HandLandmarkerHelper
import com.example.palmdetector.utils.ImageQualityHelper
import com.example.palmdetector.viewmodel.BiometricViewModel
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    handType: HandType,
    step: BiometricStep,
    viewModel: BiometricViewModel,
    onCaptureSuccess: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val feedbackText by viewModel.currentFeedback.collectAsState()
    val isCaptureEnabled by viewModel.isCaptureEnabled.collectAsState()

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var frozenLandmarks by remember { mutableStateOf<List<SimplePoint>?>(null) }

    var isSaving by remember { mutableStateOf(false) }

    var latestLiveLandmarks by remember { mutableStateOf<List<SimplePoint>?>(null) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }

    val handLandmarkerHelper = remember {
        HandLandmarkerHelper(context, object : HandLandmarkerHelper.LandmarkerListener {
            override fun onError(error: String) {
                Log.e("CameraScreen", "MediaPipe: $error")
            }

            override fun onResults(result: HandLandmarkerResult) {
                if (capturedBitmap == null) {
                    viewModel.processHandResult(result, handType, step)
                    if (result.landmarks().isNotEmpty()) {
                        latestLiveLandmarks =
                            result.landmarks()[0].map { SimplePoint(it.x(), it.y()) }
                    }
                }
            }
        })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (capturedBitmap == null) {

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build()
                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val brightness = ImageQualityHelper.getLuminosity(imageProxy)
                                    val blurScore = ImageQualityHelper.getBlurScore(imageProxy)

                                    runBlocking(Dispatchers.Main) {
                                        viewModel.updateQualityMetrics(brightness, blurScore)
                                    }
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
                                imageAnalyzer,
                                imageCapture
                            )
                        } catch (exc: Exception) {
                            Log.e("CameraScreen", "Binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val paintColor = if (isCaptureEnabled) SuccessGreen else Color.White
                val stroke = Stroke(width = 6f)

                if (step == BiometricStep.PALM) {
                    drawRect(
                        color = paintColor,
                        topLeft = Offset(width * 0.1f, height * 0.2f),
                        size = Size(width * 0.8f, height * 0.5f),
                        style = stroke
                    )
                } else {
                    drawOval(
                        color = paintColor,
                        topLeft = Offset(width * 0.3f, height * 0.35f),
                        size = Size(width * 0.4f, height * 0.3f),
                        style = stroke
                    )
                }
            }

            CameraHeader(handType, step)

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = feedbackText,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
            ) {
                ShutterButton(
                    isEnabled = isCaptureEnabled,
                    onClick = {
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bitmap = imageProxyToBitmap(image)
                                    capturedBitmap = bitmap
                                    frozenLandmarks = latestLiveLandmarks
                                    image.close()
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("CameraScreen", "Capture Error", exception)
                                }
                            }
                        )
                    }
                )
            }
        } else {
            capturedBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.15f))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "IS THIS CLEAR?",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SecondaryButton(
                        text = "RETAKE",
                        onClick = {
                            capturedBitmap = null
                            frozenLandmarks = null
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    )

                    PrimaryButton(
                        text = "Continue",
                        isLoading = isSaving,
                        enabled = !isSaving,
                        onClick = {
                            capturedBitmap?.let { bmp ->
                                isSaving = true
                                viewModel.captureAndSave(
                                    bitmap = bmp,
                                    hand = handType,
                                    step = step,
                                    landmarks = frozenLandmarks,
                                    onSuccess = {

                                        onCaptureSuccess("")
                                        isSaving = false
                                    },
                                    onError = { msg ->

                                        isSaving = false
                                        Toast.makeText(
                                            context,
                                            "Failed to save: $msg",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        capturedBitmap = null
                                        frozenLandmarks = null
                                    }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}


@Composable
fun CameraHeader(handType: HandType, step: BiometricStep) {
    val stepInfo = if (step == BiometricStep.PALM) {
        "PALM"
    } else {
        val fingerNum = when (step) {
            BiometricStep.THUMB -> 1
            BiometricStep.INDEX -> 2
            BiometricStep.MIDDLE -> 3
            BiometricStep.RING -> 4
            BiometricStep.LITTLE -> 5
            else -> 0
        }
        "${step.displayName.uppercase()} $fingerNum/5"
    }

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
                text = "${if (handType == HandType.LEFT) "LEFT" else "RIGHT"} • $stepInfo",
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun ShutterButton(isEnabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clickable(enabled = isEnabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(4.dp, Color.White, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (isEnabled) SuccessGreen else Color.Transparent)
                .border(2.dp, if (isEnabled) SuccessGreen else Color.Gray, CircleShape)
        )
    }
}

fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val matrix = Matrix()
    matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}