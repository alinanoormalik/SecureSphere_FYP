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
import java.io.IOException

class CallerIDActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caller_idactivity)

        val etPhone = findViewById<EditText>(R.id.etPhoneInput)
        val btnIdentify = findViewById<Button>(R.id.btnIdentify)
        val tvResult = findViewById<TextView>(R.id.tvCallerResult)

        btnIdentify.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            if (phone.isNotEmpty()) {
                tvResult.text = "Identifying..."
                identifyCaller(phone, tvResult)
            }
        }
    }

    private fun identifyCaller(phone: String, tvResult: TextView) {
        // 1. URL to your PythonAnywhere Server
        val url = "https://nimra3238.pythonanywhere.com/check-caller"

        // 2. JSON Data
        val json = JSONObject()
        json.put("phone", phone)

        val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(requestBody).build()
        val client = OkHttpClient()

        // 3. Send Request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { tvResult.text = "Error: Server Connection Failed" }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    val jsonRes = JSONObject(responseData)

                    // Server returns: {"valid": true/false, "carrier": "Name"}
                    val isValid = jsonRes.getBoolean("valid")
                    val carrier = jsonRes.getString("carrier")

                    runOnUiThread {
                        if (isValid) {
                            tvResult.text = "✅ Verified: $carrier"
                            tvResult.setTextColor(Color.GREEN)
                        } else {
                            tvResult.text = "⚠️ SPAM CALLER DETECTED!"
                            tvResult.setTextColor(Color.RED)
                        }
                    }
                }
            }
        })
    }
}