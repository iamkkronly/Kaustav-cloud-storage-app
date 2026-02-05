package com.kaustav.cloudstorage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StoredFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: StoredFileEntity)

    @Query("SELECT * FROM stored_files ORDER BY createdAt DESC")
    suspend fun getAll(): List<StoredFileEntity>
}
