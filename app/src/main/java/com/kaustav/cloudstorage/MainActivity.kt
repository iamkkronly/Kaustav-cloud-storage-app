package com.kaustav.cloudstorage

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
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        uploadButton.setOnClickListener {
            pickFile.launch(arrayOf("*/*"))
        }

        refreshList()
    }

    private fun refreshList() {
        val formatted = if (uploadedFiles.isEmpty()) {
            "No uploads yet. Files are not stored offline; they are sent to Telegram Saved Messages."
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
