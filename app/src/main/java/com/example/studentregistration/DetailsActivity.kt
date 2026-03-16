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

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private lateinit var session: SessionPrefs
    private var profilePhotoUri: Uri? = null   // ✅ Profile image holder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionPrefs(this)

        // ✅ Fix includeBack layout
        binding.includeBack.btnBack.setOnClickListener { finish() }
        binding.includeBack.tvScreenTitle.text = "Student Details"

        val emailFromRegister = intent.getStringExtra("email")
        if (emailFromRegister != null) session.currentUserEmail = emailFromRegister

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

    // ✅ FEES CALCULATION + PROGRESS BAR
    private fun updateFeesUI(user: User) {

        val feeMap = mapOf(
            "Computer Science" to 75000,
            "Information Technology" to 75000,
            "Electronics & Communication" to 70000,
            "Electrical & Electronics" to 72000,
            "Mechanical" to 72000,
            "Civil" to 68000,
            "AI & Data Science" to 75000,
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

    // ✅ SIGNATURE / SUBMIT LOGIC
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
            })
        }
    }
}