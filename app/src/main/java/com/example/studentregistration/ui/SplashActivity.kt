package com.example.studentregistration.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.studentregistration.R

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val btn = findViewById<AppCompatButton>(R.id.btnGetStarted)
        btn.setOnClickListener { v ->
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).withEndAction {
                v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                startActivity(Intent(this, com.example.studentregistration.StartActivity::class.java))
                finish()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }.start()
        }
    }
}
