package com.example.securesphere

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. THIS LINE MUST BE HERE. IF MISSING -> WHITE SCREEN
        setContentView(R.layout.activity_dashboard)

        // 2. Find the Logout Button
        // Make sure the ID in XML is exactly 'btnLogout'
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        btnLogout.setOnClickListener {
            // Sign out Logic
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // 3. Find the Spam Check Button
        // In your screenshot, you called it 'LinearLayout'.
        // If it is a Button in XML, you MUST call it 'Button' here.
        val btnSpam = findViewById<Button>(R.id.btnSpam)

        btnSpam.setOnClickListener {
            // Go to SpamCheckActivity
            val intent = Intent(this, SpamCheckActivity::class.java)
            startActivity(intent)
        }

        // Show a popup so we know the page Loaded successfully
        Toast.makeText(this, "Dashboard Loaded!", Toast.LENGTH_SHORT).show()
    }
}