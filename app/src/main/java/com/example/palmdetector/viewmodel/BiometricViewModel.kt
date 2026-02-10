package com.example.palmdetector.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.PointF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.palmdetector.model.BiometricStep
import com.example.palmdetector.model.CaptureState
import com.example.palmdetector.model.HandType
import com.example.palmdetector.model.SimplePoint
import com.example.palmdetector.utils.BiometricFileHelper
import com.example.palmdetector.utils.HandGeometry
import com.example.palmdetector.utils.ImageQualityHelper
import com.example.palmdetector.utils.VerificationHelper
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BiometricViewModel(application: Application) : AndroidViewModel(application) {


    private val _navigationEvent = MutableSharedFlow<Boolean>()

    private val _leftHandState = MutableStateFlow(
        BiometricStep.values().associateWith { CaptureState() }
    )
    val leftHandState = _leftHandState.asStateFlow()


    private val _rightHandState = MutableStateFlow(
        BiometricStep.values().associateWith { CaptureState() }
    )
    val rightHandState = _rightHandState.asStateFlow()

    val isBiometricRegistered: StateFlow<Boolean> =
        combine(leftHandState, rightHandState) { left, right ->
            left[BiometricStep.PALM]?.isCompleted == true || right[BiometricStep.PALM]?.isCompleted == true
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)


    private val _cameraFeedback = MutableStateFlow("Align your hand")

    private val _lightingState = MutableStateFlow("Normal")

    private val _isImageClear = MutableStateFlow(true)

    private val _isCaptureEnabled = MutableStateFlow(false)
    val isCaptureEnabled = _isCaptureEnabled.asStateFlow()

    val currentFeedback =
        combine(_cameraFeedback, _lightingState, _isImageClear) { handMsg, lightMsg, isClear ->
            when {
                lightMsg == "Too Dark" -> "Lighting is too Low. Find a brighter spot."
                lightMsg == "Too Bright" -> "Lighting is too Bright. Avoid direct light."
                !isClear -> "Image is Blurry. Hold still."
                else -> handMsg
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Align your hand")

    private val _palmReferenceData = MutableStateFlow<Map<HandType, List<PointF>>>(emptyMap())


    enum class VerifyStatus { IDLE, PROCESSING, SUCCESS, FAIL }

    private val _verificationStatus = MutableStateFlow(VerifyStatus.IDLE)
    val verificationStatus = _verificationStatus.asStateFlow()

    private val _verificationMessage = MutableStateFlow("Scan your hand...")
    val verificationMessage = _verificationMessage.asStateFlow()

    private var isVerificationRunning = false

    init {
        refreshState()
    }

    fun refreshState() {
        val context = getApplication<Application>().applicationContext
        val dir = BiometricFileHelper.getOutputDirectory(context)


        dir.listFiles()?.forEach { file ->
            val name = file.name

            val hand = if (name.startsWith("Left_Hand")) HandType.LEFT
            else if (name.startsWith("Right_Hand")) HandType.RIGHT
            else null

            if (hand != null) {
                val step = when {
                    name.contains("Thumb") -> BiometricStep.THUMB
                    name.contains("Index") -> BiometricStep.INDEX
                    name.contains("Middle") -> BiometricStep.MIDDLE
                    name.contains("Ring") -> BiometricStep.RING
                    name.contains("Little") -> BiometricStep.LITTLE
                    !name.contains("Finger") && (name.endsWith(".jpg") || name.endsWith(".png")) -> BiometricStep.PALM
                    else -> null
                }

                if (step != null) {
                    val targetFlow = if (hand == HandType.LEFT) _leftHandState else _rightHandState
                    targetFlow.update { map ->
                        map.toMutableMap().apply {
                            this[step] = CaptureState(file.absolutePath, true)
                        }
                    }
                }
            }
        }

        val leftData = BiometricFileHelper.loadPalmData(context, HandType.LEFT)
        if (leftData != null) savePalmReferenceInRam(HandType.LEFT, leftData)

        val rightData = BiometricFileHelper.loadPalmData(context, HandType.RIGHT)
        if (rightData != null) savePalmReferenceInRam(HandType.RIGHT, rightData)
    }

    private fun savePalmReferenceInRam(hand: HandType, points: List<SimplePoint>) {
        val currentMap = _palmReferenceData.value.toMutableMap()
        val pointFs = points.map { PointF(it.x, it.y) }
        currentMap[hand] = pointFs
        _palmReferenceData.value = currentMap
    }

    fun captureAndSave(
        bitmap: Bitmap,
        hand: HandType,
        step: BiometricStep,
        landmarks: List<SimplePoint>?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext

                val path = BiometricFileHelper.saveImage(context, bitmap, hand, step)

                if (step == BiometricStep.PALM && landmarks != null) {
                    BiometricFileHelper.savePalmData(context, hand, landmarks)
                    val pointFs = landmarks.map { PointF(it.x, it.y) }
                    val currentMap = _palmReferenceData.value.toMutableMap()
                    currentMap[hand] = pointFs
                    _palmReferenceData.value = currentMap
                }

                val targetFlow = if (hand == HandType.LEFT) _leftHandState else _rightHandState
                targetFlow.update { map ->
                    map.toMutableMap().apply {
                        this[step] = CaptureState(fileUri = path, isCompleted = true)
                    }
                }

                refreshState()

                withContext(Dispatchers.Main) {
                    onSuccess()
                }

                _navigationEvent.emit(true)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun updateQualityMetrics(luminosity: Double, blurScore: Double) {
        _lightingState.value = when {
            luminosity < ImageQualityHelper.LUMINOSITY_MIN -> "Too Dark"
            luminosity > ImageQualityHelper.LUMINOSITY_MAX -> "Too Bright"
            else -> "Normal"
        }

        val isClear = blurScore > ImageQualityHelper.BLUR_THRESHOLD
        _isImageClear.value = isClear

        if (_lightingState.value != "Normal" || !isClear) {
            _isCaptureEnabled.value = false
        }
    }

    fun processHandResult(
        result: HandLandmarkerResult,
        requiredHand: HandType,
        requiredStep: BiometricStep
    ) {

        if (_lightingState.value != "Normal" || !_isImageClear.value) {
            _isCaptureEnabled.value = false
            return
        }

        if (result.landmarks().isEmpty()) {
            _cameraFeedback.value = "Show your ${requiredHand.name} ${requiredStep.displayName}"
            _isCaptureEnabled.value = false
            return
        }

        val landmarks = result.landmarks()[0]
        val handednessCategory = result.handednesses()[0][0]
        val detectedLabel = handednessCategory.categoryName()

        val isCorrectHand =
            if (requiredHand == HandType.LEFT) detectedLabel == "Left" else detectedLabel == "Right"

        if (!isCorrectHand) {
            _cameraFeedback.value = "Wrong Hand! Please show ${requiredHand.name}."
            _isCaptureEnabled.value = false
            return
        }

        val isLeftHandDetected = detectedLabel == "Left"

        val isDorsal = HandGeometry.isDorsalSide(landmarks, isLeftHandDetected)

        if (!isDorsal) {
            if (requiredStep == BiometricStep.PALM) {
                _cameraFeedback.value =
                    "Palm dorsal side detected, minutiae points won’t be extracted."
            } else {
                _cameraFeedback.value = "Finger dorsal side detected, please show palm side."
            }
            _isCaptureEnabled.value = false
            return
        }



        if (requiredStep == BiometricStep.PALM) {
            val extendedCount = HandGeometry.countExtendedFingers(landmarks)
            if (extendedCount < 3) {
                _cameraFeedback.value = "Open your hand fully."
                _isCaptureEnabled.value = false
            } else {
                _cameraFeedback.value = "Perfect Palm! Hold still."
                _isCaptureEnabled.value = true
            }
        } else {
            val targetFingerName = requiredStep.name
            val isTargetExtended = HandGeometry.isFingerExtended(landmarks, targetFingerName)

            if (!isTargetExtended) {
                _cameraFeedback.value = "Extend your ${requiredStep.displayName} finger."
                _isCaptureEnabled.value = false
            } else {
                val totalExtended = HandGeometry.countExtendedFingers(landmarks)
                if (totalExtended > 2 && requiredStep != BiometricStep.THUMB) {
                    _cameraFeedback.value = "Isolate the ${requiredStep.displayName}. Curl others."
                    _isCaptureEnabled.value = false
                } else {
                    _cameraFeedback.value = "Perfect ${requiredStep.displayName}!"
                    _isCaptureEnabled.value = true
                }
            }
        }
    }

    fun resetVerification() {
        _verificationStatus.value = VerifyStatus.IDLE
        _verificationMessage.value = "Scan your hand..."
        isVerificationRunning = false
    }

    fun verifyUser(result: HandLandmarkerResult) {
        if (isVerificationRunning) return

        if (result.landmarks().isEmpty()) {
            _verificationMessage.value = "Scan your hand..."
            return
        }

        val handedness = result.handednesses()[0][0].categoryName()
        val handType = if (handedness == "Left") HandType.LEFT else HandType.RIGHT

        isVerificationRunning = true
        _verificationStatus.value = VerifyStatus.PROCESSING
        _verificationMessage.value = "$handedness Hand Detected. Verifying..."

        viewModelScope.launch {
            delay(3000)
            val storedPoints = _palmReferenceData.value[handType]

            if (storedPoints == null) {
                _verificationStatus.value = VerifyStatus.FAIL
                _verificationMessage.value = "FAILED: No $handedness Hand Registered"
            } else {
                val livePoints = result.landmarks()[0].map { PointF(it.x(), it.y()) }

                val score = VerificationHelper.calculateSimilarity(storedPoints, livePoints)

                if (score > 0.85f) {
                    _verificationStatus.value = VerifyStatus.SUCCESS
                    _verificationMessage.value = "VERIFIED: Identity Confirmed"
                } else {
                    _verificationStatus.value = VerifyStatus.FAIL
                    _verificationMessage.value = "FAILED: Fingerprints Do Not Match"
                }
            }
        }
    }
}