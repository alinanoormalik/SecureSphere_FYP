package com.example.securesphere

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.michaelrocks.libphonenumber.android.Phonenumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale

class CallerIDActivity : AppCompatActivity() {

    // Engine instance for international structural validation
    private lateinit var phoneUtil: PhoneNumberUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caller_idactivity)

        // Contextual engine initialization
        phoneUtil = PhoneNumberUtil.createInstance(this)

        val etPhone = findViewById<EditText>(R.id.etPhoneInput)
        val btnIdentify = findViewById<Button>(R.id.btnIdentify)

        val loadingView = findViewById<LinearLayout>(R.id.loadingView)
        val loadingCaption = findViewById<TextView>(R.id.loadingCaption)
        val outputCard = findViewById<MaterialCardView>(R.id.outputCard)

        val txtTarget = findViewById<TextView>(R.id.txtTarget)
        val txtCountry = findViewById<TextView>(R.id.txtCountry)
        val txtLineType = findViewById<TextView>(R.id.txtLineType)
        val txtCarrier = findViewById<TextView>(R.id.txtCarrier)
        val txtAnalysis = findViewById<TextView>(R.id.txtAnalysis)

        btnIdentify.setOnClickListener {
            val rawInput = etPhone.text.toString().trim()

            if (rawInput.isEmpty()) {
                etPhone.error = "Please enter a number"
                return@setOnClickListener
            }

            // --- SMART DYNAMIC REGION CONFIGURATION ---
            var formattedE164: String
            var targetRegion = "PK"

            try {
                val parsedNumber: Phonenumber.PhoneNumber = if (rawInput.startsWith("+")) {
                    phoneUtil.parse(rawInput, null) // Explicit global identifier matching
                } else if (rawInput.startsWith("0")) {
                    phoneUtil.parse(rawInput, "PK") // Resolves emulator locale mapping errors
                } else {
                    phoneUtil.parse(rawInput, Locale.getDefault().country.ifEmpty { "PK" })
                }

                if (!phoneUtil.isValidNumber(parsedNumber)) {
                    etPhone.error = "Invalid phone number framework detected."
                    return@setOnClickListener
                }

                formattedE164 = phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
                targetRegion = phoneUtil.getRegionCodeForNumber(parsedNumber) ?: "PK"
            } catch (e: Exception) {
                // Failsafe parsing engine fallback
                formattedE164 = rawInput
                if (formattedE164.startsWith("0")) {
                    formattedE164 = "+92" + formattedE164.substring(1)
                } else if (!formattedE164.startsWith("+")) {
                    formattedE164 = "+$formattedE164"
                }
            }

            // --- ANIMATION TRANSITION TO SECURITY SCAN STATE ---
            btnIdentify.isEnabled = false
            btnIdentify.text = "Processing..."
            outputCard.visibility = View.GONE
            loadingView.visibility = View.VISIBLE
            loadingCaption.text = "Analyzing Profile: $formattedE164"

            lifecycleScope.launch(Dispatchers.Main) {
                var responseData: String? = null

                try {
                    responseData = withContext(Dispatchers.IO) {
                        val client = OkHttpClient()
                        val url = "https://api.veriphone.io/v2/verify?phone=$formattedE164&key=2F115CB9DDD94FF5AE4BD6E4AD7B2490"
                        val request = Request.Builder().url(url).build()
                        val response = client.newCall(request).execute()
                        response.body?.string()
                    }
                } catch (e: Exception) {
                    loadingView.visibility = View.GONE
                    btnIdentify.isEnabled = true
                    btnIdentify.text = "Identify Caller"
                    etPhone.error = "Network Timeout: Threat Intel Unreachable"
                    return@launch
                }

                if (responseData != null) {
                    try {
                        val json = JSONObject(responseData)
                        val isValid = json.optBoolean("phone_valid", false)
                        val type = json.optString("phone_type", "unknown").lowercase()
                        val carrier = json.optString("carrier", "").lowercase()
                        val country = json.optString("country", "Unknown")

                        // --- SECURITY PENALTY SYSTEM ---
                        var totalPenalty = 0
                        if (!isValid) {
                            totalPenalty = 100
                        } else {
                            when (type) {
                                "voip" -> totalPenalty += 60
                                "premium_rate" -> totalPenalty += 50
                                "toll_free" -> totalPenalty += 40
                                "unknown" -> totalPenalty += 30
                            }

                            // Native cross-validation mapping configuration
                            val nativeCountry = Locale("", targetRegion).displayCountry
                            if (!country.equals(nativeCountry, ignoreCase = true) && !country.equals("Pakistan", ignoreCase = true)) {
                                totalPenalty += 25
                            }

                            val spamCarriers = listOf("twilio", "pinger", "textnow", "bandwidth", "nexmo", "plivo", "sinch", "vonage")
                            if (spamCarriers.any { carrier.contains(it) }) {
                                totalPenalty += 35
                            }
                        }

                        val finalRiskScore = Math.min(totalPenalty, 100)

                        val statusLabel: String
                        val badgeTextColor: Int
                        val badgeBgColor: Int

                        // Dynamic UX style adjustments matching material patterns
                        when {
                            finalRiskScore >= 70 -> {
                                statusLabel = "HIGH RISK / SCAM LIKELY ($finalRiskScore%)"
                                badgeTextColor = Color.parseColor("#EF4444")
                                badgeBgColor = Color.parseColor("#FEE2E2")
                            }
                            finalRiskScore >= 30 -> {
                                statusLabel = "SUSPICIOUS PROFILE ($finalRiskScore%)"
                                badgeTextColor = Color.parseColor("#F59E0B")
                                badgeBgColor = Color.parseColor("#FEF3C7")
                            }
                            else -> {
                                statusLabel = "VERIFIED LOGISTIC CARRIER"
                                badgeTextColor = Color.parseColor("#10B981")
                                badgeBgColor = Color.parseColor("#E6F4EA")
                            }
                        }

                        // --- INTERFACE METADATA RENDERING ---
                        txtTarget.text = formattedE164
                        txtCountry.text = "$country (${targetRegion.uppercase(Locale.ROOT)})"
                        txtLineType.text = type.uppercase(Locale.ROOT)
                        txtCarrier.text = if (carrier.isEmpty()) "UNLISTED / PRIVATE EXCHANGE" else carrier.uppercase(Locale.ROOT)

                        txtAnalysis.text = statusLabel
                        txtAnalysis.setTextColor(badgeTextColor)
                        txtAnalysis.setBackgroundColor(badgeBgColor)

                    } catch (e: Exception) {
                        etPhone.error = "Analysis Error: Data Stream Corrupted"
                    } finally {
                        loadingView.visibility = View.GONE
                        btnIdentify.isEnabled = true
                        btnIdentify.text = "Identify Caller"
                        outputCard.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
}