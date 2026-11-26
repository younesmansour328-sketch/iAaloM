package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ProjectEntity
import com.example.data.model.TextOverlayEntity
import com.example.data.model.VideoClipEntity

@Database(
    entities = [
        ProjectEntity::class,
        VideoClipEntity::class,
        TextOverlayEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MelonDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: MelonDatabase? = null

        fun getInstance(context: Context): MelonDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MelonDatabase::class.java,
                    "melon_editor.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
