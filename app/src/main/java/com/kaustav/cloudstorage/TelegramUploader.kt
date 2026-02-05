package com.kaustav.cloudstorage

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

class TelegramUploader(
    private val botToken: String,
    private val chatId: String
) {
    private val client = OkHttpClient()

    suspend fun uploadDocument(
        contentResolver: ContentResolver,
        uri: Uri,
        fileName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val inputStream = contentResolver.openInputStream(uri)
            ?: return@withContext Result.failure(IllegalStateException("Unable to open file"))
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

        val requestBody = object : RequestBody() {
            override fun contentType() = mimeType.toMediaTypeOrNull()

            override fun writeTo(sink: BufferedSink) {
                inputStream.source().use { source ->
                    sink.writeAll(source)
                }
            }
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("document", fileName, requestBody)
            .build()

        val request = Request.Builder()
            .url("https://api.telegram.org/bot$botToken/sendDocument")
            .post(body)
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Telegram API error: ${response.code}")
                }
            }
        }
    }
}
