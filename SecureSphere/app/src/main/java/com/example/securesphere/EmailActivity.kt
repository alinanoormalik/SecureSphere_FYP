package com.example.securesphere

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.AttrRes
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

        val etEmail = findViewById<EditText>(R.id.etEmailText)
        val btnScan = findViewById<Button>(R.id.btnScanEmail)
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

            resultCard.visibility = View.VISIBLE
            tvResultHeader.text = "Analyzing email..."
            // Use theme-aware color for "Analyzing" state
            tvResultHeader.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))

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
                        tvResultHeader.setTextColor(Color.parseColor("#EF5350"))
                        tvResultDetails.text = "Could not reach the analysis servers."
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
                                tvResultHeader.setTextColor(Color.parseColor("#EF5350"))
                                tvResultDetails.text = jsonResponse.optString("result", "An unknown error occurred.")
                                return@runOnUiThread
                            }

                            val label = jsonResponse.optString("classification", "UNKNOWN")
                            val confidence = jsonResponse.optDouble("confidence", 0.0)
                            val serverExplanation = jsonResponse.optString("explanation", "Analysis complete.")

                            tvResultDetails.text = serverExplanation
                            tvConfidencePercent.text = "${"%.1f".format(confidence)}%"
                            pbConfidence.progress = confidence.toInt()
                            tvVerdictLabel.text = "AI Classification : $label"

                            when (label) {
                                "SPAM" -> updateStatusUI(tvResultHeader, pbConfidence, "#EF5350", "Fraudulent Email Detected")
                                "SUSPICIOUS" -> updateStatusUI(tvResultHeader, pbConfidence, "#FFA500", "Suspicious Email")
                                "SHORT" -> updateStatusUI(tvResultHeader, pbConfidence, "#FBC02D", "Analysis Aborted")
                                else -> updateStatusUI(tvResultHeader, pbConfidence, "#4CAF50", "Legitimate Email")
                            }

                        } catch (e: Exception) {
                            tvResultHeader.text = "Parsing Error"
                            tvResultHeader.setTextColor(Color.parseColor("#EF5350"))
                        }
                    }
                }
            })
        }
    }

    // Helper to apply status colors dynamically
    private fun updateStatusUI(header: TextView, progress: ProgressBar, colorHex: String, title: String) {
        val color = Color.parseColor(colorHex)
        header.text = title
        header.setTextColor(color)
        progress.progressTintList = ColorStateList.valueOf(color)
    }

    // Helper to get colors from the active theme
    private fun getThemeColor(@AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }
}