package com.example.securesphere

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.GridView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.Executor

class HiddenImagesActivity : AppCompatActivity() {

    private lateinit var gridView: GridView
    private lateinit var vaultFolder: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hidden_images)

        // Set up our secure hidden folder inside internal storage path
        vaultFolder = File(filesDir, "secure_vault_images")
        if (!vaultFolder.exists()) {
            vaultFolder.mkdirs()
        }

        val btnImport = findViewById<Button>(R.id.btnImportImage)
        gridView = findViewById<GridView>(R.id.gridViewImages)

        // START BIOMETRIC CHECK IMMEDIATELY JUST LIKE THE PASSWORD MANAGER
        checkBiometricAuth()

        // Gallery Import Trigger
        btnImport.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            galleryLauncher.launch(intent)
        }
    }

    private fun checkBiometricAuth() {
        val executor: Executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Vault Unlocked", Toast.LENGTH_SHORT).show()
                    loadHiddenImages() // Load hidden files only after verification passes
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Access Denied: $errString", Toast.LENGTH_SHORT).show()
                    finish() // Close the screen immediately if auth fails
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Identity verification failed", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Encrypted Media Vault")
            .setSubtitle("Authenticate fingerprint to view hidden folder files")
            .setNegativeButtonText("Exit")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // Capture image selected from phone public storage gallery and copy to private zone
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                saveImageToSecureSandbox(imageUri)
            }
        }
    }

    private fun saveImageToSecureSandbox(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                // Generate unique secure file identity name
                val uniqueFileName = "IMG_${UUID.randomUUID()}.jpg"
                val destinationFile = File(vaultFolder, uniqueFileName)

                // Pipe data into application local space
                val outputStream = FileOutputStream(destinationFile)
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(this, "File hidden securely!", Toast.LENGTH_SHORT).show()
                loadHiddenImages() // Refresh the preview view grid
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error processing security write: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadHiddenImages() {
        // Fetch all files sitting isolated inside our local sandbox directory path
        val imageFiles = vaultFolder.listFiles()?.toList() ?: emptyList()

        // Convert file references to usable Bitmaps
        val bitmaps = mutableListOf<Bitmap>()
        for (file in imageFiles) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                bitmaps.add(bitmap)
            }
        }

        // Custom simple inline UI mapping adapter to fit images inside grid spaces smoothly
        val customAdapter = object : android.widget.BaseAdapter() {
            override fun getCount(): Int = bitmaps.size
            override fun getItem(position: Int): Any = bitmaps[position]
            override fun getItemId(position: Int): Long = position.toLong()
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup?): android.view.View {
                val imageView = if (convertView == null) {
                    android.widget.ImageView(this@HiddenImagesActivity).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(250, 250)
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        setPadding(4, 4, 4, 4)
                    }
                } else {
                    convertView as android.widget.ImageView
                }
                imageView.setImageBitmap(bitmaps[position])
                return imageView
            }
        }

        gridView.adapter = customAdapter
    }
}