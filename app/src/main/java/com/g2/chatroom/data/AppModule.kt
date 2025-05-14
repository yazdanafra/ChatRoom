package com.g2.chatroom.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChatRoomDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            ChatRoomDatabase::class.java,
            "chatroom_database"
        ).build()
    }

    @Provides
    fun provideSavedImageDao(db: ChatRoomDatabase): SavedImageDao {
        return db.savedImageDao()
    }

    @Provides
    fun provideImageRepository(dao: SavedImageDao): ImageRepository {
        return ImageRepository(dao)
    }
}
