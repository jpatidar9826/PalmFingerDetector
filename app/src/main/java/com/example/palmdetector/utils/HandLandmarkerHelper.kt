package com.example.palmdetector.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult


class HandLandmarkerHelper(
    val context: Context,
    val handLandmarkerHelperListener: LandmarkerListener
) {
    private var handLandmarker: HandLandmarker? = null

    init {
        setupHandLandmarker()
    }

    private fun setupHandLandmarker() {
        val baseOptionsBuilder = BaseOptions.builder().setModelAssetPath("hand_landmarker.task")
        val baseOptions = baseOptionsBuilder.build()

        val optionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setMinHandDetectionConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener(this::returnLivestreamResult)
            .setErrorListener(this::returnLivestreamError)

        try {
            handLandmarker = HandLandmarker.createFromOptions(context, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            handLandmarkerHelperListener.onError("Hand Landmarker failed to initialize. Try restarting the app.")
        }
    }

    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        val frameTime = SystemClock.uptimeMillis()

        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
        )
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            if (isFrontCamera) {
                postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
            }
        }
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true
        )

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        detectAsync(mpImage, frameTime)
    }

    private fun detectAsync(mpImage: MPImage, frameTime: Long) {
        handLandmarker?.detectAsync(mpImage, frameTime)
    }

    private fun returnLivestreamResult(result: HandLandmarkerResult, input: MPImage) {
        handLandmarkerHelperListener.onResults(result)
    }

    private fun returnLivestreamError(error: RuntimeException) {
        handLandmarkerHelperListener.onError(error.message ?: "Unknown error")
    }

    interface LandmarkerListener {
        fun onError(error: String)
        fun onResults(result: HandLandmarkerResult)
    }
}

object HandGeometry {


    fun isFingerExtended(landmarks: List<NormalizedLandmark>, fingerName: String): Boolean {
        return when (fingerName) {
            "THUMB" -> {
                val tTip = landmarks[4]
                val iMcp = landmarks[5]
                val distance =
                    Math.hypot((tTip.x() - iMcp.x()).toDouble(), (tTip.y() - iMcp.y()).toDouble())
                distance > 0.05
            }

            "INDEX" -> landmarks[8].y() < landmarks[6].y()
            "MIDDLE" -> landmarks[12].y() < landmarks[10].y()
            "RING" -> landmarks[16].y() < landmarks[14].y()
            "LITTLE" -> landmarks[20].y() < landmarks[18].y()
            else -> false
        }
    }

    fun countExtendedFingers(landmarks: List<NormalizedLandmark>): Int {
        var count = 0
        if (isFingerExtended(landmarks, "INDEX")) count++
        if (isFingerExtended(landmarks, "MIDDLE")) count++
        if (isFingerExtended(landmarks, "RING")) count++
        if (isFingerExtended(landmarks, "LITTLE")) count++
        return count
    }

    fun isDorsalSide(landmarks: List<NormalizedLandmark>, isLeftHand: Boolean): Boolean {
        val wrist = landmarks[0]
        val indexMcp = landmarks[5]
        val littleMcp = landmarks[17]

        val v1x = indexMcp.x() - wrist.x()
        val v1y = indexMcp.y() - wrist.y()

        val v2x = littleMcp.x() - wrist.x()
        val v2y = littleMcp.y() - wrist.y()


        val crossProductZ = (v1x * v2y) - (v1y * v2x)


        return if (isLeftHand) {
            crossProductZ > 0
        } else {
            crossProductZ < 0 //  back of the right hand
        }
    }
}