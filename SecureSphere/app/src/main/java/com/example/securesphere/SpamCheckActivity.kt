package com.example.securesphere

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.annotations.SerializedName
import com.google.android.material.progressindicator.LinearProgressIndicator
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

        // UI elements for results
        val resultCard = findViewById<View>(R.id.resultCard)
        val tvMainStatus = findViewById<TextView>(R.id.tvMainStatus)
        val tvDescription = findViewById<TextView>(R.id.tvDescription)
        val tvConfidenceLabel = findViewById<TextView>(R.id.tvConfidenceLabel)
        val confidenceBar = findViewById<LinearProgressIndicator>(R.id.confidenceBar)

        btnScan.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                // Hide card while starting a new scan for better UX
                resultCard.visibility = View.GONE

                checkUrl(url, resultCard, tvMainStatus, tvDescription, tvConfidenceLabel, confidenceBar)
            }
        }
    }

    private fun checkUrl(
        urlToCheck: String,
        card: View,
        title: TextView,
        desc: TextView,
        confLabel: TextView,
        bar: LinearProgressIndicator
    ) {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)

        api.scan(ScanRequest(urlToCheck)).enqueue(object : Callback<ScanResponse> {
            override fun onResponse(call: Call<ScanResponse>, response: Response<ScanResponse>) {
                if (response.isSuccessful) {
                    val body = response.body() ?: return

                    // 1. Show the card
                    card.visibility = View.VISIBLE

                    // 2. Set the text content
                    title.text = body.resultText
                    desc.text = body.description

                    // 3. CONVERT PERCENTAGE AND ANIMATE THE PURPLE BAR
                    // We remove the "%" symbol and turn the string into a number
                    val confNum = try {
                        body.confidence.replace("%", "").toDouble().toInt()
                    } catch (e: Exception) {
                        0
                    }

                    confLabel.text = "AI Confidence: $confNum%"

                    // This moves the purple line to the exact percentage with a smooth slide
                    bar.setProgress(confNum, true)

                    // 4. Set the Title Color based on risk (Red for Danger, Green for Safe)
                    // The Progress Bar stays Purple as you requested
                    when (body.statusCode) {
                        "SAFE" -> title.setTextColor(Color.parseColor("#2E7D32"))
                        "PHISHING" -> title.setTextColor(Color.parseColor("#C62828"))
                        else -> title.setTextColor(Color.parseColor("#F9A825"))
                    }
                }
            }

            override fun onFailure(call: Call<ScanResponse>, t: Throwable) {
                Toast.makeText(this@SpamCheckActivity, "Connection Failed", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

// Data models for Retrofit
data class ScanRequest(@SerializedName("url") val url: String)

data class ScanResponse(
    @SerializedName("status_code") val statusCode: String,
    @SerializedName("result") val resultText: String,
    @SerializedName("desc") val description: String,
    @SerializedName("confidence") val confidence: String
)

interface ApiService {
    @POST("scan")
    fun scan(@Body request: ScanRequest): Call<ScanResponse>
}