package com.example.securesphere

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class SignUpActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private val client = OkHttpClient() // Added for Python API communication

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

            // --- NEW 2FA LOGIC STARTS HERE ---
            // We call Python first to send the OTP.
            // We DO NOT create the Firebase user yet.

            val json = JSONObject()
            json.put("email", email)

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://nimra3238.pythonanywhere.com/send-otp") // REPLACE WITH YOUR URL
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        btnSignUp.isEnabled = true
                        Toast.makeText(this@SignUpActivity, "Server Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@SignUpActivity, "OTP Sent to $email", Toast.LENGTH_SHORT).show()

                            // Move to 2FA screen and pass the user info forward
                            val intent = Intent(this@SignUpActivity, TwoFactorActivity::class.java)
                            intent.putExtra("NAME", name)
                            intent.putExtra("EMAIL", email)
                            intent.putExtra("PASSWORD", password)
                            startActivity(intent)
                        }
                    } else {
                        runOnUiThread {
                            btnSignUp.isEnabled = true
                            Toast.makeText(this@SignUpActivity, "Error: " + response.body?.string(), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
            // --- NEW 2FA LOGIC ENDS HERE ---
        }
    }
}