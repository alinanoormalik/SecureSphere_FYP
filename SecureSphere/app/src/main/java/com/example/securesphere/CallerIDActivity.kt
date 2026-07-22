package com.example.securesphere

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
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

    private lateinit var phoneUtil: PhoneNumberUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caller_idactivity)

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

            var formattedE164: String
            var targetRegion = "PK"

            try {
                val parsedNumber: Phonenumber.PhoneNumber = if (rawInput.startsWith("+")) {
                    phoneUtil.parse(rawInput, null)
                } else if (rawInput.startsWith("0")) {
                    phoneUtil.parse(rawInput, "PK")
                } else {
                    phoneUtil.parse(rawInput, Locale.getDefault().country.ifEmpty { "PK" })
                }

                if (!phoneUtil.isValidNumber(parsedNumber)) {
                    etPhone.error = "Invalid phone number detected."
                    return@setOnClickListener
                }
                formattedE164 = phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
                targetRegion = phoneUtil.getRegionCodeForNumber(parsedNumber) ?: "PK"
            } catch (e: Exception) {
                formattedE164 = rawInput
                if (formattedE164.startsWith("0")) formattedE164 = "+92" + formattedE164.substring(1)
                else if (!formattedE164.startsWith("+")) formattedE164 = "+$formattedE164"
            }

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
                        client.newCall(request).execute().body?.string()
                    }
                } catch (e: Exception) {
                    loadingView.visibility = View.GONE
                    btnIdentify.isEnabled = true
                    btnIdentify.text = "Identify Caller"
                    etPhone.error = "Network Timeout"
                    return@launch
                }

                if (responseData != null) {
                    try {
                        val json = JSONObject(responseData)
                        val isValid = json.optBoolean("phone_valid", false)
                        val type = json.optString("phone_type", "unknown").lowercase()
                        val carrier = json.optString("carrier", "").lowercase()
                        val country = json.optString("country", "Unknown")

                        var totalPenalty = 0
                        if (!isValid) totalPenalty = 100 else {
                            when (type) {
                                "voip" -> totalPenalty += 60
                                "premium_rate" -> totalPenalty += 50
                                "toll_free" -> totalPenalty += 40
                                "unknown" -> totalPenalty += 30
                            }
                            val nativeCountry = Locale("", targetRegion).displayCountry
                            if (!country.equals(nativeCountry, ignoreCase = true) && !country.equals("Pakistan", ignoreCase = true)) totalPenalty += 25
                            val spamCarriers = listOf("twilio", "pinger", "textnow", "bandwidth", "nexmo", "plivo", "sinch", "vonage")
                            if (spamCarriers.any { carrier.contains(it) }) totalPenalty += 35
                        }

                        val finalRiskScore = Math.min(totalPenalty, 100)

                        // Apply Dynamic Themed Badges
                        when {
                            finalRiskScore >= 70 -> setBadge(txtAnalysis, "HIGH RISK ($finalRiskScore%)", "#EF4444")
                            finalRiskScore >= 30 -> setBadge(txtAnalysis, "SUSPICIOUS ($finalRiskScore%)", "#F59E0B")
                            else -> setBadge(txtAnalysis, "VERIFIED CARRIER", "#10B981")
                        }

                        txtTarget.text = formattedE164
                        txtCountry.text = "$country (${targetRegion.uppercase()})"
                        txtLineType.text = type.uppercase()
                        txtCarrier.text = if (carrier.isEmpty()) "UNLISTED" else carrier.uppercase()

                    } catch (e: Exception) {
                        etPhone.error = "Data Error"
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

    /**
     * Sets a badge color that is legible in both Dark and Light mode.
     * Uses alpha (transparency) for the background so it doesn't clash with dark themes.
     */
    private fun setBadge(view: TextView, text: String, colorHex: String) {
        val color = Color.parseColor(colorHex)
        view.text = text
        view.setTextColor(color)

        // Create a transparent version of the color for the background (20% opacity)
        val bgColor = ColorUtils.setAlphaComponent(color, 40)
        view.backgroundTintList = ColorStateList.valueOf(bgColor)

        // Ensure the background is actually visible (setting a small rounded corner background)
        view.setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
        // Note: In a production app, you'd use a custom XML shape for the badge background
    }
}