package com.example.securesphere

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.annotations.SerializedName
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class SpamCheckActivity : AppCompatActivity() {

    // FIX 1: Base URL must end with / and not include the endpoint
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
                tvResult.text = "Scanning in Real-Time..."
                tvResult.setTextColor(Color.GRAY)
                checkUrl(url, tvResult)
            } else {
                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUrl(urlToCheck: String, tvResult: TextView) {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)

        api.scan(ScanRequest(urlToCheck))
            .enqueue(object : Callback<ScanResponse> {
                override fun onResponse(call: Call<ScanResponse>, response: Response<ScanResponse>) {
                    if (response.isSuccessful) {
                        val result = response.body()

                        // FIX 2: Check if result or the expected field is null
                        if (result?.resultText == null) {
                            tvResult.text = "Server Error: Missing keys"
                            return
                        }

                        // FIX 3: Match the exact labels sent from Python AI
                        val responseLabel = result.resultText
                        tvResult.text = responseLabel

                        if (responseLabel.contains("SAFE")) {
                            tvResult.setTextColor(Color.GREEN)
                        } else if (responseLabel.contains("SPAM") || responseLabel.contains("DETECTED")) {
                            tvResult.setTextColor(Color.RED)
                        } else {
                            tvResult.setTextColor(Color.YELLOW)
                        }

                    } else {
                        tvResult.text = "Error: ${response.code()}"
                    }
                }

                override fun onFailure(call: Call<ScanResponse>, t: Throwable) {
                    tvResult.text = "Network Error: ${t.message}"
                }
            })
    }
}

// FIX 4: Data classes must match the Backend JSON keys
data class ScanRequest(
    @SerializedName("url") val url: String
)

data class ScanResponse(
    @SerializedName("status") val status: String,
    @SerializedName("result") val resultText: String  // This matches 'result' from Python
)

interface ApiService {
    // FIX 5: Endpoint is defined here
    @POST("scan")
    fun scan(@Body request: ScanRequest): Call<ScanResponse>
}