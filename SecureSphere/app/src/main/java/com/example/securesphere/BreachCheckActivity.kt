package com.example.securesphere

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.nulabinc.zxcvbn.Zxcvbn
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class BreachCheckActivity : AppCompatActivity() {

    private val passwordAnalyzer = Zxcvbn()

    // Increased timeouts to 60s to ensure Sherlock has time to finish
    private val client = OkHttpClient.Builder()
        .connectTimeout(200, TimeUnit.SECONDS)
        .readTimeout(200, TimeUnit.SECONDS)
        .writeTimeout(200, TimeUnit.SECONDS)
        .build()

    // YOUR CURRENT IP FROM CMD
    private val BACKEND_BASE = "http://192.168.43.233:5000 "

    private var passwordRisk: Int? = null
    private var emailRisk: Int? = null
    private var osintRisk: Int? = null

    private lateinit var tvUnifiedScore: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_breach_check)

        tvUnifiedScore = findViewById(R.id.tvUnifiedScore)
        val osintLoading = findViewById<ProgressBar>(R.id.osintLoading)
        val cardOsint = findViewById<View>(R.id.cardOsint)
        val tvOsintStatus = findViewById<TextView>(R.id.tvOsintStatus)
        val lvOsintResults = findViewById<ListView>(R.id.lvOsintResults)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        lvOsintResults.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        // --- STEP 1: EMAIL BUTTON ---
        findViewById<Button>(R.id.btnCheckEmail).setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                findViewById<TextView>(R.id.tvEmailResult).text = "Checking breach databases..."
                findViewById<TextView>(R.id.tvEmailResult).setTextColor(Color.parseColor("#FFA000"))
                checkEmailBreach(email, findViewById(R.id.tvEmailResult))
            }
        }

        // --- STEP 2: OSINT BUTTON ---
        // --- STEP 2: THE OSINT BUTTON (FIXED) ---
        findViewById<Button>(R.id.btnRunSherlock).setOnClickListener {
            // 1. Get the username
            val etUsername = findViewById<EditText>(R.id.etUsername)
            val username = etUsername.text.toString().trim()

            // 2. References to the views
            val osintLoading = findViewById<ProgressBar>(R.id.osintLoading)
            val cardOsint = findViewById<View>(R.id.cardOsint)
            val tvOsintStatus = findViewById<TextView>(R.id.tvOsintStatus)

            if (username.isNotEmpty() && !username.contains(" ")) {
                // --- FIX STARTS HERE ---

                // First, show the Card so the user can see the messages inside it
                cardOsint.visibility = View.VISIBLE

                // Show the loading spinner
                osintLoading.visibility = View.VISIBLE

                // Update the status text immediately
                tvOsintStatus.visibility = View.VISIBLE
                tvOsintStatus.text = "ENGINE START: Scanning 400+ Platforms...\n(This usually takes 40-50 seconds)"
                tvOsintStatus.setTextColor(Color.parseColor("#FFA000")) // Orange

                // Clear the list from previous searches so it looks fresh
                findViewById<ListView>(R.id.lvOsintResults).adapter = null

                // Start the backend scan
                runSherlockScan(username, tvOsintStatus, findViewById(R.id.lvOsintResults), osintLoading, cardOsint)

                // --- FIX ENDS HERE ---
            } else {
                Toast.makeText(this, "Enter username without spaces", Toast.LENGTH_SHORT).show()
            }
        }

        // --- STEP 3: PASSWORD BUTTON ---
        findViewById<Button>(R.id.btnCheck).setOnClickListener {
            val pwd = etPassword.text.toString().trim()
            if (pwd.isNotEmpty()) {
                findViewById<TextView>(R.id.tvStatus).text = "Analyzing Security..."
                findViewById<TextView>(R.id.tvStatus).setTextColor(Color.parseColor("#FFA000"))
                thread {
                    val strength = passwordAnalyzer.measure(pwd)
                    runOnUiThread { checkBreach(pwd, strength.score * 25, findViewById(R.id.tvStatus)) }
                }
            }
        }
    }

    private fun runSherlockScan(username: String, tvStatus: TextView, lvResults: ListView, loader: ProgressBar, card: View) {
        val request = Request.Builder().url("$BACKEND_BASE/scan_username?username=$username").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    loader.visibility = View.GONE
                    tvStatus.text = "Connection Failed: Check Backend IP"
                    tvStatus.setTextColor(Color.RED)
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: "{}"
                try {
                    val json = JSONObject(body)
                    val arr = json.optJSONArray("platforms_found")
                    val names = mutableListOf<String>()
                    val urls = mutableListOf<String>()
                    for (i in 0 until (arr?.length() ?: 0)) {
                        val obj = arr!!.getJSONObject(i)
                        names.add(obj.getString("platform"))
                        urls.add(obj.getString("url"))
                    }
                    runOnUiThread {
                        loader.visibility = View.GONE
                        if (names.isEmpty()) {
                            tvStatus.text = "No public footprints found."
                            tvStatus.setTextColor(Color.GRAY)
                            card.visibility = View.GONE
                        } else {
                            // RESTORE CARD VISIBILITY
                            card.visibility = View.VISIBLE
                            tvStatus.text = "✅ Identified ${names.size} Verified Profiles\n(Tap any link to verify in browser)"
                            tvStatus.setTextColor(Color.parseColor("#2E7D32")) // Green

                            lvResults.adapter = object : ArrayAdapter<String>(this@BreachCheckActivity, android.R.layout.simple_list_item_2, android.R.id.text1, names) {
                                override fun getView(pos: Int, conv: View?, parent: ViewGroup): View {
                                    val v = super.getView(pos, conv, parent)
                                    v.findViewById<TextView>(android.R.id.text1).setTextColor(Color.parseColor("#6200EE"))
                                    v.findViewById<TextView>(android.R.id.text1).setTypeface(null, Typeface.BOLD)
                                    v.findViewById<TextView>(android.R.id.text2).text = urls[pos]
                                    v.findViewById<TextView>(android.R.id.text2).setTextColor(Color.parseColor("#1565C0"))
                                    return v
                                }
                            }
                            lvResults.setOnItemClickListener { _, _, p, _ -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urls[p]))) }
                        }
                        osintRisk = minOf(100, names.size * 10)
                        updateUnifiedScore()
                    }
                } catch (e: Exception) { runOnUiThread { loader.visibility = View.GONE } }
            }
        })
    }

    private fun checkEmailBreach(email: String, tvResult: TextView) {
        val request = Request.Builder().url("$BACKEND_BASE/check_email_breach?email=$email").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { tvResult.text = "Connection Failed" }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: "{}"
                try {
                    val json = JSONObject(body)
                    val exposed = json.optBoolean("exposed", false)
                    val count = json.optInt("breach_count", 0)
                    val breachesArr = json.optJSONArray("breaches")
                    runOnUiThread {
                        if (!exposed) {
                            tvResult.text = "✅ No breaches found for this email."
                            tvResult.setTextColor(Color.parseColor("#2E7D32"))
                            emailRisk = 0
                        } else {
                            val sb = StringBuilder("⚠️ EXPOSED in $count breach(es):\n\n")
                            breachesArr?.let { for (i in 0 until it.length()) sb.append("• ${it.getString(i)}\n") }
                            tvResult.text = sb.toString()
                            tvResult.setTextColor(Color.parseColor("#C62828"))
                            emailRisk = minOf(100, count * 15)
                        }
                        updateUnifiedScore()
                    }
                } catch (e: Exception) { runOnUiThread { tvResult.text = "Error parsing response" } }
            }
        })
    }

    private fun checkBreach(pwd: String, score: Int, tvStatus: TextView) {
        val hash = sha1(pwd).uppercase()
        val prefix = hash.take(5)
        val suffix = hash.substring(5)
        val request = Request.Builder().url("https://api.pwnedpasswords.com/range/$prefix").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val resBody = response.body?.string() ?: ""
                val isPwned = resBody.contains(suffix)
                runOnUiThread {
                    if (isPwned) {
                        tvStatus.text = "⚠️ BREACHED! Found in public leaks.\nSecurity Strength: $score/100"
                        tvStatus.setTextColor(Color.parseColor("#C62828"))
                        passwordRisk = 100
                    } else {
                        tvStatus.text = "✅ SECURE. Not found in leaks.\nSecurity Strength: $score/100"
                        tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                        passwordRisk = 100 - score
                    }
                    updateUnifiedScore()
                }
            }
            override fun onFailure(call: Call, e: IOException) { runOnUiThread { tvStatus.text = "Internet Error" } }
        })
    }

    private fun updateUnifiedScore() {
        val comps = mutableListOf<Pair<Int, Double>>()
        emailRisk?.let { comps.add(it to 0.35) }
        passwordRisk?.let { comps.add(it to 0.40) }
        osintRisk?.let { comps.add(it to 0.25) }
        if (comps.isEmpty()) return
        val totalWeight = comps.sumOf { it.second }
        val finalScore = (comps.sumOf { it.first * it.second } / totalWeight).toInt()
        val label = when {
            finalScore <= 30 -> "LOW RISK ✅"
            finalScore <= 60 -> "MODERATE RISK ⚠️"
            else -> "HIGH RISK ❌"
        }
        val color = when {
            finalScore <= 30 -> "#2E7D32"
            finalScore <= 60 -> "#FFA000"
            else -> "#C62828"
        }
        tvUnifiedScore.text = "IDENTITY RISK SCORE: $finalScore/100 — $label\nBased on ${comps.size} of 3 checks"
        tvUnifiedScore.setTextColor(Color.parseColor(color))
    }

    private fun sha1(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}