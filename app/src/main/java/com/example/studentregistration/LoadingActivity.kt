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

        // Disable back while loading
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* no-op */ }
        })

        lifecycleScope.launch {
            // Short UX delay for loader
            delay(800)
            goToDashboard()
        }
    }

    private fun goToDashboard() {
        startActivity(Intent(this@LoadingActivity, DashboardActivity::class.java).apply {
            // Make Dashboard the root so Back won't return to register or loader
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtras(intent) // forward any extras if needed
        })
        finish()
    }
}

