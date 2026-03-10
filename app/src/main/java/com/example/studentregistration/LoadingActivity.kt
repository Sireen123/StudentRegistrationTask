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

        // Disable back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* no-op */ }
        })

        val extras = intent.extras

        lifecycleScope.launch {
            delay(900)

            // Normalize keys (email may come as "email" or "email_from_login")
            val email =
                extras?.getString("email")
                    ?: extras?.getString("email_from_login")
                    ?: ""

            val hasArrears = extras?.getBoolean("hasArrears", false) ?: false
            val arrearsCount = extras?.getString("arrearsCount") ?: "0"
            val selectedSemester = extras?.getString("selectedSemester")
            val collegeName = extras?.getString("collegeName") ?: ""

            startActivity(Intent(this@LoadingActivity, DetailsActivity::class.java).apply {
                putExtra("email", email)
                putExtra("hasArrears", hasArrears)
                putExtra("arrearsCount", arrearsCount)
                putExtra("selectedSemester", selectedSemester)
                putExtra("collegeName", collegeName)
            })
            finish()
        }
    }
}
