package com.example.studentregistration

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentregistration.adapter.FaqAdapter
import com.example.studentregistration.databinding.ActivityStartBinding
import com.example.studentregistration.model.FaqItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {

        // ✅ Load saved theme before UI loads
        prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val savedTheme = prefs.getString("app_theme", "light")

        if (savedTheme == "dark") {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        initUi()
        initThemeButtons()
    }

    private fun initUi() {
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Existing user
        binding.btnExistingUser.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // New user
        binding.btnNewUser.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // FAQ
        binding.faqTileOutside.setOnClickListener {
            showFaqBottomSheet()
        }
    }

    // ✅ THEME SWITCH BUTTON HANDLER
    private fun initThemeButtons() {

        // 🌞 LIGHT MODE
        binding.btnLight.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            prefs.edit().putString("app_theme", "light").apply()
            recreate()
        }

        // 🌙 DARK MODE
        binding.btnDark.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            prefs.edit().putString("app_theme", "dark").apply()
            recreate()
        }
    }

    private fun showFaqBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.faq_bottom_sheet, null)
        dialog.setContentView(view)

        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.65f).toInt()
            }
        }

        view.findViewById<ImageView>(R.id.btnClose)?.setOnClickListener {
            dialog.dismiss()
        }

        val rv = view.findViewById<RecyclerView>(R.id.rvFaq)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = FaqAdapter(
            listOf(
                FaqItem("How to create account?", "Tap New User and fill details."),
                FaqItem("Why OTP?", "For account security."),
                FaqItem("Invalid input?", "Fill all fields properly."),
                FaqItem("Why select department?", "Required for certificate."),
                FaqItem("Where is PDF saved?", "Downloads folder.")
            )
        )

        dialog.show()
    }
}
