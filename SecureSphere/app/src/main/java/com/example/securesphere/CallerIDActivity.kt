package com.example.securesphere

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

        // Find views
        val etPhone = findViewById<EditText>(R.id.etPhoneInput)
        val btnIdentify = findViewById<Button>(R.id.btnIdentify)
        val tvResult = findViewById<TextView>(R.id.tvCallerResult)

        btnIdentify.setOnClickListener {
            val phoneNumber = etPhone.text.toString().trim()

            if (phoneNumber.isEmpty()) {
                etPhone.error = "Enter a number first"
                return@setOnClickListener
            }

            // Show status
            tvResult.visibility = View.VISIBLE
            tvResult.text = "🔍 Analyzing caller data..."

            // API Call in Background
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

                                val phone = json.optString("phone", phoneNumber)
                                val carrier = json.optString("carrier", "Unknown")
                                val country = json.optString("country", "Unknown")
                                val region = json.optString("phone_region", "Unknown")
                                val type = json.optString("phone_type", "Unknown")
                                val isValid = json.optBoolean("phone_valid", false)

                                var riskStatus = "SAFE"
                                if (type.equals("voip", ignoreCase = true) || !isValid) {
                                    riskStatus = "FRAUD RISK"
                                }

                                // Format output (Theme will handle text color automatically)
                                val report = "CALLER ANALYSIS REPORT\n" +
                                        "------------------------------------------\n" +
                                        "Number   :  $phone\n" +
                                        "Country  :  $country\n" +
                                        "Region   :  $region\n" +
                                        "Carrier  :  $carrier\n" +
                                        "Type     :  ${type.uppercase()}\n" +
                                        "------------------------------------------\n" +
                                        "STATUS   :  $riskStatus"

                                tvResult.text = report

                            } catch (e: Exception) {
                                tvResult.text = "Error parsing response"
                            }
                        } else {
                            tvResult.text = "No response from server"
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        tvResult.text = "Connection Error: Check Internet"
                    }
                }
            }.start()
        }
    }
}