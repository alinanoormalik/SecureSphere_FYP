package com.example.securesphere

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class SpamCheckActivity : AppCompatActivity() {

    // 👇 PASTE YOUR FRIEND'S URL HERE (Keep the / at the end)
    private val BASE_URL = "https://nimra3238.pythonanywhere.com/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spam_check)

        val etUrl = findViewById<EditText>(R.id.etUrlInput)
        val btnScan = findViewById<Button>(R.id.btnScan)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        btnScan.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                tvResult.text = "Checking..."
                checkUrl(url, tvResult)
            }
        }
    }

    private fun checkUrl(urlToCheck: String, tvResult: TextView) {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)

        api.scan(ScanRequest(urlToCheck)).enqueue(object : Callback<ScanResponse> {
            override fun onResponse(call: Call<ScanResponse>, response: Response<ScanResponse>) {
                if (response.body()?.status == "Safe") {
                    tvResult.text = "✅ SAFE"
                    tvResult.setTextColor(Color.GREEN)
                } else {
                    tvResult.text = "⚠️ DANGER"
                    tvResult.setTextColor(Color.RED)
                }
            }
            override fun onFailure(call: Call<ScanResponse>, t: Throwable) {
                tvResult.text = "Error: ${t.message}"
            }
        })
    }
}

data class ScanRequest(@SerializedName("url") val url: String)
data class ScanResponse(@SerializedName("status") val status: String)

interface ApiService {
    @POST("scan")
    fun scan(@Body request: ScanRequest): Call<ScanResponse>
}