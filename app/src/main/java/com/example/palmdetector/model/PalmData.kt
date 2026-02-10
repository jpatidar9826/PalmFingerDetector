package com.example.palmdetector.model

import kotlinx.serialization.Serializable

@Serializable
data class PalmData(
    val handType: String,
    val landmarks: List<SimplePoint>
)

@Serializable
data class SimplePoint(val x: Float, val y: Float)