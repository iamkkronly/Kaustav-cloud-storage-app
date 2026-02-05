package com.kaustav.cloudstorage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [StoredFileEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storedFileDao(): StoredFileDao
}
