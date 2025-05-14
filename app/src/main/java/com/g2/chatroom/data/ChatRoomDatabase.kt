package com.g2.chatroom.data

import SavedImage
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SavedImage::class], version = 1, exportSchema = false)
abstract class ChatRoomDatabase : RoomDatabase() {
    abstract fun savedImageDao(): SavedImageDao

    companion object {
        @Volatile private var INSTANCE: ChatRoomDatabase? = null

        fun getDatabase(context: Context): ChatRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatRoomDatabase::class.java,
                    "chatroom_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
