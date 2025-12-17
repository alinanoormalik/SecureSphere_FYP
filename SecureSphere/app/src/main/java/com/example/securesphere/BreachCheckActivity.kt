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

class BreachCheckActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_breach_check)

        val etEmail = findViewById<EditText>(R.id.etBreachEmail)
        val btnCheck = findViewById<Button>(R.id.btnCheckBreach)
        val tvResult = findViewById<TextView>(R.id.tvBreachResult)

        btnCheck.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                tvResult.text = "Searching Database..."
                checkBreach(email, tvResult)
            }
        }
    }

    private fun checkBreach(email: String, tvResult: TextView) {
        // 1. URL to your PythonAnywhere Server
        val url = "https://nimra3238.pythonanywhere.com/check-breach"

        // 2. JSON Data
        val json = JSONObject()
        json.put("email", email)

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
                    val status = jsonRes.getString("status") // "Safe" or "Unsafe"

                    runOnUiThread {
                        if (status == "Unsafe") {
                            tvResult.text = "⚠️ LEAKED! Change Password!"
                            tvResult.setTextColor(Color.RED)
                        } else {
                            tvResult.text = "✅ SAFE. No leaks found."
                            tvResult.setTextColor(Color.GREEN)
                        }
                    }
                }
            }
        })
    }
}