package com.example.securesphere

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // 1. LOGOUT BUTTON
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // 2. URL SCANNER BUTTON (Old "Spam" Button)
        // Note: In your XML, this ID must be "btnSpam"
        val btnUrlScan = findViewById<Button>(R.id.btnSpam)
        btnUrlScan.setOnClickListener {
            val intent = Intent(this, SpamCheckActivity::class.java)
            startActivity(intent)
        }

        // 3. EMAIL SPAM BUTTON (The New AI Feature)
        // Note: In your XML, this ID must be "btnOpenEmail"
        val btnEmail = findViewById<Button>(R.id.btnOpenEmail)
        btnEmail.setOnClickListener {
            val intent = Intent(this, EmailActivity::class.java)
            startActivity(intent)
        }

        // Find the generic Button, not a CardView
        val btnVault = findViewById<Button>(R.id.cardVault)

        btnVault.setOnClickListener {
            val intent = Intent(this, PasswordManagerActivity::class.java)
            startActivity(intent)
        }
        // 4. BREACH CHECKER
        val btnBreach = findViewById<Button>(R.id.btnOpenBreach)
        btnBreach.setOnClickListener {
            val intent = Intent(this, BreachCheckActivity::class.java)
            startActivity(intent)
        }

        val malwareBtn = findViewById<Button>(R.id.malwareBtn)

        malwareBtn.setOnClickListener {
            startActivity(Intent(this, MalwareAnalysisActivity::class.java))
        }
        //caller
        val callerIdButton = findViewById<Button>(R.id.btnOpenCaller)

        callerIdButton.setOnClickListener {
            val intent = Intent(this, CallerIDActivity::class.java)
            startActivity(intent)
        }
        // 4. Success Message
        Toast.makeText(this, "Dashboard Loaded!", Toast.LENGTH_SHORT).show()



    }


}

