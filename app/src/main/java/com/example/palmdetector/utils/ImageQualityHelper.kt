package com.example.palmdetector.utils

import androidx.camera.core.ImageProxy

object ImageQualityHelper {

    const val LUMINOSITY_MIN = 50.0
    const val LUMINOSITY_MAX = 200.0
    const val BLUR_THRESHOLD = 500.0


    fun getLuminosity(image: ImageProxy): Double {
        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        buffer.rewind()

        var sum = 0L
        for (i in 0 until data.size step 4) {
            val r = data[i].toInt() and 0xFF
            val g = data[i + 1].toInt() and 0xFF
            val b = data[i + 2].toInt() and 0xFF
            sum += (0.299 * r + 0.587 * g + 0.114 * b).toLong()
        }
        val pixelCount = data.size / 4
        return if (pixelCount > 0) sum.toDouble() / pixelCount else 0.0
    }


    fun getBlurScore(image: ImageProxy): Double {

        val step = 4
        val buffer = image.planes[0].buffer
        val width = image.width
        val height = image.height
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        buffer.rewind()

        var sum = 0.0
        var sqSum = 0.0
        var count = 0

        for (y in 1 until height - 1 step step) {
            for (x in 1 until width - 1 step step) {
                val index = y * width + x
                val pixel = (data[index].toInt() and 0xFF)

                val left = (data[index - 1].toInt() and 0xFF)
                val right = (data[index + 1].toInt() and 0xFF)
                val up = (data[index - width].toInt() and 0xFF)
                val down = (data[index + width].toInt() and 0xFF)

                val laplacian = (4 * pixel) - (left + right + up + down)

                sum += laplacian
                sqSum += (laplacian * laplacian)
                count++
            }
        }

        val mean = sum / count
        val variance = (sqSum / count) - (mean * mean)
        return variance
    }
}