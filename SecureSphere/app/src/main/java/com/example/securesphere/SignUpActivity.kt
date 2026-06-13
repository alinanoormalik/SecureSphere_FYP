package com.example.securesphere

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class SignUpActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        mAuth = FirebaseAuth.getInstance()

        val etName = findViewById<TextInputEditText>(R.id.etSignUpName)
        val etEmail = findViewById<TextInputEditText>(R.id.etSignUpEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etSignUpPassword)
        val btnSignUp = findViewById<Button>(R.id.btnSignUpSubmit)
        val tvLoginLink = findViewById<TextView>(R.id.tvNavigateToLogin)

        tvLoginLink.setOnClickListener { finish() }

        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || password.length < 6) {
                Toast.makeText(this, "Complete credentials accurately (Password min 6 chars).", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSignUp.isEnabled = false

            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser

                    // Bind the real custom display name inside the authentication channel
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            Toast.makeText(this, "Security profile updated. Initiating 2FA verification...", Toast.LENGTH_SHORT).show()

                            // Handshake over verification checkpoint routing explicitly
                            val intent = Intent(this, TwoFactorActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            btnSignUp.isEnabled = true
                            Toast.makeText(this, "Profile sync issues: ${profileTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    btnSignUp.isEnabled = true
                    Toast.makeText(this, "Registration Roadblock: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}