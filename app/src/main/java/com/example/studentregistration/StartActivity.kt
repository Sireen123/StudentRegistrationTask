package com.example.studentregistration

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.studentregistration.databinding.ActivityStartBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Existing flows
        binding.btnExistingUser.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.btnNewUser.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // ✅ FAB → Show FAQ Bottom Sheet
        binding.fabFaq.setOnClickListener {
            showFaqBottomSheet()
        }
    }

    // ---------------------------------------------------------
    // Bottom Sheet with clean, short, real FAQs
    // ---------------------------------------------------------
    private fun showFaqBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.faq_bottom_sheet, null)
        dialog.setContentView(view)

        val tvContent = view.findViewById<TextView>(R.id.tvFaqContent)

        // ✅ Real-app style FAQs (based on your Activities)
        tvContent.text = """
1) How do I create a new student account?
→ Tap ‘New User’, fill the registration form, and submit.

2) Why do I need phone OTP during login?
→ To protect your account from duplicate login.

3) My phone shows “Invalid”. What should I check?
→ Fill all the Details Properly .

4) Why must I select Department and Semester?
→ They are needed for fee calculation and certificate details.

5) How are fees shown after registration?
→ You’ll see total, paid, and balance in the next screen.

6) Why do I need to add a digital signature?
→ To confirm your details before generating the certificate.

7) Where is my certificate PDF saved?
→ In your device’s Downloads folder. You can also share it directly.

8) Why are there different certificates?
→ The app shows a final certificate or arrear certificate based on your arrear status.
        """.trimIndent()

        dialog.show()
    }
}