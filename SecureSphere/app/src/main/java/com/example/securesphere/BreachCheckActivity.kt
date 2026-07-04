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

    // Initialize the heavy library once to save RAM
    private val passwordAnalyzer = Zxcvbn()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // YOUR CURRENT LAPTOP IP
    private val BACKEND_BASE = "http://192.168.1.3:5000"

    private var passwordRisk: Int? = null
    private var emailRisk: Int? = null
    private var osintRisk: Int? = null

    private lateinit var tvUnifiedScore: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_breach_check)

        tvUnifiedScore = findViewById(R.id.tvUnifiedScore)

        // Email Section
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val btnCheckEmail = findViewById<Button>(R.id.btnCheckEmail)
        val tvEmailResult = findViewById<TextView>(R.id.tvEmailResult)

        // OSINT Section
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val btnRunSherlock = findViewById<Button>(R.id.btnRunSherlock)
        val tvOsintStatus = findViewById<TextView>(R.id.tvOsintStatus)
        val lvOsintResults = findViewById<ListView>(R.id.lvOsintResults)
        val osintLoading = findViewById<ProgressBar>(R.id.osintLoading)
        val cardOsint = findViewById<View>(R.id.cardOsint)

        // Password Section
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val btnCheck = findViewById<Button>(R.id.btnCheck)

        lvOsintResults.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        btnCheckEmail.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tvEmailResult.text = "Searching databases..."
                tvEmailResult.setTextColor(Color.parseColor("#FFA000"))
                checkEmailBreach(email, tvEmailResult)
            } else {
                Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
            }
        }

        btnRunSherlock.setOnClickListener {
            val username = etUsername.text.toString().trim()
            if (username.isNotEmpty() && !username.contains(" ")) {
                osintLoading.visibility = View.VISIBLE
                cardOsint.visibility = View.GONE
                tvOsintStatus.text = "Scanning footprints..."
                runSherlockScan(username, tvOsintStatus, lvOsintResults, osintLoading, cardOsint)
            } else {
                Toast.makeText(this, "No spaces allowed in username", Toast.LENGTH_SHORT).show()
            }
        }

        btnCheck.setOnClickListener {
            val pwd = etPassword.text.toString().trim()
            if (pwd.isNotEmpty()) {
                tvStatus.text = "Analyzing strength..."
                tvStatus.setTextColor(Color.parseColor("#FFA000"))
                thread {
                    val strength = passwordAnalyzer.measure(pwd)
                    val score = strength.score * 25
                    runOnUiThread { checkBreach(pwd, score, tvStatus) }
                }
            }
        }
    }

    private fun checkEmailBreach(email: String, tvResult: TextView) {
        val url = "$BACKEND_BASE/check_email_breach?email=$email"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { tvResult.text = "Connection Failed" }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: "{}"
                try {
                    val json = JSONObject(body)
                    val status = json.optString("status")
                    val exposed = json.optBoolean("exposed", false)
                    val count = json.optInt("breach_count", 0)
                    val breachesArr = json.optJSONArray("breaches")

                    runOnUiThread {
                        if (status != "SUCCESS") {
                            tvResult.text = "⚠️ Service Busy. Retry in a moment."
                        } else if (!exposed) {
                            tvResult.text = "✅ Account is currently safe."
                            tvResult.setTextColor(Color.parseColor("#2E7D32"))
                            emailRisk = 0
                        } else {
                            val sb = StringBuilder("❌ EXPOSED in $count breaches:\n")
                            breachesArr?.let {
                                for (i in 0 until it.length()) sb.append("• ${it.getString(i)}\n")
                            }
                            tvResult.text = sb.toString()
                            tvResult.setTextColor(Color.parseColor("#C62828"))
                            emailRisk = minOf(100, count * 15)
                        }
                        updateUnifiedScore()
                    }
                } catch (e: Exception) {
                    runOnUiThread { tvResult.text = "API Response Error" }
                }
            }
        })
    }

    private fun runSherlockScan(username: String, tvStatus: TextView, lvResults: ListView, loader: ProgressBar, card: View) {
        val url = "$BACKEND_BASE/scan_username?username=$username"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    loader.visibility = View.GONE
                    tvStatus.text = "Engine Offline"
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: "{}"
                try {
                    val json = JSONObject(body)
                    val arr = json.optJSONArray("platforms_found")
                    val names = mutableListOf<String>()
                    val urls = mutableListOf<String>()

                    arr?.let {
                        for (i in 0 until it.length()) {
                            val obj = it.getJSONObject(i)
                            names.add(obj.getString("platform"))
                            urls.add(obj.getString("url"))
                        }
                    }

                    runOnUiThread {
                        loader.visibility = View.GONE
                        if (names.isEmpty()) {
                            tvStatus.text = "No public fingerprints found."
                        } else {
                            tvStatus.text = "✅ Identified ${names.size} Verified Profiles"
                            card.visibility = View.VISIBLE
                            val adapter = object : ArrayAdapter<String>(this@BreachCheckActivity, android.R.layout.simple_list_item_2, android.R.id.text1, names) {
                                override fun getView(pos: Int, conv: View?, parent: ViewGroup): View {
                                    val v = super.getView(pos, conv, parent)
                                    v.findViewById<TextView>(android.R.id.text1).apply {
                                        setTextColor(Color.parseColor("#6200EE"))
                                        setTypeface(null, Typeface.BOLD)
                                    }
                                    v.findViewById<TextView>(android.R.id.text2).apply {
                                        text = urls[pos]
                                        setTextColor(Color.parseColor("#1565C0"))
                                        textSize = 11f
                                    }
                                    return v
                                }
                            }
                            lvResults.adapter = adapter
                            lvResults.setOnItemClickListener { _, _, p, _ ->
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urls[p])))
                            }
                        }
                        osintRisk = minOf(100, names.size * 10)
                        updateUnifiedScore()
                    }
                } catch (e: Exception) {
                    runOnUiThread { loader.visibility = View.GONE }
                }
            }
        })
    }

    private fun checkBreach(password: String, score: Int, tvStatus: TextView) {
        thread {
            val hash = sha1(password).uppercase()
            val prefix = hash.take(5)
            val suffix = hash.substring(5)

            val request = Request.Builder().url("https://api.pwnedpasswords.com/range/$prefix").build()
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val resBody = response.body?.string() ?: ""
                    val isPwned = resBody.contains(suffix)
                    runOnUiThread {
                        if (isPwned) {
                            tvStatus.text = "❌ BREACHED IN LEAKS!\nStrength: $score/100"
                            tvStatus.setTextColor(Color.parseColor("#C62828"))
                            passwordRisk = 100
                        } else {
                            tvStatus.text = "✅ PASSWORD SECURE\nStrength: $score/100"
                            tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                            passwordRisk = 100 - score
                        }
                        updateUnifiedScore()
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread { tvStatus.text = "Database Timeout" }
                }
            })
        }
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