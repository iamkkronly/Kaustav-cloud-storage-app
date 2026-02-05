package com.kaustav.cloudstorage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stored_files")
data class StoredFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val storedPath: String,
    val createdAt: Long
)
