package com.example.securesphere

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class TwoFactorActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private lateinit var tvTimer: TextView
    private lateinit var etCode: TextInputEditText
    private lateinit var btnVerify: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_two_factor)

        etCode = findViewById(R.id.etVerificationCode)
        btnVerify = findViewById(R.id.btnVerifyCode)
        tvTimer = findViewById(R.id.tvTimer)

        startTimer()

        btnVerify.setOnClickListener {
            verifyCode(etCode.text.toString())
        }
    }

    private fun startTimer() {
        object : CountDownTimer(60000, 1000) {
            override fun onTick(millis: Long) {
                tvTimer.text = "Code expires in: 00:${millis / 1000}"
            }
            override fun onFinish() {
                tvTimer.text = "Expired"
                etCode.isEnabled = false
                btnVerify.isEnabled = false
            }
        }.start()
    }

    private fun verifyCode(code: String) {
        val json = JSONObject().put("email", "user@example.com").put("code", code)
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("YOUR_PYTHONANYWHERE_URL/verify-2fa").post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { /* Handle error */ }
            override fun onResponse(call: Call, response: Response) {
                val success = response.isSuccessful
                runOnUiThread {
                    if (success) {
                        startActivity(Intent(this@TwoFactorActivity, DashboardActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@TwoFactorActivity, "Invalid or Expired", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}