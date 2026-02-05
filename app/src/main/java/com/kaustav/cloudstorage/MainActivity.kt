package com.kaustav.cloudstorage

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var filesList: TextView
    private lateinit var tokenInput: EditText
    private lateinit var chatIdInput: EditText
    private var isLoggedIn = false
    private val uploadedFiles = mutableListOf<String>()
    private var uploader: TelegramUploader? = null

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            if (!isLoggedIn) {
                Toast.makeText(
                    this,
                    "Connect to Telegram to upload files.",
                    Toast.LENGTH_LONG
                ).show()
                return@registerForActivityResult
            }
            val displayName = FileUtils.displayName(contentResolver, uri) ?: "unknown file"
            val telegramUploader = uploader ?: return@registerForActivityResult
            lifecycleScope.launch {
                val result = telegramUploader.uploadDocument(
                    contentResolver = contentResolver,
                    uri = uri,
                    fileName = displayName
                )
                if (result.isSuccess) {
                    uploadedFiles.add(displayName)
                    refreshList()
                    Toast.makeText(
                        this@MainActivity,
                        "Uploaded \"$displayName\" to Telegram.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Upload failed: ${result.exceptionOrNull()?.message ?: "unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        filesList = findViewById(R.id.filesList)
        tokenInput = findViewById(R.id.tokenInput)
        chatIdInput = findViewById(R.id.chatIdInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val uploadButton = findViewById<Button>(R.id.uploadButton)

        loginButton.setOnClickListener {
            val token = tokenInput.text.toString().trim()
            val chatId = chatIdInput.text.toString().trim()
            if (token.isBlank() || chatId.isBlank()) {
                Toast.makeText(
                    this,
                    "Enter your bot token and chat ID to connect.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            uploader = TelegramUploader(token, chatId)
            isLoggedIn = true
            statusText.text = "Status: Connected to Telegram"
        }

        uploadButton.setOnClickListener {
            pickFile.launch(arrayOf("*/*"))
        }

        refreshList()
    }

    private fun refreshList() {
        val formatted = if (uploadedFiles.isEmpty()) {
            "No uploads yet. Files are sent to Telegram and not stored offline on this device."
        } else {
            uploadedFiles.joinToString("\n") { "• $it" }
        }
        filesList.text = formatted
    }

    companion object {
        const val TELEGRAM_API_ID = "20110837"
        const val TELEGRAM_API_HASH = "b9658b136c2b71af2bdb7497649ace5c"
    }
}
