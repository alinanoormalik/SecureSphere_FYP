package com.example.securesphere

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class BreachCheckActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var btnCheck: Button
    private lateinit var tvResult: TextView
    private lateinit var cb2FA: CheckBox
    private lateinit var cbReuse: CheckBox
    private lateinit var cbLegacy: CheckBox
    private lateinit var resultContainer: LinearLayout
    private lateinit var tvRiskBadge: TextView
    private lateinit var cbPhoneLinked: CheckBox
    private lateinit var dynamicBreachList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_breach_check)

        // Initialize UI hooks matching the XML elements
        etEmail = findViewById(R.id.etBreachEmail)
        btnCheck = findViewById(R.id.btnCheckBreach)
        tvResult = findViewById(R.id.tvBreachResult)
        cb2FA = findViewById(R.id.cb2FA)
        cbReuse = findViewById(R.id.cbReuse)
        cbLegacy = findViewById(R.id.cbLegacy)
        cbPhoneLinked = findViewById(R.id.cbPhoneLinked)
        resultContainer = findViewById(R.id.resultContainer)
        tvRiskBadge = findViewById(R.id.tvRiskBadge)
        dynamicBreachList = findViewById(R.id.dynamicBreachList)

        btnCheck.setOnClickListener {
            val emailInput = etEmail.text.toString().trim()

            if (emailInput.isEmpty()) {
                etEmail.error = "Please enter an email"
                return@setOnClickListener
            }

            // Lock UI input controls during API fetch transactions
            btnCheck.isEnabled = false
            btnCheck.text = "Analyzing Threat Vector..."
            resultContainer.visibility = View.GONE
            dynamicBreachList.removeAllViews()

            // Safe Coroutine routing to keep the main interface responsive
            executeRiskAssessment(emailInput)
        }
    }

    private fun executeRiskAssessment(email: String) {
        val q1 = cb2FA.isChecked
        val q2 = cbReuse.isChecked
        val q3 = cbLegacy.isChecked
        val q4 = cbPhoneLinked.isChecked

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // For Android Studio Emulator, 10.0.2.2 reroutes directly to your computer's localhost FastAPI server instance
                val targetUrl = URL("https://nimra3238.pythonanywhere.com/check-breach")
                val conn = targetUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.doOutput = true

                // Structure payload schema components
                val rootJson = JSONObject().apply {
                    put("email", email)
                    put("questionnaire", JSONObject().apply {
                        put("is_2fa_disabled", q1)
                        put("is_password_reused", q2)
                        put("has_legacy_connected_apps", q3)
                        put("is_phone_linked", q4)
                    })
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use { os ->
                    os.write(rootJson.toString())
                    os.flush()
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseString = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(responseString)

                    withContext(Dispatchers.Main) {
                        renderAssessmentResults(jsonResponse)
                    }
                } else {
                    showErrorOnMainThread("Engine error code: ${conn.responseCode}")
                }
            } catch (e: Exception) {
                showErrorOnMainThread("Network offline. Confirm FastAPI is hosting local server port loop.")
            }
        }
    }

    private fun renderAssessmentResults(data: JSONObject) {
        btnCheck.isEnabled = true
        btnCheck.text = "Run Security Audit"
        resultContainer.visibility = View.VISIBLE

        val score = data.getInt("vulnerability_index_percentage")
        val grade = data.getString("risk_grade")

        tvResult.text = "Vulnerability Index Percentage: $score%"
        tvRiskBadge.text = grade

        when (grade) {
            "CRITICAL RISK" -> {
                tvRiskBadge.setBackgroundColor(Color.parseColor("#D32F2F"))
                tvRiskBadge.setTextColor(Color.WHITE)
            }
            "MEDIUM RISK" -> {
                tvRiskBadge.setBackgroundColor(Color.parseColor("#F57C00"))
                tvRiskBadge.setTextColor(Color.WHITE)
            }
            else -> {
                tvRiskBadge.setBackgroundColor(Color.parseColor("#388E3C"))
                tvRiskBadge.setTextColor(Color.WHITE)
            }
        }

        val breachLogs: JSONArray = data.getJSONArray("breach_logs")
        if (breachLogs.length() == 0) {
            val clearTxt = TextView(this).apply {
                text = "✔ Safe baseline status. No dark web data exposures discovered."
                setTextColor(Color.GREEN)
                setPadding(0, 8, 0, 8)
            }
            dynamicBreachList.addView(clearTxt)
        } else {
            for (i in 0 until breachLogs.length()) {
                val leakObj = breachLogs.getJSONObject(i)
                val entryField = TextView(this).apply {
                    text = "⚠ Leak: ${leakObj.getString("breach_name")} (${leakObj.getInt("year")})\n   Exposed elements: ${leakObj.getString("exposed_data")}"
                    setTextColor(Color.parseColor("#FFCC00"))
                    setPadding(0, 10, 0, 10)
                }
                dynamicBreachList.addView(entryField)
            }
        }
    }

    private suspend fun showErrorOnMainThread(message: String) {
        withContext(Dispatchers.Main) {
            btnCheck.isEnabled = true
            btnCheck.text = "Run Security Audit"
            Toast.makeText(this@BreachCheckActivity, message, Toast.LENGTH_LONG).show()
        }
    }
}