package com.example.studentregistration

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private lateinit var session: SessionPrefs

    private var extraHasArrears: Boolean = false
    private var extraArrearsCount: String = "0"
    private var extraSelectedSem: String? = null
    private var collegeName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionPrefs(this)

        // Back + title
        binding.root.findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }
        binding.root.findViewById<TextView>(R.id.tvScreenTitle)?.text = "Student Details"

        // Email from previous screen
        val emailFromRegister = intent.getStringExtra("email")
        if (emailFromRegister != null) session.currentUserEmail = emailFromRegister

        // Extras
        extraHasArrears = intent.getBooleanExtra("hasArrears", false)
        extraArrearsCount = intent.getStringExtra("arrearsCount") ?: "0"
        extraSelectedSem = intent.getStringExtra("selectedSemester")
        collegeName = session.collegeName ?: ""

        // Arrears UI
        binding.groupArrears.visibility = View.VISIBLE
        if (extraHasArrears) {
            binding.tvHasArrears.text = "Arrears History: Yes"
            binding.tvArrearsCount.text = "No. of arrears: $extraArrearsCount"
        } else {
            binding.tvHasArrears.text = "Arrears History: No"
            binding.tvArrearsCount.text = "No. of arrears: 0"
        }

        // Load user from DB
        session.currentUserEmail?.let { loadUserDetails(it) }

        // Course listeners
        setCourseClickListeners()

        // Signature
        setupSignature()

        updateSubmitEnabled()
    }

    private fun setupSignature() {
        binding.sectionSignature.visibility = View.VISIBLE

        binding.signPad.setOnSignedListener(object : SignaturePad.OnSignedListener {
            override fun onStartSigning() {}

            override fun onSigned() {
                binding.tvSignDate.text = "Date: ${today()}"
                binding.tvSignHash.text = computeShortHash()
                updateSubmitEnabled()
            }

            override fun onClear() {
                binding.tvSignDate.text = "Date:"
                binding.tvSignHash.text = ""
                updateSubmitEnabled()
            }
        })

        binding.btnClearSign.setOnClickListener { binding.signPad.clear() }

        binding.btnSubmit.setOnClickListener {
            if (binding.signPad.isEmpty) {
                Toast.makeText(this, "Please add signature", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val file = File(cacheDir, "signature_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use {
                binding.signPad.signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)

            startActivity(
                Intent(this, AcknowledgementActivity::class.java).apply {
                    putExtra("name", binding.tvName.text.toString())
                    putExtra("reg", binding.tvReg.text.toString())
                    putExtra("roll", binding.tvRoll.text.toString())
                    putExtra("dept", binding.tvDept.text.toString().replace("Department: ", "").trim())
                    putExtra("sem", binding.tvSem.text.toString().replace("Semester: ", "").trim())
                    putExtra("address", binding.tvAddress.text.toString())
                    putExtra("phone", binding.tvPhone.text.toString())
                    putExtra("email", binding.tvEmail.text.toString())
                    putExtra("parent", binding.tvParent.text.toString())
                    putExtra("signature_uri", uri)
                    putExtra("signed_on", today())
                    putExtra("hasArrears", extraHasArrears)
                    putExtra("arrearsCount", extraArrearsCount)
                    putExtra("collegeName", collegeName)
                }
            )
        }
    }

    private fun loadUserDetails(email: String) {
        val dao = AppDatabase.getDatabase(this).userDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val user = dao.getUserByEmail(email)

            withContext(Dispatchers.Main) {
                if (user != null) {

                    // Assign user values to UI
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
                    binding.tvSem.text = "Semester: ${extraSelectedSem ?: user.semester}"
                    binding.tvSignedByLabel.text = "Signed by: ${user.name}"

                    // Fee structure
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
                    val balance = (total - paid).coerceAtLeast(0)

                    binding.tvPaidAmount.text = "₹$paid"
                    binding.tvTotalAmount.text = "₹$total"
                    binding.tvBalanceAmount.text = "Balance: ₹$balance"

                    // Progress bar percentage
                    val percent = if (total > 0) (paid * 100 / total) else 0

                    ObjectAnimator.ofInt(binding.feesProgress, "progress", percent).apply {
                        duration = 800
                        interpolator = DecelerateInterpolator()
                        start()
                    }

                    // ✅ DOTS FIX — final correct logic
                    loadDots(total, paid)
                }
            }
        }
    }

    // ✅ FINAL DOT GENERATION (works 100%)
    private fun loadDots(total: Int, paid: Int) {

        val step = 25000
        val totalDots = if (total <= 0) 1 else ceil(total / step.toDouble()).toInt()
        val filledDots = (paid / step).coerceAtMost(totalDots)

        val container = binding.dotsContainer
        container.removeAllViews()

        repeat(totalDots) { index ->

            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(10), dp(10)).apply {
                    setMargins(dp(6), 0, dp(6), 0)
                }

                if (index < filledDots)
                    setBackgroundResource(R.drawable.progress_dot_paid)
                else
                    setBackgroundResource(R.drawable.progress_dot_future)
            }

            container.addView(dot)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun updateSubmitEnabled() {
        binding.btnSubmit.isEnabled = !binding.signPad.isEmpty
    }

    private fun today(): String =
        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())

    private fun computeShortHash(): String {
        val bmp = binding.signPad.signatureBitmap
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val bytes = baos.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
            .uppercase()
            .take(16)
    }

    private fun setCourseClickListeners() {
        binding.tvCourseCSE.setOnClickListener { openCourse("BE CSE", "75,000") }
        binding.tvCourseECE.setOnClickListener { openCourse("BE ECE", "70,000") }
        binding.tvCourseCIVIL.setOnClickListener { openCourse("BE CIVIL", "68,000") }
        binding.tvCourseMECH.setOnClickListener { openCourse("BE MECH", "72,000") }
        binding.tvCourseBTECH.setOnClickListener { openCourse("BTECH", "80,000") }
        binding.tvCourseBARCH.setOnClickListener { openCourse("BARCH", "85,000") }
        binding.tvCourseBCA.setOnClickListener { openCourse("BCA", "65,000") }
    }

    private fun openCourse(name: String, fee: String) {
        startActivity(
            Intent(this, CourseDetailsActivity::class.java).apply {
                putExtra("courseName", name)
                putExtra("courseFee", fee)
            }
        )
    }
}