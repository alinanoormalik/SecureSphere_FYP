package com.example.securesphere

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class EmailActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email)

        val etEmail = findViewById<EditText>(R.id.etEmailText)
        val btnScan = findViewById<Button>(R.id.btnScanEmail)
        val tvResult = findViewById<TextView>(R.id.tvEmailResult)

        btnScan.setOnClickListener {

            val emailText = etEmail.text.toString().trim()

            if (emailText.isEmpty()) {
                etEmail.error = "Please enter email content"
                return@setOnClickListener
            }

            tvResult.text = "Analyzing email..."
            tvResult.setTextColor(Color.GRAY)

            val json = JSONObject()
            json.put("email_text", emailText)

            val body = json.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://nimra3238.pythonanywhere.com/predict-email")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        tvResult.text = "Server connection failed"
                        tvResult.setTextColor(Color.RED)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseData = response.body?.string()

                    runOnUiThread {
                        try {
                            val jsonResponse = JSONObject(responseData ?: "")
                            val status = jsonResponse.getString("status")

                            if (status != "SUCCESS") {
                                tvResult.text = "Invalid server response"
                                tvResult.setTextColor(Color.RED)
                                return@runOnUiThread
                            }

                            val label = jsonResponse.getString("classification")
                            val confidence = jsonResponse.getDouble("confidence") * 100

                            when (label) {
                                "SPAM" -> {
                                    tvResult.text =
                                        "🚨 Fraudulent Email Detected\nConfidence: ${"%.1f".format(confidence)}%"
                                    tvResult.setTextColor(Color.RED)
                                }
                                "SUSPICIOUS" -> {
                                    tvResult.text =
                                        "⚠️ Suspicious Email\nManual review recommended\nConfidence: ${"%.1f".format(confidence)}%"
                                    tvResult.setTextColor(Color.parseColor("#FFA500")) // Orange
                                }
                                else -> {
                                    tvResult.text =
                                        "✅ Legitimate Email\nConfidence: ${"%.1f".format(confidence)}%"
                                    tvResult.setTextColor(Color.GREEN)
                                }
                            }

                        } catch (e: Exception) {
                            tvResult.text = "Parsing error"
                            tvResult.setTextColor(Color.RED)
                        }
                    }
                }
            })
        }
    }
}
