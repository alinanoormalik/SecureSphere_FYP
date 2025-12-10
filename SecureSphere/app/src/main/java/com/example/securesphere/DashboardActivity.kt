package com.example.securesphere

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // --- 1. EXISTING LOGOUT LOGIC ---
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            // Clear back stack so user cannot go back
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // --- 2. NEW SPAM CHECK LOGIC (Added this!) ---
        val btnSpam = findViewById<Button>(R.id.btnSpam)

        btnSpam.setOnClickListener {
            val intent = Intent(this, SpamCheckActivity::class.java)
            startActivity(intent)
        }
    }
}