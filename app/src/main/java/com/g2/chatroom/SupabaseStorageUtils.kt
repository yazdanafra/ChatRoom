package com.g2.chatroom

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import java.util.UUID

class SupabaseStorageUtils(val context: Context) {

    val supabase = createSupabaseClient(
        "https://btvpprsnjvetmysyqipz.supabase.co",
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJ0dnBwcnNuanZldG15c3lxaXB6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDYzNjk5NDIsImV4cCI6MjA2MTk0NTk0Mn0.StjJIBwqQe8Nidl_87FVukumgfkqaMF7_FOTuZ8JT7M"
    ) {
        install(Storage)
    }

    suspend fun uploadImage(uri: Uri): String? {
        try {
            val extension = uri.path?.substringAfterLast(".") ?: "jpg"
            val fileName = "${UUID.randomUUID()}.$extension"
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            supabase.storage.from(BUCKET_NAME).upload(fileName, inputStream.readBytes())
            val publicUrl = supabase.storage.from(BUCKET_NAME).publicUrl(fileName)
            return publicUrl
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }


    companion object {
        const val BUCKET_NAME = "chatroom-images"
    }
}