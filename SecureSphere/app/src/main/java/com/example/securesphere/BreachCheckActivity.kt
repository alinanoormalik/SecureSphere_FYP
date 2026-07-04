package com.example.securesphere

import android.graphics.Color
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

    // Fix: Load the heavy Zxcvbn library only once when the activity starts
    private val passwordAnalyzer = Zxcvbn()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val BACKEND_BASE = "http://192.168.1.3:5000" //http://10.0.2.2:5000 for emulator

    private var passwordRisk: Int? = null
    private var emailRisk: Int? = null
    private var osintRisk: Int? = null

    private lateinit var tvUnifiedScore: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_breach_check)

        tvUnifiedScore = findViewById(R.id.tvUnifiedScore)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val btnCheckEmail = findViewById<Button>(R.id.btnCheckEmail)
        val tvEmailResult = findViewById<TextView>(R.id.tvEmailResult)
        val tvEmailSource = findViewById<TextView>(R.id.tvEmailSource)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val btnRunSherlock = findViewById<Button>(R.id.btnRunSherlock)
        val tvOsintStatus = findViewById<TextView>(R.id.tvOsintStatus)
        val lvOsintResults = findViewById<ListView>(R.id.lvOsintResults)

        // Handles the scroll issue for the list
        lvOsintResults.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val btnCheck = findViewById<Button>(R.id.btnCheck)

        btnCheckEmail.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tvEmailResult.text = "Checking breach databases..."
                tvEmailResult.setTextColor(Color.parseColor("#FFA000"))
                checkEmailBreach(email, tvEmailResult, tvEmailSource)
            } else {
                Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show()
            }
        }

        btnRunSherlock.setOnClickListener {
            val username = etUsername.text.toString().trim()
            if (username.isNotEmpty()) {
                tvOsintStatus.text = "ENGINE START: Scanning 400+ Platforms...\n(30-60 seconds)"
                tvOsintStatus.setTextColor(Color.parseColor("#FFA000"))
                lvOsintResults.visibility = View.GONE
                runSherlockScan(username, tvOsintStatus, lvOsintResults)
            } else {
                Toast.makeText(this, "Enter a username first", Toast.LENGTH_SHORT).show()
            }
        }

        btnCheck.setOnClickListener {
            val pwd = etPassword.text.toString().trim()
            if (pwd.isNotEmpty()) {
                tvStatus.text = "Analyzing Security..."
                tvStatus.setTextColor(Color.parseColor("#FFA000"))

                // Run heavy math on a background thread to prevent the "Not Responding" error
                thread {
                    val strength = passwordAnalyzer.measure(pwd)
                    val score = strength.score * 25
                    runOnUiThread {
                        checkBreach(pwd, score, tvStatus)
                    }
                }
            } else {
                Toast.makeText(this, "Enter a password first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkEmailBreach(email: String, tvResult: TextView, tvSource: TextView) {
        val url = "$BACKEND_BASE/check_email_breach?email=$email"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { tvResult.text = "Connection Error"; tvResult.setTextColor(Color.GRAY) }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: "{}"
                try {
                    val json = JSONObject(body)
                    val status = json.optString("status", "ERROR")
                    val exposed = json.optBoolean("exposed", false)
                    val count = json.optInt("breach_count", 0)
                    val source = json.optString("source", "")
                    val breachesArr = json.optJSONArray("breaches")

                    runOnUiThread {
                        if (status != "SUCCESS") {
                            tvResult.text = "⚠️ ${json.optString("message", "Error")}"
                            tvResult.setTextColor(Color.parseColor("#FFA000"))
                        } else if (!exposed) {
                            tvResult.text = "✅ No breaches found."
                            tvResult.setTextColor(Color.parseColor("#2E7D32"))
                            emailRisk = 0
                        } else {
                            val sb = StringBuilder("⚠️ EXPOSED in $count breach(es):\n\n")
                            breachesArr?.let {
                                for (i in 0 until it.length()) sb.append("• ${it.getString(i)}\n")
                            }
                            tvResult.text = sb.toString()
                            tvResult.setTextColor(Color.parseColor("#C62828"))
                            emailRisk = minOf(100, count * 15)
                        }
                        tvSource.text = if (source.isNotEmpty()) "Source: $source" else ""
                        updateUnifiedScore()
                    }
                } catch (e: Exception) {
                    runOnUiThread { tvResult.text = "Error parsing data." }
                }
            }
        })
    }

    private fun runSherlockScan(username: String, tvStatus: TextView, lvResults: ListView) {
        val url = "$BACKEND_BASE/scan_username?username=$username"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { tvStatus.text = "ERROR: Offline"; tvStatus.setTextColor(Color.parseColor("#C62828")) }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: "{}"
                try {
                    val json = JSONObject(body)
                    val count = json.optInt("count", 0)
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
                        if (count == 0) {
                            tvStatus.text = "No public profiles found."
                            lvResults.visibility = View.GONE
                        } else {
                            tvStatus.text = "✅ $count profile(s) found"
                            tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                            val adapter = object : ArrayAdapter<String>(this@BreachCheckActivity, android.R.layout.simple_list_item_2, android.R.id.text1, names) {
                                override fun getView(pos: Int, conv: View?, parent: ViewGroup): View {
                                    val v = super.getView(pos, conv, parent)
                                    v.findViewById<TextView>(android.R.id.text2).apply {
                                        text = urls[pos]
                                        setTextColor(Color.parseColor("#1565C0"))
                                    }
                                    return v
                                }
                            }
                            lvResults.adapter = adapter
                            lvResults.visibility = View.VISIBLE
                            lvResults.setOnItemClickListener { _, _, p, _ ->
                                startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(urls[p])))
                            }
                        }
                        osintRisk = minOf(100, count * 10)
                        updateUnifiedScore()
                    }
                } catch (e: Exception) {
                    runOnUiThread { tvStatus.text = "Error parsing scan." }
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
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread { tvStatus.text = "Network Error" }
                }
                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string() ?: ""
                    val isPwned = responseBody.contains(suffix)
                    runOnUiThread {
                        if (isPwned) {
                            tvStatus.text = "⚠️ BREACHED!\nSecurity Score: $score/100"
                            tvStatus.setTextColor(Color.parseColor("#C62828"))
                            passwordRisk = 100
                        } else {
                            tvStatus.text = "✅ SECURE.\nSecurity Score: $score/100"
                            tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                            passwordRisk = 100 - score
                        }
                        updateUnifiedScore()
                    }
                }
            })
        }
    }

    private fun updateUnifiedScore() {
        val components = mutableListOf<Pair<Int, Double>>()
        emailRisk?.let { components.add(it to 0.35) }
        passwordRisk?.let { components.add(it to 0.40) }
        osintRisk?.let { components.add(it to 0.25) }

        if (components.isEmpty()) return

        val totalWeight = components.sumOf { it.second }
        val score = (components.sumOf { it.first * it.second } / totalWeight).toInt()

        // Fixed formatting of the 'when' statement
        val label = when {
            score <= 30 -> "LOW RISK ✅"
            score <= 60 -> "MODERATE RISK ⚠️"
            else -> "HIGH RISK ❌"
        }

        val color = when {
            score <= 30 -> "#2E7D32"
            score <= 60 -> "#FFA000"
            else -> "#C62828"
        }

        tvUnifiedScore.text = "IDENTITY RISK SCORE: $score/100 — $label\nBased on ${components.size} of 3 checks"
        tvUnifiedScore.setTextColor(Color.parseColor(color))
    }

    private fun sha1(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}