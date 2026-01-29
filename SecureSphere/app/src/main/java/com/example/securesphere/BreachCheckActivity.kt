package com.example.securesphere

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class BreachCheckActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_breach_check)

        // 1. Setup Views
        val etEmail = findViewById<EditText>(R.id.etBreachEmail)
        val btnCheck = findViewById<Button>(R.id.btnCheckBreach)
        val tvResult = findViewById<TextView>(R.id.tvBreachResult)

        // 2. Button Click Listener
        btnCheck.setOnClickListener {

            val emailInput = etEmail.text.toString().trim()

            if (emailInput.isEmpty()) {
                etEmail.error = "Please enter an email"
                return@setOnClickListener
            }

            // Show loading message
            tvResult.text = "Searching Dark Web..."
            tvResult.setTextColor(Color.YELLOW)

            // 3. START BACKGROUND THREAD
            Thread {
                try {
                    // Prepare JSON
                    val jsonObject = JSONObject()
                    jsonObject.put("email", emailInput)

                    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val body = jsonObject.toString().toRequestBody(mediaType)
                    val request = Request.Builder()
                        .url("https://nimra3238.pythonanywhere.com/check-breach")
                        .post(body)
                        .build()



                    val client = OkHttpClient()
                    val response = client.newCall(request).execute()
                    val responseData = response.body?.string() ?: ""

                    // 4. BACK TO UI THREAD
                    runOnUiThread {
                        try {

                            // HTTP error check
                            if (!response.isSuccessful) {
                                tvResult.text = "❌ Server Error (${response.code})"
                                tvResult.setTextColor(Color.RED)
                                return@runOnUiThread
                            }

                            // HTML / 404 protection
                            if (responseData.trim().startsWith("<")) {
                                tvResult.text =
                                    "❌ API Error: Invalid endpoint or server issue"
                                tvResult.setTextColor(Color.RED)
                                return@runOnUiThread
                            }

                            // Safe JSON parsing
                            val jsonResponse = JSONObject(responseData)
                            val status = jsonResponse.optString("status", "UNKNOWN")
                            val source = jsonResponse.optString("source", "N/A")

                            when (status) {
                                "UNSAFE" -> {
                                    tvResult.text =
                                        "⚠️ ALERT: BREACH FOUND!\nSource: $source"
                                    tvResult.setTextColor(Color.RED)
                                }

                                "SAFE" -> {
                                    tvResult.text = "✅ SAFE. No leaks found."
                                    tvResult.setTextColor(Color.GREEN)
                                }

                                else -> {
                                    tvResult.text =
                                        "⚠ Unexpected response:\n$responseData"
                                    tvResult.setTextColor(Color.BLACK)
                                }
                            }

                        } catch (e: Exception) {
                            tvResult.text = "❌ Error parsing server response"
                            tvResult.setTextColor(Color.RED)
                        }
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        tvResult.text = "❌ Connection Error: ${e.message}"
                        tvResult.setTextColor(Color.RED)
                    }
                }
            }.start()   // ✅ THREAD STARTED
        }
    }
}
