package com.g2.chatroom.data

import SavedImage
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.util.UUID
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ImageRepository(private val savedImageDao: SavedImageDao) {

    val allSavedImages = savedImageDao.getAllSavedImages()

    suspend fun saveImage(context: Context, imageUrl: String, messageId: String?): SavedImage? {
        val existing = savedImageDao.getImageByUrl(imageUrl)
        if (existing != null) return existing

        val fileName = "${UUID.randomUUID()}.jpg"
        val localUri = saveImageFromUrl(context, imageUrl, fileName) ?: return null

        val savedImage = SavedImage(
            originalUrl = imageUrl,
            localPath = localUri.toString(),
            fileName = fileName,
            messageId = messageId
        )
        val id = savedImageDao.insert(savedImage)
        return savedImage.copy(id = id)
    }

    suspend fun deleteImage(savedImage: SavedImage) {
        savedImage.localPath.toUri().path?.let { File(it).delete() }
        savedImageDao.delete(savedImage)
    }

    suspend fun saveImageFromUrl(context: Context, imageUrl: String, fileName: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ChatRoom")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                val conn = URL(imageUrl).openConnection() as HttpURLConnection
                conn.connect()
                val input = conn.inputStream
                val output = FileOutputStream(file)
                input.use { it.copyTo(output) }
                return@withContext Uri.fromFile(file)
            } catch (e: Exception) {
                Log.e("ImageSave", "Error saving image", e)
                null
            }
        }
    }

}
