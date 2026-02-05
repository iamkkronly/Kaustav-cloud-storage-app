package com.kaustav.cloudstorage

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object FileUtils {
    fun displayName(contentResolver: ContentResolver, uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null) ?: return null
        cursor.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex == -1) return null
            if (it.moveToFirst()) {
                return it.getString(nameIndex)
            }
        }
        return null
    }

    fun copyTo(contentResolver: ContentResolver, uri: Uri, destFile: File) {
        contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
