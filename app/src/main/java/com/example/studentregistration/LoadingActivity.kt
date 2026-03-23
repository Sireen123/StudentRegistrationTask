package com.example.studentregistration

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.studentregistration.data.FirebaseRepo
import com.example.studentregistration.databinding.ActivityLoadingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoadingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Disable back while loading
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })

        lifecycleScope.launch {
            delay(800)

            val studentId = intent.getStringExtra(MainActivity.EXTRA_STUDENT_ID)
            val navTarget = intent.getStringExtra(MainActivity.EXTRA_NAV_TARGET) ?: "DASHBOARD"
            val forceDashboard = intent.getBooleanExtra(MainActivity.EXTRA_FORCE_DASHBOARD, true)

            val detailsOk = studentId?.let { checkDetailsSaved(it) } ?: false

            if (!detailsOk) {
                Toast.makeText(
                    this@LoadingActivity,
                    "Preparing your profile…",
                    Toast.LENGTH_SHORT
                ).show()
            }

            goToDashboard(studentId, navTarget, forceDashboard)
        }
    }

    private suspend fun checkDetailsSaved(studentId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val snap = FirebaseRepo.rtdb.child("details").child(studentId).get().await()
            snap.exists()
        } catch (_: Exception) {
            false
        }
    }

    // ✅ Forward ALL extras (User parcelable, arrears, collegeName, etc.)
    private fun goToDashboard(studentId: String?, navTarget: String, forceDashboard: Boolean) {
        val newIntent = Intent(this, DashboardActivity::class.java).apply {

            // Reset activity stack
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

            // Keep original navigation extras
            putExtra(MainActivity.EXTRA_STUDENT_ID, studentId)
            putExtra(MainActivity.EXTRA_NAV_TARGET, navTarget)
            putExtra(MainActivity.EXTRA_FORCE_DASHBOARD, forceDashboard)

            // ✅ Forward EVERYTHING that MainActivity placed in goLoader()
            intent.extras?.let { putExtras(it) }
        }

        startActivity(newIntent)
        finish()
    }
}