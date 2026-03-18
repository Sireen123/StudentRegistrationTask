package com.example.studentregistration

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentregistration.adapter.FaqAdapter
import com.example.studentregistration.data.FirebaseRepo
import com.example.studentregistration.databinding.ActivityStartBinding
import com.example.studentregistration.model.FaqItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ 0.5 sec delay for RTDB sync
        Handler(Looper.getMainLooper()).postDelayed({

            val current = FirebaseRepo.auth.currentUser

            if (current != null) {
                // ✅ Check RTDB after delay
                FirebaseRepo.rtdb.child("users").child(current.uid).get()
                    .addOnSuccessListener { snap ->
                        if (snap.exists()) {
                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()
                        } else {
                            FirebaseRepo.auth.signOut()
                            initUi()
                        }
                    }
                    .addOnFailureListener {
                        initUi()
                    }
            } else {
                initUi()
            }

        }, 500) // ✅ 500ms delay
    }

    private fun initUi() {
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnExistingUser.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.btnNewUser.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<View>(R.id.faqTileOutside).setOnClickListener {
            showFaqBottomSheet()
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

        view.findViewById<ImageView>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }

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