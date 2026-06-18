package com.example.securesphere

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        mAuth = FirebaseAuth.getInstance()

        // Feature Navigation
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

        findViewById<FrameLayout>(R.id.btnProfile).setOnClickListener { renderProfileSettingsEngine() }
    }

    private fun renderProfileSettingsEngine() {
        val currentUser = mAuth.currentUser
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_profile_settings, null)
        val dialogBuilder = AlertDialog.Builder(this).setView(dialogView).create()

        dialogView.findViewById<TextView>(R.id.tvSettingsUserName).text = currentUser?.displayName ?: "SecureSphere User"
        dialogView.findViewById<TextView>(R.id.tvSettingsUserEmail).text = currentUser?.email ?: "Identity Protected"

        // --- 1. THEME TOGGLE ---
        val themeSwitch = dialogView.findViewById<SwitchMaterial>(R.id.switchTheme)
        themeSwitch.isChecked = (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            dialogBuilder.dismiss()
        }

        // --- 2. YOUR INFO ---
        dialogView.findViewById<Button>(R.id.btnYourInformation).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Account Profile")
                .setMessage("<b>User:</b> ${currentUser?.displayName}<br><b>Email:</b> ${currentUser?.email}<br><br><i>Security Status: Active Protected</i>")
                .setMessage(Html.fromHtml("<b>User:</b> ${currentUser?.displayName ?: "N/A"}<br><b>Email:</b> ${currentUser?.email}<br><br>Security Level: <b>Encrypted</b>", Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("Close", null)
                .show()
        }

        // --- 3. CHANGE PASSWORD (FIXED: NO MORE STUCK BIOMETRICS) ---
        dialogView.findViewById<Button>(R.id.btnDialogChangePassword).setOnClickListener {
            openPasswordChangeDialog() // Open directly to avoid biometric bugs during FYP
        }

        // --- 4. AESTHETIC ABOUT SECTION ---
        dialogView.findViewById<Button>(R.id.btnAboutApp).setOnClickListener {
            val aboutText = """
                <p><b>SecureSphere: AI-Powered Security v1.0.8</b></p>
                <p>Your all-in-one terminal for advanced digital privacy:</p>
                <p>• <b>Spam URL:</b> Malicious link & redirect analysis.<br>
                • <b>Email Audit:</b> Phishing & spoofing detection.<br>
                • <b>Identity Breach:</b> Global data leak monitoring.<br>
                • <b>Caller ID:</b> Unknown call risk-scoring.<br>
                • <b>Password Vault:</b> Encrypted credential storage.<br>
                • <b>Image Vault:</b> Private media storage.<br>
                • <b>Malware Scan:</b> App integrity analysis.<br>
                • <b>Risky Tracking:</b> Location privacy audit.</p>
                <p><small>Final Year Project © 2026</small></p>
            """.trimIndent()

            MaterialAlertDialogBuilder(this)
                .setTitle("Application Framework")
                .setMessage(Html.fromHtml(aboutText, Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("Done", null)
                .show()
        }

        // --- 5. FEEDBACK ---
        dialogView.findViewById<Button>(R.id.btnSendFeedback).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:securesphere.official@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "SecureSphere Feedback")
            }
            try { startActivity(Intent.createChooser(intent, "Send Email")) }
            catch (e: Exception) { Toast.makeText(this, "Mail app not found", Toast.LENGTH_SHORT).show() }
        }

        // --- 6. LOGOUT ---
        dialogView.findViewById<Button>(R.id.btnDialogDisconnect).setOnClickListener {
            dialogBuilder.dismiss()
            mAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
        dialogBuilder.show()
    }

    private fun openPasswordChangeDialog() {
        val v = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val etEmail = v.findViewById<EditText>(R.id.etVerifyEmail)
        val etOldPass = v.findViewById<EditText>(R.id.etOldPassword)
        val etNewPass = v.findViewById<EditText>(R.id.etNewPassword)

        MaterialAlertDialogBuilder(this)
            .setTitle("Identity Verification")
            .setMessage("Confirm credentials to update your master security key.")
            .setView(v)
            .setPositiveButton("Update") { _, _ ->
                val em = etEmail.text.toString().trim()
                val old = etOldPass.text.toString().trim()
                val new = etNewPass.text.toString().trim()

                if (em.isEmpty() || old.isEmpty() || new.isEmpty()) {
                    Toast.makeText(this, "Please fill all security fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val user = mAuth.currentUser
                val cred = EmailAuthProvider.getCredential(em, old)

                user?.reauthenticate(cred)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        user.updatePassword(new).addOnCompleteListener { upTask ->
                            if (upTask.isSuccessful) {
                                MaterialAlertDialogBuilder(this)
                                    .setTitle("Security Updated")
                                    .setMessage("Success! Your password has been changed in the Firebase backend.")
                                    .setPositiveButton("Great", null).show()
                            } else {
                                Toast.makeText(this, "Error: ${upTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Auth Failed: Wrong Email or Password", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}