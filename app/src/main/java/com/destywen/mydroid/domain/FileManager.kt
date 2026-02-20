package com.destywen.mydroid.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

class FileManager(private val context: Context) {
    suspend fun saveImage(uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri).use { stream0 ->
            val orientation = ExifInterface(stream0!!).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
            val degree = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            context.contentResolver.openInputStream(uri).use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                val rotated = if (degree != 0f) {
                    val matrix = Matrix().apply { postRotate(degree) }
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                } else {
                    bitmap
                }
                val resized = if (min(rotated.width, rotated.height) >= 1024) {
                    val scale = 1024f / min(rotated.width, rotated.height)
                    rotated.scale((rotated.width * scale).toInt(), (rotated.height * scale).toInt())
                } else {
                    rotated
                }

                val hasAlpha = resized.hasAlpha()
                val name = if (hasAlpha) {
                    "img_${System.currentTimeMillis()}.png"
                } else {
                    "img_${System.currentTimeMillis()}.jpg"
                }
                val format = if (hasAlpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val quality = if (hasAlpha) 100 else 80

                File(File(context.filesDir, "img"), name).outputStream().use { resized.compress(format, quality, it) }
                name
            }
        }
    }

    suspend fun deleteImage(name: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(File(context.filesDir, "img"), name)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }
}