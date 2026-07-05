package com.example.securesphere

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class EmailActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email)

        // Core Input Views
        val etEmail = findViewById<EditText>(R.id.etEmailText)
        val btnScan = findViewById<Button>(R.id.btnScanEmail)

        // Detailed Result Card Views
        val resultCard = findViewById<CardView>(R.id.resultCard)
        val tvResultHeader = findViewById<TextView>(R.id.tvResultHeader)
        val tvResultDetails = findViewById<TextView>(R.id.tvResultDetails)
        val tvVerdictLabel = findViewById<TextView>(R.id.tvVerdictLabel)
        val tvConfidencePercent = findViewById<TextView>(R.id.tvConfidencePercent)
        val pbConfidence = findViewById<ProgressBar>(R.id.pbConfidence)

        btnScan.setOnClickListener {
            val emailText = etEmail.text.toString().trim()

            if (emailText.isEmpty()) {
                etEmail.error = "Please enter email content"
                return@setOnClickListener
            }

            // Set loading placeholder states
            resultCard.visibility = View.VISIBLE
            tvResultHeader.text = "Analyzing email..."
            tvResultHeader.setTextColor(Color.GRAY)
            tvResultDetails.text = "Running dynamic ML vector analysis on the provided content..."
            tvVerdictLabel.text = "Verdict: PENDING"
            tvConfidencePercent.text = "0.0%"
            pbConfidence.progress = 0

            val json = JSONObject()
            json.put("email_text", emailText)

            val body = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://nimra3238.pythonanywhere.com/predict-email")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        tvResultHeader.text = "Connection Failed"
                        tvResultHeader.setTextColor(Color.RED)
                        tvResultDetails.text = "Could not reach the analysis servers. Please check your network settings."
                        tvVerdictLabel.text = "Verdict: ERROR"
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseData = response.body?.string()

                    runOnUiThread {
                        try {
                            val jsonResponse = JSONObject(responseData ?: "")
                            val status = jsonResponse.optString("status", "ERROR")

                            if (status != "SUCCESS") {
                                tvResultHeader.text = "Server Error"
                                tvResultHeader.setTextColor(Color.RED)
                                tvResultDetails.text = jsonResponse.optString("result", "An unknown server anomaly occurred.")
                                tvVerdictLabel.text = "Verdict: ERROR"
                                return@runOnUiThread
                            }

                            val label = jsonResponse.optString("classification", "UNKNOWN")
                            val confidence = jsonResponse.optDouble("confidence", 0.0)

                            // Extract custom breakdown message generated dynamically by backend setup
                            val serverExplanation = jsonResponse.optString("explanation", "Analysis complete.")

                            // Display the unique dynamic line from the backend onto the card
                            tvResultDetails.text = serverExplanation
                            tvConfidencePercent.text = "${"%.1f".format(confidence)}%"
                            pbConfidence.progress = confidence.toInt()
                            tvVerdictLabel.text = "AI Classification : $label"

                            // Handle specific UI colors and headers based on categorization
                            when (label) {
                                "SPAM" -> {
                                    val redColor = Color.parseColor("#EF5350")
                                    tvResultHeader.text = "Fraudulent Email Detected"
                                    tvResultHeader.setTextColor(redColor)
                                    pbConfidence.progressTintList = ColorStateList.valueOf(redColor)
                                }

                                "SUSPICIOUS" -> {
                                    val orangeColor = Color.parseColor("#FFA500")
                                    tvResultHeader.text = "Suspicious Email"
                                    tvResultHeader.setTextColor(orangeColor)
                                    pbConfidence.progressTintList = ColorStateList.valueOf(orangeColor)
                                }

                                "SHORT" -> {
                                    val yellowColor = Color.parseColor("#FBC02D")
                                    tvResultHeader.text = "Analysis Aborted"
                                    tvResultHeader.setTextColor(yellowColor)
                                    pbConfidence.progressTintList = ColorStateList.valueOf(yellowColor)
                                }

                                else -> { // "LEGITIMATE"
                                    val greenColor = Color.parseColor("#4CAF50")
                                    tvResultHeader.text = "Legitimate Email"
                                    tvResultHeader.setTextColor(greenColor)
                                    pbConfidence.progressTintList = ColorStateList.valueOf(greenColor)
                                }
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                            tvResultHeader.text = "Parsing Error"
                            tvResultHeader.setTextColor(Color.RED)
                            tvResultDetails.text = "Failed to translate structural engine properties cleanly."
                            tvVerdictLabel.text = "Verdict: UNKNOWN"
                        }
                    }
                }
            })
        }
    }
}