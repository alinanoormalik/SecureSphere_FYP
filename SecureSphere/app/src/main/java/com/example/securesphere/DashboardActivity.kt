package com.example.securesphere

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        mAuth = FirebaseAuth.getInstance()

        // Mapped UI component hooks synchronized with your exact CardView layout elements
        val btnSpamUrl = findViewById<CardView>(R.id.btnCheckSpamUrl)
        val btnEmailSpam = findViewById<CardView>(R.id.btnCheckEmailSpam)
        val btnIdentityBreach = findViewById<CardView>(R.id.btnCheckIdentityBreach)
        val btnCallerId = findViewById<CardView>(R.id.btnCallerIdCheck)
        val btnPasswordVault = findViewById<CardView>(R.id.btnOpenPasswordVault)
        val btnImageVault = findViewById<CardView>(R.id.btnOpenHiddenImageVault)
        val btnMalwareAnalysis = findViewById<CardView>(R.id.btnMalwareAnalysis)
        val btnRiskyArea = findViewById<CardView>(R.id.btnRiskyAreaLocation)

        btnSpamUrl.setOnClickListener {
            // Intent handling when ready
        }

        btnEmailSpam.setOnClickListener {
            startActivity(Intent(this, EmailActivity::class.java))
        }

        btnIdentityBreach.setOnClickListener {
            startActivity(Intent(this, BreachCheckActivity::class.java))
        }

        btnCallerId.setOnClickListener {
            startActivity(Intent(this, CallerIDActivity::class.java))
        }

        btnPasswordVault.setOnClickListener {
            startActivity(Intent(this, PasswordManagerActivity::class.java))
        }

        btnImageVault.setOnClickListener {
            startActivity(Intent(this, HiddenImagesActivity::class.java))
        }

        btnMalwareAnalysis.setOnClickListener {
            // Intent handling when ready
        }

        btnRiskyArea.setOnClickListener {
            startActivity(Intent(this, RiskyShareActivity::class.java))
        }

        // Fixed FrameLayout user avatar profile anchor element
        val btnProfileSettings = findViewById<FrameLayout>(R.id.btnProfile)
        btnProfileSettings.setOnClickListener {
            renderProfileSettingsEngine()
        }
    }

    /**
     * Inflates custom dialogue overlay modules matching your dialog layouts exactly
     */
    private fun renderProfileSettingsEngine() {
        val currentUserNode = mAuth.currentUser
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_profile_settings, null)

        val tvProfileName = dialogView.findViewById<TextView>(R.id.tvSettingsUserName)
        val tvProfileEmail = dialogView.findViewById<TextView>(R.id.tvSettingsUserEmail)
        val btnChangePassword = dialogView.findViewById<Button>(R.id.btnDialogChangePassword)
        val btnDisconnectSession = dialogView.findViewById<Button>(R.id.btnDialogDisconnect)

        // Binding actual text data from auth node safely
        tvProfileName.text = currentUserNode?.displayName ?: "SecureSphere User"
        tvProfileEmail.text = currentUserNode?.email ?: "No Registered Session Found"

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnChangePassword.setOnClickListener {
            val passwordField = EditText(this).apply {
                hint = "Minimum 6 characters"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            AlertDialog.Builder(this)
                .setTitle("Update Security Access Credentials")
                .setMessage("Enter your new system authorization passphrase:")
                .setView(passwordField)
                .setPositiveButton("CONFIRM") { _, _ ->
                    val freshPassphrase = passwordField.text.toString().trim()
                    if (freshPassphrase.length >= 6) {
                        currentUserNode?.updatePassword(freshPassphrase)
                            ?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this, "Passphrase updated. System re-secured.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Modification Denied: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "Security parameters validation failed: Weak Passphrase.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("CANCEL", null)
                .show()
        }

        // Disconnect session and routing back to log in
        btnDisconnectSession.setOnClickListener {
            dialogBuilder.dismiss()
            mAuth.signOut()
            Toast.makeText(this, "Account Session Revoked.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        dialogBuilder.show()
    }
}