package com.example.securesphere

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
    private val client = OkHttpClient()

    // Validation Flags
    private var isCaseMet = false
    private var isNumMet = false
    private var isSpecialMet = false
    private var isLenMet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        mAuth = FirebaseAuth.getInstance()

        // UI Elements
        val etName = findViewById<TextInputEditText>(R.id.etSignUpName)
        val etEmail = findViewById<TextInputEditText>(R.id.etSignUpEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etSignUpPassword)
        val btnSignUp = findViewById<Button>(R.id.btnSignUpSubmit)
        val tvLoginLink = findViewById<TextView>(R.id.tvNavigateToLogin)

        // Password Rule TextViews
        val ruleCase = findViewById<TextView>(R.id.ruleCase)
        val ruleNum = findViewById<TextView>(R.id.ruleNumber)
        val ruleSpecial = findViewById<TextView>(R.id.ruleSpecial)
        val ruleLen = findViewById<TextView>(R.id.ruleLength)

        // Colors
        val themePurple = Color.parseColor("#BB86FC")
        val inactiveGrey = Color.parseColor("#9E9E9E")

        tvLoginLink.setOnClickListener { finish() }

        // LIVE PASSWORD VALIDATION LOGIC
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pwd = s.toString()

                // 1. Check Upper and Lower Case
                isCaseMet = pwd.any { it.isUpperCase() } && pwd.any { it.isLowerCase() }
                ruleCase.setTextColor(if (isCaseMet) themePurple else inactiveGrey)

                // 2. Check for at least one number
                isNumMet = pwd.any { it.isDigit() }
                ruleNum.setTextColor(if (isNumMet) themePurple else inactiveGrey)

                // 3. Check for at least one special character
                isSpecialMet = pwd.any { !it.isLetterOrDigit() }
                ruleSpecial.setTextColor(if (isSpecialMet) themePurple else inactiveGrey)

                // 4. Check for length (at least 8)
                isLenMet = pwd.length >= 8
                ruleLen.setTextColor(if (isLenMet) themePurple else inactiveGrey)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // SUBMIT BUTTON LOGIC
        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Step 1: Check if any field is empty
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all details.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Step 2: Enforce all 4 security rules
            if (!isCaseMet || !isNumMet || !isSpecialMet || !isLenMet) {
                Toast.makeText(this, "Security requirements not met (Check purple rules).", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            btnSignUp.isEnabled = false

            // Step 3: Call Python Backend for OTP
            val json = JSONObject()
            json.put("email", email)

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://nimra3238.pythonanywhere.com/send-otp")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        btnSignUp.isEnabled = true
                        Toast.makeText(this@SignUpActivity, "Network Error: Server unreachable", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        runOnUiThread {
                            // Go to 2FA Screen
                            val intent = Intent(this@SignUpActivity, TwoFactorActivity::class.java)
                            intent.putExtra("NAME", name)
                            intent.putExtra("EMAIL", email)
                            intent.putExtra("PASSWORD", password)
                            startActivity(intent)
                        }
                    } else {
                        runOnUiThread {
                            btnSignUp.isEnabled = true
                            Toast.makeText(this@SignUpActivity, "OTP Failed: Check email format", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }
}