package com.example.securesphere

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class PasswordManagerActivity : AppCompatActivity() {

    private lateinit var dbHelper: PasswordDbHelper
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_manager)

        // Initialize Database and UI
        dbHelper = PasswordDbHelper(this)
        val etSite = findViewById<EditText>(R.id.etSiteName)
        val etUser = findViewById<EditText>(R.id.etUsername)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnSave = findViewById<Button>(R.id.btnSavePass)
        listView = findViewById<ListView>(R.id.listViewPasswords)

        // START BIOMETRIC CHECK IMMEDIATELY
        checkBiometricAuth()

        // Button Logic
        btnSave.setOnClickListener {
            val site = etSite.text.toString()
            val user = etUser.text.toString()
            val pass = etPass.text.toString()

            if (site.isNotEmpty() && pass.isNotEmpty()) {
                dbHelper.addPassword(site, user, pass)
                Toast.makeText(this, "Saved Securely!", Toast.LENGTH_SHORT).show()
                // Clear inputs
                etSite.text.clear()
                etUser.text.clear()
                etPass.text.clear()
                // Refresh list
                loadPasswords()
            } else {
                Toast.makeText(this, "Enter details first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkBiometricAuth() {
        val executor: Executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {

                // 1. SUCCESS: User verified, load the passwords
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Identity Verified", Toast.LENGTH_SHORT).show()
                    loadPasswords() // Only load data here!
                }

                // 2. ERROR: User canceled or No Fingerprint set
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Auth Error: $errString", Toast.LENGTH_SHORT).show()
                    finish() // Close the screen if they fail!
                }

                // 3. FAILED: Wrong Fingerprint
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Wrong Fingerprint", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Secure Vault Access")
            .setSubtitle("Scan fingerprint to view passwords")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun loadPasswords() {
        val list = dbHelper.getAllPasswords()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
        listView.adapter = adapter
    }
}