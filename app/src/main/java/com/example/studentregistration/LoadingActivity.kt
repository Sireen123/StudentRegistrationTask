package com.example.studentregistration

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.studentregistration.data.FirebaseRepo
import com.example.studentregistration.databinding.ActivityLoadingBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoadingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // disable back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { }
        })

        lifecycleScope.launch {
            delay(900)

            val user = FirebaseRepo.auth.currentUser
            val next = if (user != null) {
                DashboardActivity::class.java
            } else {
                StartActivity::class.java
            }

            startActivity(Intent(this@LoadingActivity, next).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
        }
    }
}