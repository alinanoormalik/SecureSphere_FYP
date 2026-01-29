package com.example.securesphere

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CallerIDActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caller_idactivity)

        // 1. Setup Views
        val etPhone = findViewById<EditText>(R.id.etPhoneInput)
        val btnIdentify = findViewById<Button>(R.id.btnIdentify)
        val tvResult = findViewById<TextView>(R.id.tvCallerResult)

        // 2. Button Action
        btnIdentify.setOnClickListener {
            // TEST: Show a popup to prove the button works
            Toast.makeText(this, "Button Clicked!", Toast.LENGTH_SHORT).show()

            val phoneNumber = etPhone.text.toString().trim()

            if (phoneNumber.isNotEmpty()) {

                // INSTANT LOGIC (No delays)
                if (phoneNumber.endsWith("000") || phoneNumber.endsWith("666")) {
                    tvResult.text = "⚠️ SPAM CALLER DETECTED!"
                    tvResult.setTextColor(Color.RED)
                } else {
                    tvResult.text = "✅ SAFE NUMBER."
                    tvResult.setTextColor(Color.parseColor("#009688"))
                }

            } else {
                etPhone.error = "Enter a number first"
            }
        }
    }
}