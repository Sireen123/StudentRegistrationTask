package com.example.studentregistration

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentregistration.adapter.FaqAdapter
import com.example.studentregistration.databinding.ActivityStartBinding
import com.example.studentregistration.model.FaqItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnExistingUser.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.btnNewUser.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // ✅ OUTSIDE FAQ tile button click
        findViewById<View>(R.id.faqTileOutside).setOnClickListener {
            showFaqBottomSheet()
        }
    }

    private fun showFaqBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.faq_bottom_sheet, null)
        dialog.setContentView(view)

        // expand auto
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (sheet != null) {
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.65f).toInt()
            }
        }

        // ✅ Correct close button ID here
        val closeBtn = view.findViewById<ImageView>(R.id.btnClose)
        closeBtn?.setOnClickListener { dialog.dismiss() }

        // FAQ List setup
        val rv = view.findViewById<RecyclerView>(R.id.rvFaq)
        rv.layoutManager = LinearLayoutManager(this)

        val list = listOf(
            FaqItem("How to create account?", "Tap New User and fill details."),
            FaqItem("Why OTP?", "For account security."),
            FaqItem("Invalid input?", "Fill all fields properly."),
            FaqItem("Why select department?", "Required for certificate."),
            FaqItem("Where is PDF saved?", "Inside Downloads folder.")
        )

        rv.adapter = FaqAdapter(list)
        dialog.show()
    }
}