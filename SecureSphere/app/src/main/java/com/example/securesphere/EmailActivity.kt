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

class EmailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email)

        val etText = findViewById<EditText>(R.id.etEmailText)
        val btnScan = findViewById<Button>(R.id.btnScanEmail)
        val tvResult = findViewById<TextView>(R.id.tvEmailResult)

        btnScan.setOnClickListener {
            val content = etText.text.toString()
            if (content.isNotEmpty()) {
                tvResult.text = "Analyzing..."
                checkEmail(content, tvResult)
            }
        }
    }

    private fun checkEmail(text: String, tvResult: TextView) {
        // YOUR PYTHONANYWHERE URL
        val url = "https://nimra3238.pythonanywhere.com/predict-email"

        val json = JSONObject()
        json.put("text", text)

        val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(requestBody).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { tvResult.text = "Error: Server Down" }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    val result = JSONObject(responseData).getString("result")
                    runOnUiThread {
                        if (result == "spam") {
                            tvResult.text = "⚠️ SPAM DETECTED"
                            tvResult.setTextColor(Color.RED)
                        } else {
                            tvResult.text = "✅ SAFE EMAIL"
                            tvResult.setTextColor(Color.GREEN)
                        }
                    }
                }
            }
        })
    }
}