package com.example.palmdetector.utils

import android.graphics.PointF


object VerificationHelper {

    fun calculateSimilarity(
        storedLandmarks: List<PointF>,
        liveLandmarks: List<PointF>
    ): Float {
        if (storedLandmarks.size != 21 || liveLandmarks.size != 21) return 0f

        val storedScale = getDistance(storedLandmarks[0], storedLandmarks[9])
        val liveScale = getDistance(liveLandmarks[0], liveLandmarks[9])

        if (storedScale == 0f || liveScale == 0f) return 0f

        var totalError = 0f

        val fingerTips = listOf(4, 8, 12, 16, 20)
        val fingerBases = listOf(2, 5, 9, 13, 17)

        for (i in fingerTips.indices) {
            val tipIdx = fingerTips[i]
            val baseIdx = fingerBases[i]

            val storedFingerLen = getDistance(storedLandmarks[tipIdx], storedLandmarks[baseIdx])
            val liveFingerLen = getDistance(liveLandmarks[tipIdx], liveLandmarks[baseIdx])

            val storedRatio = storedFingerLen / storedScale
            val liveRatio = liveFingerLen / liveScale

            totalError += Math.abs(storedRatio - liveRatio)
        }

        val avgError = totalError / 5f

        return (1.0f - (avgError * 5)).coerceIn(0.0f, 1.0f)
    }

    private fun getDistance(p1: PointF, p2: PointF): Float {
        return Math.sqrt(
            Math.pow(
                (p2.x - p1.x).toDouble(),
                2.0
            ) + Math.pow((p2.y - p1.y).toDouble(), 2.0)
        ).toFloat()
    }
}