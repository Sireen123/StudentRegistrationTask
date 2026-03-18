package com.example.studentregistration

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.studentregistration.databinding.ActivityLoadingBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoadingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Disable system back while loading
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* no-op */ }
        })

        // (Optional) read extras if you need them later
        // val email = intent.getStringExtra("email") ?: intent.getStringExtra("email_from_login") ?: ""

        // ✅ Simulate brief loading, then go straight to Dashboard
        lifecycleScope.launch {
            delay(900) // you can change the duration

            val intent = Intent(this@LoadingActivity, DashboardActivity::class.java).apply {
                // ✅ Make Dashboard the new root so back won’t return to Loading
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            finish()
        }
    }
}