package com.example.palmdetector.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.palmdetector.model.BiometricStep
import com.example.palmdetector.model.HandType
import com.example.palmdetector.model.SimplePoint
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BiometricFileHelper {
    private const val TAG = "BiometricFileHelper"
    private const val FOLDER_NAME = "Finger Data"

    private const val TARGET_WIDTH = 1024

    fun getOutputDirectory(context: Context): File {
        val mediaDir = context.getExternalFilesDir(null)?.let {
            File(it, FOLDER_NAME).apply {
                if (!exists()) mkdirs()
            }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else File(
            context.filesDir,
            FOLDER_NAME
        ).apply { mkdirs() }
    }

    fun saveImage(context: Context, bitmap: Bitmap, hand: HandType, step: BiometricStep): String {

        deleteOldCaptures(context, hand, step)


        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val handPrefix = if (hand == HandType.LEFT) "Left_Hand" else "Right_Hand"

        val fileName = if (step == BiometricStep.PALM) {
            "${handPrefix}_$timestamp.jpg"
        } else {
            "${handPrefix}_${step.displayName}_Finger_$timestamp.jpg"
        }

        val file = File(getOutputDirectory(context), fileName)

        try {
            val resizedBitmap = resizeBitmap(bitmap, TARGET_WIDTH)

            FileOutputStream(file).use { out ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            Log.d(TAG, "Saved Optimized Image: ${file.absolutePath} (${file.length() / 1024} KB)")
            return file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "FAILED to save image", e)
            throw e
        }
    }

    private fun resizeBitmap(source: Bitmap, maxWidth: Int): Bitmap {
        if (source.width <= maxWidth) return source

        val aspectRatio = source.height.toFloat() / source.width.toFloat()
        val targetHeight = (maxWidth * aspectRatio).toInt()

        return Bitmap.createScaledBitmap(source, maxWidth, targetHeight, true)
    }


    private fun deleteOldCaptures(context: Context, hand: HandType, step: BiometricStep) {
        val dir = getOutputDirectory(context)
        val handPrefix = if (hand == HandType.LEFT) "Left_Hand" else "Right_Hand"

        dir.listFiles()?.forEach { file ->
            val name = file.name
            if (name.startsWith(handPrefix)) {
                if (step == BiometricStep.PALM && !name.contains("Finger")) {
                    file.delete()
                    val jsonName = name.replace(".jpg", ".json").replace(".png", ".json")
                    File(dir, jsonName).delete()
                } else if (step != BiometricStep.PALM && name.contains("Finger") && name.contains(
                        step.displayName
                    )
                ) {
                    file.delete()
                }
            }
        }
    }

    fun savePalmData(context: Context, hand: HandType, landmarks: List<SimplePoint>) {
        val dir = getOutputDirectory(context)
        val handPrefix = if (hand == HandType.LEFT) "Left_Hand" else "Right_Hand"


        val palmImage = dir.listFiles()?.find {
            it.name.startsWith(handPrefix) && !it.name.contains("Finger") && (it.name.endsWith(".jpg") || it.name.endsWith(
                ".png"
            ))
        } ?: return


        val jsonName = palmImage.name.substringBeforeLast(".") + ".json"
        val jsonFile = File(dir, jsonName)

        val root = JSONObject()
        val array = JSONArray()
        landmarks.forEach {
            val p = JSONObject().put("x", it.x.toDouble()).put("y", it.y.toDouble())
            array.put(p)
        }
        root.put("landmarks", array)

        jsonFile.writeText(root.toString())
    }

    fun loadPalmData(context: Context, hand: HandType): List<SimplePoint>? {
        val dir = getOutputDirectory(context)
        val handPrefix = if (hand == HandType.LEFT) "Left_Hand" else "Right_Hand"

        val jsonFile = dir.listFiles()?.find {
            it.name.startsWith(handPrefix) && !it.name.contains("Finger") && it.name.endsWith(".json")
        } ?: return null

        return try {
            val root = JSONObject(jsonFile.readText())
            val array = root.getJSONArray("landmarks")
            val list = mutableListOf<SimplePoint>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(SimplePoint(obj.getDouble("x").toFloat(), obj.getDouble("y").toFloat()))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }
}