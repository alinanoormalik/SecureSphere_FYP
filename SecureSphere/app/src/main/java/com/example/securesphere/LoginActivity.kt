package com.example.securesphere

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 1. Initialize Firebase
        auth = FirebaseAuth.getInstance()

        // 2. Connect to the IDs in your XML file
        val emailInput = findViewById<EditText>(R.id.loginEmail)
        val passInput = findViewById<EditText>(R.id.loginPass)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoToSignup = findViewById<Button>(R.id.btnGoToSignup)

        // 3. LOGIC FOR LOGIN BUTTON
        btnLogin.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val pass = passInput.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                // Check Firebase
                auth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                            // *** INTENT CODE STARTS HERE ***
                            // This code moves you to the Dashboard
                            val intent = Intent(this, DashboardActivity::class.java)
                            startActivity(intent)
                            finish()
                            // *** INTENT CODE ENDS HERE ***

                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. LOGIC FOR SIGNUP BUTTON
        btnGoToSignup.setOnClickListener {
            // *** INTENT CODE STARTS HERE ***
            // This code moves you to the Signup Screen
            // Note: I used 'SignUpActivity' with Capital U to match your file
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            // *** INTENT CODE ENDS HERE ***
        }
    }
}