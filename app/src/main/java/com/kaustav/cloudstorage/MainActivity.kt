package com.kaustav.cloudstorage

<<<<<<< codex/create-android-apk-for-kaustav-cloud-storage-6s1s6r
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var filesList: TextView
    private var isLoggedIn = false
    private val uploadedFiles = mutableListOf<String>()

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            if (!isLoggedIn) {
                Toast.makeText(
                    this,
                    "Login with Telegram to upload files to Saved Messages.",
                    Toast.LENGTH_LONG
                ).show()
                return@registerForActivityResult
            }
            val displayName = FileUtils.displayName(contentResolver, uri) ?: "unknown file"
            uploadedFiles.add(displayName)
            refreshList()
            Toast.makeText(
                this,
                "Uploading \"$displayName\" to Telegram Saved Messages (demo).",
                Toast.LENGTH_LONG
            ).show()
=======
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
>>>>>>> main
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

<<<<<<< codex/create-android-apk-for-kaustav-cloud-storage-6s1s6r
        statusText = findViewById(R.id.statusText)
        filesList = findViewById(R.id.filesList)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val uploadButton = findViewById<Button>(R.id.uploadButton)

        loginButton.setOnClickListener {
            isLoggedIn = true
            statusText.text = "Status: Connected to Telegram (demo)"
            Toast.makeText(
                this,
                "Telegram login simulated. Uploads will go to Saved Messages.",
                Toast.LENGTH_LONG
            ).show()
        }

=======
        database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "kaustav.db")
            .fallbackToDestructiveMigration()
            .build()

        filesList = findViewById(R.id.filesList)
        val uploadButton = findViewById<Button>(R.id.uploadButton)

>>>>>>> main
        uploadButton.setOnClickListener {
            pickFile.launch(arrayOf("*/*"))
        }

<<<<<<< codex/create-android-apk-for-kaustav-cloud-storage-6s1s6r
        refreshList()
    }

    private fun refreshList() {
        val formatted = if (uploadedFiles.isEmpty()) {
            "No uploads yet. Files are not stored offline; they are sent to Telegram Saved Messages."
        } else {
            uploadedFiles.joinToString("\n") { "• $it" }
=======
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
>>>>>>> main
        }
        filesList.text = formatted
    }

<<<<<<< codex/create-android-apk-for-kaustav-cloud-storage-6s1s6r
=======
    object IntentFlags {
        const val READ = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

>>>>>>> main
    companion object {
        const val TELEGRAM_API_ID = "20110837"
        const val TELEGRAM_API_HASH = "b9658b136c2b71af2bdb7497649ace5c"
    }
}
