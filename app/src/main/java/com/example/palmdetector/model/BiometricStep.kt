package com.example.palmdetector.model

enum class HandType { LEFT, RIGHT }

enum class BiometricStep(val displayName: String, val isPalm: Boolean) {
    PALM("Palm", true),
    THUMB("Thumb", false),
    INDEX("Index", false),
    MIDDLE("Middle", false),
    RING("Ring", false),
    LITTLE("Little", false);
}

data class CaptureState(
    val fileUri: String? = null,
    val isCompleted: Boolean = false
)