package com.g2.chatroom.data

import android.content.ContentValues
import com.g2.chatroom.data.SavedImage
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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

    suspend fun saveImageFromUrl(
        context: Context,
        imageUrl: String,
        fileName: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            // 1) Download into app-private cache
            val cacheDir = File(context.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), "ChatRoom")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val temp = File(cacheDir, fileName)
            URL(imageUrl).openConnection().let { conn ->
                (conn as HttpURLConnection).run {
                    connect()
                    inputStream.use { input ->
                        FileOutputStream(temp).use { output ->
                            input.copyTo(output)
                        }
                    }
                    disconnect()
                }
            }

            // 2) On Android Q+ insert into MediaStore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/ChatRoom"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )!!

                // copy bytes from temp file into the public MediaStore
                resolver.openOutputStream(uri)?.use { out ->
                    temp.inputStream().use { it.copyTo(out) }
                }

                // release “pending” flag so it appears
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                return@withContext uri
            }

            // 3) On older devices just return the file:// URI
            return@withContext Uri.fromFile(temp)
        }
        catch (e: Exception) {
            Log.e("ImageSave", "Error saving image", e)
            null
        }
    }



}
