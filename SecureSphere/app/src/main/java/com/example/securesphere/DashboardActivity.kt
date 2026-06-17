package com.example.securesphere

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor

class DashboardActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        mAuth = FirebaseAuth.getInstance()

        // Existing CardView hooks
        val btnSpamUrl = findViewById<CardView>(R.id.btnCheckSpamUrl)
        val btnEmailSpam = findViewById<CardView>(R.id.btnCheckEmailSpam)
        val btnIdentityBreach = findViewById<CardView>(R.id.btnCheckIdentityBreach)
        val btnCallerId = findViewById<CardView>(R.id.btnCallerIdCheck)
        val btnPasswordVault = findViewById<CardView>(R.id.btnOpenPasswordVault)
        val btnImageVault = findViewById<CardView>(R.id.btnOpenHiddenImageVault)
        val btnMalwareAnalysis = findViewById<CardView>(R.id.btnMalwareAnalysis)
        val btnRiskyArea = findViewById<CardView>(R.id.btnRiskyAreaLocation)

        btnSpamUrl.setOnClickListener { startActivity(Intent(this, SpamCheckActivity::class.java)) }
        btnEmailSpam.setOnClickListener { startActivity(Intent(this, EmailActivity::class.java)) }
        btnIdentityBreach.setOnClickListener { startActivity(Intent(this, BreachCheckActivity::class.java)) }
        btnCallerId.setOnClickListener { startActivity(Intent(this, CallerIDActivity::class.java)) }
        btnPasswordVault.setOnClickListener { startActivity(Intent(this, PasswordManagerActivity::class.java)) }
        btnImageVault.setOnClickListener { startActivity(Intent(this, HiddenImagesActivity::class.java)) }
        btnMalwareAnalysis.setOnClickListener { startActivity(Intent(this, MalwareAnalysisActivity::class.java)) }
        btnRiskyArea.setOnClickListener { startActivity(Intent(this, RiskyShareActivity::class.java)) }

        val btnProfileSettings = findViewById<FrameLayout>(R.id.btnProfile)
        btnProfileSettings.setOnClickListener { renderProfileSettingsEngine() }
    }

    private fun renderProfileSettingsEngine() {
        val currentUserNode = mAuth.currentUser
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_profile_settings, null)
        val dialogBuilder = AlertDialog.Builder(this).setView(dialogView).create()

        // UI elements
        dialogView.findViewById<TextView>(R.id.tvSettingsUserName).text = currentUserNode?.displayName ?: "SecureSphere User"
        dialogView.findViewById<TextView>(R.id.tvSettingsUserEmail).text = currentUserNode?.email ?: "No Registered Session"

        // 1. YOUR INFORMATION
        dialogView.findViewById<Button>(R.id.btnYourInformation).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Your Information")
                .setMessage("Name: ${currentUserNode?.displayName ?: "N/A"}\nEmail: ${currentUserNode?.email ?: "N/A"}")
                .setPositiveButton("OK", null)
                .show()
        }

        // 2. CHANGE PASSWORD (Biometric Auth First)
        dialogView.findViewById<Button>(R.id.btnDialogChangePassword).setOnClickListener {
            showBiometricPrompt {
                // Biometric Success -> Open Password Change Dialog
                openPasswordChangeDialog()
            }
        }

        // 3. ABOUT
        dialogView.findViewById<Button>(R.id.btnAboutApp).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("About SecureSphere")
                .setMessage("SecureSphere v1.0\nYour all-in-one security and privacy companion.")
                .setPositiveButton("OK", null)
                .show()
        }

        // 4. TRANSMIT FEEDBACK
        dialogView.findViewById<Button>(R.id.btnSendFeedback).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@securesphere.com")
                putExtra(Intent.EXTRA_SUBJECT, "Feedback from SecureSphere User")
            }
            startActivity(Intent.createChooser(intent, "Send Feedback"))
        }

        dialogView.findViewById<Button>(R.id.btnDialogDisconnect).setOnClickListener {
            dialogBuilder.dismiss()
            mAuth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        dialogBuilder.show()
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
        })
        biometricPrompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle("Identity Verification").setSubtitle("Confirm fingerprint to proceed").setNegativeButtonText("Cancel").build())
    }

    private fun openPasswordChangeDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        MaterialAlertDialogBuilder(this)
            .setTitle("Change Password")
            .setView(view)
            .setPositiveButton("Update") { _, _ ->
                val newPass = view.findViewById<EditText>(R.id.etNewPassword).text.toString()
                mAuth.currentUser?.updatePassword(newPass)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }
}