package com.kaustav.cloudstorage

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream

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

    fun readBytes(contentResolver: ContentResolver, uri: Uri): ByteArray {
        val outputStream = ByteArrayOutputStream()
        contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytes = input.read(buffer)
            while (bytes >= 0) {
                outputStream.write(buffer, 0, bytes)
                bytes = input.read(buffer)
            }
        }
        return outputStream.toByteArray()
    }
}
