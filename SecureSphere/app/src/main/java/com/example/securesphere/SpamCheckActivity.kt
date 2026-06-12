package com.example.securesphere

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.securesphere.R
import com.google.gson.annotations.SerializedName
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class SpamCheckActivity : AppCompatActivity() {

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

        api.scan(ScanRequest(urlToCheck))
            .enqueue(object : Callback<ScanResponse> {

                override fun onResponse(
                    call: Call<ScanResponse>,
                    response: Response<ScanResponse>
                ) {
                    val result = response.body()

                    if (result == null) {
                        tvResult.text = "Invalid response"
                        return
                    }

                    when (result.verdict) {
                        "SAFE" -> {
                            tvResult.text = "✅ SAFE\nRisk: ${result.riskScore}%"
                            tvResult.setTextColor(Color.GREEN)
                        }
                        "WARNING" -> {
                            tvResult.text = "⚠️ WARNING\nRisk: ${result.riskScore}%"
                            tvResult.setTextColor(Color.YELLOW)
                        }
                        "DANGER" -> {
                            tvResult.text = "❌ DANGER\nRisk: ${result.riskScore}%"
                            tvResult.setTextColor(Color.RED)
                        }
                    }
                }

                override fun onFailure(call: Call<ScanResponse>, t: Throwable) {
                    tvResult.text = "Error: ${t.message}"
                }
            })
    }
}

data class ScanRequest(
    @SerializedName("url") val url: String
)

data class ScanResponse(
    @SerializedName("verdict") val verdict: String,
    @SerializedName("risk_score") val riskScore: Int
)

interface ApiService {
    @POST("scan")
    fun scan(@Body request: ScanRequest): Call<ScanResponse>
}
