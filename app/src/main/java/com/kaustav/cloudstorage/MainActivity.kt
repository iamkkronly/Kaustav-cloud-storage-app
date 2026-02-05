package com.kaustav.cloudstorage

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var database: AppDatabase
    private lateinit var filesList: TextView

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                IntentFlags.READ
            )
            lifecycleScope.launch {
                saveFile(uri)
                refreshList()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "kaustav.db")
            .fallbackToDestructiveMigration()
            .build()

        filesList = findViewById(R.id.filesList)
        val uploadButton = findViewById<Button>(R.id.uploadButton)

        uploadButton.setOnClickListener {
            pickFile.launch(arrayOf("*/*"))
        }

        lifecycleScope.launch {
            refreshList()
        }
    }

    private suspend fun saveFile(uri: Uri) {
        val fileName = FileUtils.displayName(contentResolver, uri) ?: "unknown"
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val bytes = FileUtils.readBytes(contentResolver, uri)
        val localFile = File(filesDir, fileName)
        withContext(Dispatchers.IO) {
            localFile.outputStream().use { it.write(bytes) }
            database.storedFileDao().insert(
                StoredFileEntity(
                    displayName = fileName,
                    mimeType = mimeType,
                    sizeBytes = bytes.size.toLong(),
                    storedPath = localFile.absolutePath,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun refreshList() {
        val items = withContext(Dispatchers.IO) { database.storedFileDao().getAll() }
        val formatted = if (items.isEmpty()) {
            "No files stored yet."
        } else {
            items.joinToString("\n\n") { item ->
                val size = NumberFormat.getInstance().format(item.sizeBytes)
                val date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    .format(Date(item.createdAt))
                "${item.displayName}\n${item.mimeType} • ${size} bytes\nSaved: $date"
            }
        }
        filesList.text = formatted
    }

    object IntentFlags {
        const val READ = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    companion object {
        const val TELEGRAM_API_ID = "20110837"
        const val TELEGRAM_API_HASH = "b9658b136c2b71af2bdb7497649ace5c"
    }
}
