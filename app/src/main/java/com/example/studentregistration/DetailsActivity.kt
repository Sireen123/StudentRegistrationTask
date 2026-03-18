package com.example.studentregistration

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.studentregistration.data.AppDatabase
import com.example.studentregistration.data.User
import com.example.studentregistration.databinding.ActivityDetailsBinding
import com.github.gcacace.signaturepad.views.SignaturePad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// ✅ Works on API 24+ (no desugaring needed)
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private lateinit var session: SessionPrefs
    private var profilePhotoUri: Uri? = null   // ✅ Profile image holder

    // ✅ Change the pattern if you prefer ("dd-MM-yyyy", etc.)
    private val dateFormatter by lazy { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionPrefs(this)

        // ✅ Auto-fill today's date in the "Date:" text above Submit
        setCurrentDate()

        // ✅ Top bar
        binding.includeBack.btnBack.setOnClickListener { finish() }
        binding.includeBack.tvScreenTitle.text = "Student Details"

        // ✅ Email from Register flow (if any)
        val emailFromRegister = intent.getStringExtra("email")
        if (emailFromRegister != null) session.currentUserEmail = emailFromRegister

        // ✅ Read extras
        val hasArrears = intent.getBooleanExtra("hasArrears", false)
        val arrearsCount = intent.getStringExtra("arrearsCount") ?: "0"
        val selectedSem = intent.getStringExtra("selectedSemester")
        val collegeName = session.collegeName ?: ""

        // ✅ Start loading UI
        setLoading(true)

        session.currentUserEmail?.let {
            loadUserDetails(it, selectedSem, hasArrears, arrearsCount, collegeName)
        }

        setupSignature(hasArrears, arrearsCount, collegeName)
    }

    override fun onResume() {
        super.onResume()
        // ✅ In case the app returns another day
        setCurrentDate()
    }

    // =========================
    // Data Load & UI Binding
    // =========================
    private fun loadUserDetails(
        email: String,
        selectedSem: String?,
        hasArrears: Boolean,
        arrearsCount: String,
        college: String
    ) {
        val dao = AppDatabase.getDatabase(this).userDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val user = dao.getUserByEmail(email)
            withContext(Dispatchers.Main) {
                if (user != null) {

                    binding.tvName.text = "Name: ${user.name}"
                    binding.tvReg.text = "Register No: ${user.registerNo}"
                    binding.tvRoll.text = "Roll No: ${user.rollNo}"
                    binding.tvAddress.text = "Address: ${user.address}"
                    binding.tvPhone.text = "Phone: ${user.phone}"
                    binding.tvEmail.text = "Email: ${user.email}"
                    binding.tvDob.text = "Date of Birth: ${user.dob}"
                    binding.tvGender.text = "Gender: ${user.gender}"
                    binding.tvParent.text = "Parent/Guardian: ${user.parentName}"
                    binding.tvDept.text = "Department: ${user.department}"
                    binding.tvSem.text = "Semester: ${selectedSem ?: user.semester}"
                    binding.tvSignedByLabel.text = "Signed by: ${user.name}"

                    // ✅ Load profile image
                    if (!user.profilePhoto.isNullOrBlank()) {
                        profilePhotoUri = Uri.parse(user.profilePhoto)
                        binding.imgProfile.setImageURI(profilePhotoUri)
                    }

                    // ✅ Update fees UI
                    updateFeesUI(user)
                }

                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSubmit.isEnabled = !isLoading
        binding.signPad.isEnabled = !isLoading
    }

    // =========================
    // Fees Calculation + Dots
    // =========================
    private fun updateFeesUI(user: User) {

        val feeMap = mapOf(
            "Computer Science" to 75000,
            "Information Technology" to 75000,
            "Electronics &amp; Communication" to 70000,
            "Electrical &amp; Electronics" to 72000,
            "Mechanical" to 72000,
            "Civil" to 68000,
            "AI &amp; Data Science" to 75000,
            "Cyber Security" to 75000,
            "Bio Technology" to 80000
        )

        val total = feeMap[user.department] ?: 0
        val paid = user.feesPaid.toIntOrNull() ?: 0

        val percent = if (total == 0) 0 else (paid.toFloat() / total * 100).toInt()

        binding.tvPaidAmount.text = "₹$paid"
        binding.tvTotalAmount.text = "₹$total"
        binding.tvBalanceAmount.text = "Balance: ₹${total - paid}"

        // ✅ Animate progress bar
        ObjectAnimator.ofInt(binding.feesProgress, "progress", percent).apply {
            duration = 1200
            interpolator = DecelerateInterpolator()
            start()
        }

        buildDots(percent)
    }

    private fun buildDots(percent: Int) {
        binding.dotsContainer.removeAllViews()

        val dotCount = 10   // 10 dots represent 100%
        val filled = percent / 10

        for (i in 1..dotCount) {
            val dot = View(this)
            dot.layoutParams = LinearLayout.LayoutParams(18, 18).apply {
                setMargins(6, 0, 6, 0)
            }
            dot.setBackgroundResource(
                if (i <= filled) R.drawable.progress_dot_paid
                else R.drawable.progress_dot_empty
            )
            binding.dotsContainer.addView(dot)
        }
    }

    // =========================
    // Signature / Submit Logic
    // =========================
    private fun setupSignature(hasArrears: Boolean, arrearCount: String, college: String) {

        binding.signPad.setOnSignedListener(object : SignaturePad.OnSignedListener {
            override fun onStartSigning() {}
            override fun onSigned() {
                binding.btnSubmit.isEnabled = true
            }
            override fun onClear() {
                binding.btnSubmit.isEnabled = false
            }
        })

        binding.btnClearSign.setOnClickListener { binding.signPad.clear() }

        binding.btnSubmit.setOnClickListener {

            if (binding.signPad.isEmpty) {
                Toast.makeText(this, "Please add signature", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Save signature to cache and pass via FileProvider
            val file = File(cacheDir, "sig_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use {
                binding.signPad.signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            val sigUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )

            startActivity(Intent(this, AcknowledgementActivity::class.java).apply {

                putExtra("name", binding.tvName.text.toString())
                putExtra("reg", binding.tvReg.text.toString())
                putExtra("roll", binding.tvRoll.text.toString())
                putExtra("dept", binding.tvDept.text.toString().replace("Department: ", ""))
                putExtra("sem", binding.tvSem.text.toString().replace("Semester: ", ""))
                putExtra("address", binding.tvAddress.text.toString())
                putExtra("phone", binding.tvPhone.text.toString())
                putExtra("email", binding.tvEmail.text.toString())
                putExtra("parent", binding.tvParent.text.toString())
                putExtra("signature_uri", sigUri)

                putExtra("hasArrears", hasArrears)
                putExtra("arrearsCount", arrearCount)
                putExtra("collegeName", college)

                putExtra("profile_photo_uri", profilePhotoUri?.toString())

                // Optional: pass the shown date too (if you want it on the next screen)
                // putExtra("signed_date", binding.tvSignDate.text.toString())
            })
        }
    }

    // =========================
    // Date Helpers (API 24+)
    // =========================
    private fun setCurrentDate() {
        val today = dateFormatter.format(Date())
        // Your XML `tvSignDate` already has "Date:" as text, so set the full line:
        binding.tvSignDate.text = "Date: $today"
    }
}