package com.example.securesphere

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onStart() {
        super.onStart()
        // PROFESSIONAL AUTO-LOGIN: Check if a Firebase session already exists
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val sharedPref = getSharedPreferences("SecureSpherePrefs", Context.MODE_PRIVATE)
            val skip2FA = sharedPref.getBoolean("Skip2FA_${currentUser.uid}", false)

            if (skip2FA) {
                // If "Remember Me" was previously selected, skip everything and go to Dashboard
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()
        val sharedPref = getSharedPreferences("SecureSpherePrefs", Context.MODE_PRIVATE)

        val etEmail = findViewById<TextInputEditText>(R.id.etLoginEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etLoginPassword)
        val cbRememberMe = findViewById<CheckBox>(R.id.cbRememberMe)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignUpLink = findViewById<TextView>(R.id.tvNavigateToSignUp)

        tvSignUpLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please populate all authentication fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = mAuth.currentUser?.uid
                    val rememberMeChecked = cbRememberMe.isChecked

                    // Save the "Remember Me" preference for this specific user
                    sharedPref.edit().putBoolean("Skip2FA_$userId", rememberMeChecked).apply()

                    if (rememberMeChecked) {
                        Toast.makeText(this, "Welcome back! Session persisted.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    } else {
                        // User did not check "Remember Me", route to 2FA verification
                        startActivity(Intent(this, TwoFactorActivity::class.java))
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}