package com.example.poco.data.local.datasourceimpl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.example.poco.data.local.datasource.PetPhotoDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PetPhotoDataSourceImpl @Inject constructor(@ApplicationContext private val context: Context) :
    PetPhotoDataSource {
    private val photoDir: File get() = File(context.filesDir, "pet_photos").apply { mkdirs() }

    override suspend fun importPhoto(uri: String): String = withContext(Dispatchers.IO) {
        val source = Uri.parse(uri)
        val bitmap = decodeScaled(source) ?: error("사진을 읽을 수 없어요")
        val rotated = bitmap.rotateBy(readRotation(source))
        val file = File(photoDir, "${UUID.randomUUID()}.jpg")
        file.outputStream().use { rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        file.absolutePath
    }

    override suspend fun deletePhoto(path: String) {
        withContext(Dispatchers.IO) {
            val file = File(path)
            // 앱 사진 폴더 안의 파일만 지운다
            if (file.parentFile == photoDir) file.delete()
        }
    }

    /** 원본이 커도 메모리를 아끼도록 긴 변 [MAX_SIDE_PX] 근처로 줄여서 읽는다 */
    private fun decodeScaled(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE_PX) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded =
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return null
        val scale = MAX_SIDE_PX.toFloat() / maxOf(decoded.width, decoded.height)
        return if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt(),
                (decoded.height * scale).toInt(),
                true
            )
        } else {
            decoded
        }
    }

    private fun readRotation(uri: Uri): Int = runCatching {
        context.contentResolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    }.getOrDefault(0)

    private fun Bitmap.rotateBy(degrees: Int): Bitmap {
        if (degrees == 0) return this
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private companion object {
        const val MAX_SIDE_PX = 512
        const val JPEG_QUALITY = 88
    }
}
