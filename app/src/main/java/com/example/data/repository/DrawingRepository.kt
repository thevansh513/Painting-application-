package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.database.DrawingDao
import com.example.data.database.DrawingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class DrawingRepository(
    private val context: Context,
    private val drawingDao: DrawingDao
) {
    val allDrawings: Flow<List<DrawingEntity>> = drawingDao.getAllDrawings()

    suspend fun getDrawingById(id: Long): DrawingEntity? = withContext(Dispatchers.IO) {
        drawingDao.getDrawingById(id)
    }

    suspend fun saveDrawing(
        id: Long,
        title: String,
        strokesJson: String,
        bitmap: Bitmap
    ): Long = withContext(Dispatchers.IO) {
        val fileName = "drawing_${System.currentTimeMillis()}.png"
        val appFilesDir = File(context.filesDir, "drawings")
        if (!appFilesDir.exists()) {
            appFilesDir.mkdirs()
        }
        val localFile = File(appFilesDir, fileName)
        FileOutputStream(localFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val drawingEntity = if (id == 0L) {
            DrawingEntity(
                title = title,
                filePath = localFile.absolutePath,
                strokesJson = strokesJson,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        } else {
            // If updating, we can try to delete the old image file to save space
            val existing = drawingDao.getDrawingById(id)
            if (existing != null) {
                try {
                    val oldFile = File(existing.filePath)
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            DrawingEntity(
                id = id,
                title = title,
                filePath = localFile.absolutePath,
                strokesJson = strokesJson,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }

        if (id == 0L) {
            drawingDao.insertDrawing(drawingEntity)
        } else {
            drawingDao.updateDrawing(drawingEntity)
            id
        }
    }

    suspend fun updateDrawingTitle(id: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val existing = drawingDao.getDrawingById(id)
        if (existing != null) {
            val updated = existing.copy(
                title = newTitle,
                updatedAt = System.currentTimeMillis()
            )
            drawingDao.updateDrawing(updated)
        }
    }

    suspend fun deleteDrawing(id: Long) = withContext(Dispatchers.IO) {
        val existing = drawingDao.getDrawingById(id)
        if (existing != null) {
            try {
                val file = File(existing.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            drawingDao.deleteDrawingById(id)
        }
    }

    suspend fun exportToPublicGallery(bitmap: Bitmap, title: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val displayName = "${title.replace(" ", "_")}_${System.currentTimeMillis()}.png"
            val imageDetails = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PaintingEditor")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageDetails)
            if (imageUri != null) {
                resolver.openOutputStream(imageUri).use { out ->
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    } else {
                        return@withContext Result.failure(Exception("Could not open output stream for gallery"))
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    imageDetails.clear()
                    imageDetails.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, imageDetails, null, null)
                }
                Result.success("Saved to Gallery/Pictures/PaintingEditor")
            } else {
                Result.failure(Exception("Failed to generate MediaStore URI"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getShareUriForBitmap(bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        try {
            val sharedDir = File(context.cacheDir, "shared_images")
            if (!sharedDir.exists()) {
                sharedDir.mkdirs()
            }
            val tempFile = File(sharedDir, "shared_drawing.png")
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(
                context,
                "com.aistudio.paintingeditor.vpkrtw.fileprovider",
                tempFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
