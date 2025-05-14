package com.g2.chatroom.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedImageDao {
    @Insert
    suspend fun insert(savedImage: SavedImage): Long

    @Query("SELECT * FROM saved_images ORDER BY savedAt DESC")
    fun getAllSavedImages(): Flow<List<SavedImage>>

    @Query("SELECT * FROM saved_images WHERE messageId = :messageId")
    suspend fun getImageForMessage(messageId: String): SavedImage?

    @Query("SELECT * FROM saved_images WHERE originalUrl = :url")
    suspend fun getImageByUrl(url: String): SavedImage?

    @Delete
    suspend fun delete(savedImage: SavedImage)
}
