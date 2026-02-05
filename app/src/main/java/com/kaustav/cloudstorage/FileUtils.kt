package com.kaustav.cloudstorage

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

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

}
