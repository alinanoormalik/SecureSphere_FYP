package com.example.securesphere

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class CallerIDActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caller_idactivity)

        val etPhone = findViewById<EditText>(R.id.etPhoneInput)
        val btnIdentify = findViewById<Button>(R.id.btnIdentify)
        val tvResult = findViewById<TextView>(R.id.tvCallerResult)

        btnIdentify.setOnClickListener {
            var phoneNumber = etPhone.text.toString().trim()

            if (phoneNumber.isEmpty()) {
                etPhone.error = "Please enter a number"
                return@setOnClickListener
            }

            if (phoneNumber.startsWith("0")) {
                phoneNumber = "+92" + phoneNumber.substring(1)
            } else if (!phoneNumber.startsWith("+")) {
                phoneNumber = "+$phoneNumber"
            }

            tvResult.visibility = View.VISIBLE
            tvResult.text = "🔍 Interrogating Global Telemetry..."
            tvResult.setTextColor(tvResult.textColors.defaultColor)

            Thread {
                try {
                    val client = OkHttpClient()
                    val url = "https://api.veriphone.io/v2/verify?phone=$phoneNumber&key=2F115CB9DDD94FF5AE4BD6E4AD7B2490"

                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    val responseData = response.body?.string()

                    runOnUiThread {
                        if (responseData != null) {
                            try {
                                val json = JSONObject(responseData)
                                val isValid = json.optBoolean("phone_valid", false)
                                val type = json.optString("phone_type", "unknown").lowercase()
                                val carrier = json.optString("carrier", "").lowercase()
                                val country = json.optString("country", "Unknown")

                                // --- LOGIC KEPT IN BACKGROUND FOR COLOR ACCURACY ---
                                var totalPenalty = 0
                                if (!isValid) {
                                    totalPenalty = 100
                                } else {
                                    when (type) {
                                        "voip" -> totalPenalty += 60
                                        "premium_rate" -> totalPenalty += 50
                                        "toll_free" -> totalPenalty += 40
                                        "unknown" -> totalPenalty += 30
                                    }
                                    if (country != "Pakistan") {
                                        totalPenalty += 25
                                    }
                                    val spamCarriers = listOf("twilio", "pinger", "textnow", "bandwidth", "nexmo", "plivo", "sinch", "vonage")
                                    if (spamCarriers.any { carrier.contains(it) }) {
                                        totalPenalty += 35
                                    }
                                }

                                val finalRiskScore = Math.min(totalPenalty, 100)

                                val statusColor: Int
                                val statusLabel: String
                                when {
                                    finalRiskScore >= 70 -> {
                                        statusLabel = "HIGH RISK / SCAM LIKELY"
                                        statusColor = Color.parseColor("#FF5252") // Red
                                    }
                                    finalRiskScore >= 30 -> {
                                        statusLabel = "SUSPICIOUS ACTIVITY"
                                        statusColor = Color.parseColor("#FFB74D") // Orange
                                    }
                                    else -> {
                                        statusLabel = "VERIFIED IDENTITY"
                                        statusColor = Color.parseColor("#00E676") // Green
                                    }
                                }

                                // --- CLEAN OUTPUT FORMAT (NO THREAT INDEX) ---
                                val report = """
                                    CALLER FORENSIC REPORT
                                    ----------------------------------
                                    TARGET    : $phoneNumber
                                    COUNTRY   : $country
                                    LINE TYPE : ${type.uppercase()}
                                    CARRIER   : ${if(carrier.isEmpty()) "PRIVATE EXCHANGE" else carrier.uppercase()}
                                    ----------------------------------
                                    ANALYSIS     : $statusLabel
                                """.trimIndent()

                                tvResult.text = report
                                tvResult.setTextColor(statusColor)

                            } catch (e: Exception) {
                                tvResult.text = "Analysis Error: Metadata Corrupt"
                            }
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread { tvResult.text = "Connection Error: API Timeout" }
                }
            }.start()
        }
    }
}