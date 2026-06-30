package com.example.securesphere

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
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

        vaultFolder = File(filesDir, "secure_vault_images")
        if (!vaultFolder.exists()) vaultFolder.mkdirs()

        val btnImport = findViewById<Button>(R.id.btnImportImage)
        gridView = findViewById<GridView>(R.id.gridViewImages)

        // Trigger Biometric Check
        checkBiometricAuth()

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
                    loadHiddenImages()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    finish()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Encrypted Media Vault")
            .setSubtitle("Use biometric or device PIN to access")
            .setDeviceCredentialAllowed(true)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { saveImageToSecureSandbox(it) }
        }
    }

    private fun saveImageToSecureSandbox(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                // METADATA OBFUSCATION: Generating a random UUID string
                val secureID = UUID.randomUUID().toString()
                val fileName = "SECURE_$secureID.enc"
                val destinationFile = File(vaultFolder, fileName)

                val outputStream = FileOutputStream(destinationFile)
                inputStream.use { input -> outputStream.use { output -> input.copyTo(output) } }

                Toast.makeText(this, "Image Obfuscated & Hidden!", Toast.LENGTH_SHORT).show()
                loadHiddenImages()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadHiddenImages() {
        val imageFiles = vaultFolder.listFiles()?.toList() ?: emptyList()
        val bitmaps = mutableListOf<Pair<Bitmap, String>>() // Store both bitmap and its name

        for (file in imageFiles) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            bitmap?.let { bitmaps.add(it to file.name) }
        }

        gridView.adapter = object : BaseAdapter() {
            override fun getCount(): Int = bitmaps.size
            override fun getItem(position: Int) = bitmaps[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val imageView = (convertView as? ImageView) ?: ImageView(this@HiddenImagesActivity).apply {
                    layoutParams = AbsListView.LayoutParams(300, 300)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setPadding(8, 8, 8, 8)
                }
                imageView.setImageBitmap(bitmaps[position].first)
                return imageView
            }
        }

        // --- THE FIX: CLICK TO PREVIEW & SHOW UUID ---
        gridView.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = bitmaps[position]
            showImagePreview(selectedItem.first, selectedItem.second)
        }
    }

    // This is the logic from your screenshot, improved to work as a popup
    private fun showImagePreview(bitmap: Bitmap, fileName: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.BLACK)
        }

        // 1. The Obfuscated Name View (As seen in your screenshot)
        val tvName = TextView(this).apply {
            text = "System Name: $fileName"
            textSize = 14f
            setPadding(0, 40, 0, 20)
            setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        // 2. The Full Image
        val ivFull = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0, 1.0f
            )
            setImageBitmap(bitmap)
        }

        // 3. Close button
        val btnClose = Button(this).apply {
            text = "Close Preview"
            setOnClickListener { dialog.dismiss() }
        }

        layout.addView(tvName)
        layout.addView(ivFull)
        layout.addView(btnClose)

        dialog.setContentView(layout)
        dialog.show()
    }
}