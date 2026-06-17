package com.example.securesphere

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class TwoFactorActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private val client = OkHttpClient()
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_two_factor)

        mAuth = FirebaseAuth.getInstance()

        val etOtpCode = findViewById<TextInputEditText>(R.id.etVerificationCode)
        val btnVerify = findViewById<Button>(R.id.btnVerifyCode)
        val tvTimer = findViewById<TextView>(R.id.tvTimer)

        // START 2-MINUTE TIMER (120,000ms)
        startTimer(tvTimer)

        btnVerify.setOnClickListener {
            val code = etOtpCode.text.toString().trim()
            if (code.length == 6) {
                verifyTheCode(code)
            } else {
                Toast.makeText(this, "Enter the 6-digit code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startTimer(tvTimer: TextView) {
        // 120000 ms = 2 minutes
        countDownTimer = object : CountDownTimer(120000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                // Formats it as 01:59, 01:58, etc.
                val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                tvTimer.text = "Code expires in: $timeFormatted"
            }

            override fun onFinish() {
                tvTimer.text = "Code expired. Please try again."
            }
        }.start()
    }

    private fun verifyTheCode(enteredCode: String) {
        val email = intent.getStringExtra("EMAIL") ?: ""
        val json = JSONObject().put("email", email).put("code", enteredCode)
        val body = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://nimra3238.pythonanywhere.com/verify-otp")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@TwoFactorActivity, "Connection Lost", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        countDownTimer?.cancel()
                        createUserInFirebase()
                    }
                } else {
                    runOnUiThread { Toast.makeText(this@TwoFactorActivity, "Wrong Code!", Toast.LENGTH_SHORT).show() }
                }
            }
        })
    }

    private fun createUserInFirebase() {
        val name = intent.getStringExtra("NAME") ?: ""
        val email = intent.getStringExtra("EMAIL") ?: ""
        val password = intent.getStringExtra("PASSWORD") ?: ""

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                    user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                        Toast.makeText(this, "Success! Security Profile Created.", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, DashboardActivity::class.java)
                        startActivity(intent)
                        finishAffinity()
                    }
                } else {
                    Toast.makeText(this, "Auth Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}