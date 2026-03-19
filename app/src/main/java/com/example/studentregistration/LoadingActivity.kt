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
    private var triedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // disable back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { }
        })

        lifecycleScope.launch {
            // Keep your existing loading delay
            delay(900)
            proceedOrRetry()
        }
    }

    private fun proceedOrRetry() {
        val user = FirebaseRepo.auth.currentUser
        if (user == null) {
            // Not logged in → go Start
            startActivity(Intent(this@LoadingActivity, StartActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
            return
        }

        // Check if /users/{uid} exists (created in MainActivity after registration)
        FirebaseRepo.rtdb.child("users").child(user.uid).get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    // ✅ Profile exists → Dashboard
                    startActivity(Intent(this@LoadingActivity, DashboardActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtras(intent)
                    })
                    finish()
                } else {
                    // Retry once after a short delay (RTDB write may still be syncing)
                    retryOnceOrFallback()
                }
            }
            .addOnFailureListener {
                // Network/rules error → retry once then fallback
                retryOnceOrFallback()
            }
    }

    private fun retryOnceOrFallback() {
        if (!triedOnce) {
            triedOnce = true
            lifecycleScope.launch {
                delay(600) // short retry delay
                proceedOrRetry()
            }
        } else {
            startActivity(Intent(this@LoadingActivity, StartActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
        }
    }
}