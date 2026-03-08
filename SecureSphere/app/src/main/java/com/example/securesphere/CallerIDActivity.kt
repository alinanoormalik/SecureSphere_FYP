package com.example.securesphere

import android.os.Bundle
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

        // 1. Setup Views (This defines tvResult so it won't be red!)
        val etPhone = findViewById<EditText>(R.id.etPhoneInput)
        val btnIdentify = findViewById<Button>(R.id.btnIdentify) // Make sure ID matches XML
        val tvResult = findViewById<TextView>(R.id.tvCallerResult) // Make sure ID matches XML

        // 2. Button Action
        btnIdentify.setOnClickListener {
            val phoneNumber = etPhone.text.toString().trim()

            if (phoneNumber.isEmpty()) {
                etPhone.error = "Enter a number first"
                return@setOnClickListener
            }

            tvResult.text = "🔍 Checking Database..."

            // 3. Start Background Thread for API
            Thread {
                try {
                    val client = OkHttpClient()

                    // ✅ YOUR API KEY IS HERE (Fixed URL)
                    val url = "https://api.veriphone.io/v2/verify?phone=$phoneNumber&key=2F115CB9DDD94FF5AE4BD6E4AD7B2490"

                    val request = Request.Builder()
                        .url(url)
                        .build()

                    val response = client.newCall(request).execute()
                    val result = response.body?.string()

                    // 4. Update UI with the result
                    runOnUiThread {
                        if (result != null) {
                            try {
                                // Parse the JSON
                                val json = JSONObject(result)

                                // Extract details safely
                                val phone = json.optString("phone", phoneNumber)
                                val carrier = json.optString("carrier", "Unknown Carrier")
                                val country = json.optString("country", "Unknown Country")
                                val region = json.optString("phone_region", "Unknown Region")
                                val type = json.optString("phone_type", "Unknown")
                                val isValid = json.optBoolean("phone_valid", false)

                                // Risk Logic
                                var riskLevel = "✅ Safe"
                                if (type.equals("voip", ignoreCase = true)) {
                                    riskLevel = "⚠️ High Risk (VOIP)"
                                } else if (!isValid) {
                                    riskLevel = "❌ Invalid Number"
                                }

                                // Nice Formatting
                                val formattedText = """
                                    📞 CALLER ANALYSIS
                                    ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
                                    
                                    📱 Number:   $phone
                                    🌍 Country:  $country
                                    📍 Region:   $region
                                    🏢 Carrier:  $carrier
                                    📡 Type:     ${type.uppercase()}
                                    
                                    🛡️ SECURITY STATUS:
                                    $riskLevel
                                """.trimIndent()

                                tvResult.text = formattedText

                            } catch (e: Exception) {
                                tvResult.text = "Error parsing: ${e.message}"
                            }
                        } else {
                            tvResult.text = "No response from server"
                        }
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        tvResult.text = "Connection Error: ${e.message}"
                    }
                }
            }.start()
        }
    }
}