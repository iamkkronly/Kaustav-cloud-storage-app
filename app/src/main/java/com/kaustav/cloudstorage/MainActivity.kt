package com.kaustav.cloudstorage

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var statusText: TextView
    private lateinit var loginButton: Button

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            lifecycleScope.launch {
                saveAndUploadFile(uri)
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

        statusText = findViewById(R.id.statusText)
        filesList = findViewById(R.id.filesList)
        loginButton = findViewById(R.id.loginButton)
        val uploadButton = findViewById<Button>(R.id.uploadButton)

        // Initialize Telegram Client
        try {
            TelegramClient.initialize(applicationContext)
        } catch (e: Throwable) {
            e.printStackTrace()
            Toast.makeText(this, "Init failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // Observe Auth State
        lifecycleScope.launch {
            TelegramClient.authState.collect { state ->
                when (state) {
                    is AuthState.Initial -> {
                        statusText.text = "Status: Initializing..."
                        loginButton.isEnabled = false
                    }
                    is AuthState.WaitPhoneNumber -> {
                        statusText.text = "Status: Waiting for login"
                        loginButton.text = "Login"
                        loginButton.isEnabled = true
                        loginButton.setOnClickListener {
                            showPhoneNumberDialog()
                        }
                    }
                    is AuthState.WaitCode -> {
                        statusText.text = "Status: Enter Code"
                        loginButton.text = "Enter Code"
                        loginButton.isEnabled = true
                        loginButton.setOnClickListener {
                            showCodeDialog()
                        }
                    }
                    is AuthState.WaitPassword -> {
                        statusText.text = "Status: Enter Password (2FA)"
                        loginButton.text = "Enter Password"
                        loginButton.isEnabled = true
                        loginButton.setOnClickListener {
                            showPasswordDialog()
                        }
                    }
                    is AuthState.LoggedIn -> {
                        statusText.text = "Status: Connected to Telegram"
                        loginButton.text = "Connected"
                        loginButton.isEnabled = false
                    }
                    is AuthState.LoggingOut -> {
                        statusText.text = "Status: Logging out..."
                        loginButton.isEnabled = false
                    }
                    is AuthState.Closed -> {
                        statusText.text = "Status: Client Closed"
                    }
                }
            }
        }

        uploadButton.setOnClickListener {
            if (TelegramClient.authState.value !is AuthState.LoggedIn) {
                 Toast.makeText(this, "Please login to Telegram first", Toast.LENGTH_SHORT).show()
                 return@setOnClickListener
            }
            pickFile.launch(arrayOf("*/*"))
        }

        lifecycleScope.launch {
            refreshList()
        }
    }

    private fun showPhoneNumberDialog() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_PHONE
        AlertDialog.Builder(this)
            .setTitle("Enter Phone Number")
            .setMessage("Please enter your number in international format (e.g. +123456789)")
            .setView(input)
            .setPositiveButton("Submit") { _, _ ->
                TelegramClient.sendPhoneNumber(input.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCodeDialog() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        AlertDialog.Builder(this)
            .setTitle("Enter Code")
            .setMessage("Please enter the code you received on Telegram")
            .setView(input)
            .setPositiveButton("Submit") { _, _ ->
                TelegramClient.checkCode(input.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPasswordDialog() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
        AlertDialog.Builder(this)
            .setTitle("Enter Password")
            .setMessage("Please enter your 2FA password")
            .setView(input)
            .setPositiveButton("Submit") { _, _ ->
                TelegramClient.checkPassword(input.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private suspend fun saveAndUploadFile(uri: Uri) {
        val fileName = FileUtils.displayName(contentResolver, uri) ?: "unknown"
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val localFile = File(filesDir, fileName)

        withContext(Dispatchers.IO) {
            FileUtils.copyTo(contentResolver, uri, localFile)
            val size = localFile.length()

            // Upload to Telegram
            TelegramClient.uploadFile(localFile.absolutePath) { success ->
                 runOnUiThread {
                     val msg = if (success) "Uploading to Saved Messages..." else "Upload failed (check logs)"
                     Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                 }
            }

            database.storedFileDao().insert(
                StoredFileEntity(
                    displayName = fileName,
                    mimeType = mimeType,
                    sizeBytes = size,
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
}
