package com.example.studentregistration

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.studentregistration.data.AppDatabase
import com.example.studentregistration.data.User
import com.example.studentregistration.databinding.ActivityDetailsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private lateinit var session: SessionPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionPrefs(this)

        session.currentUserEmail?.let { loadUserDetails(it) }

        setCourseClickListeners()
    }

    private fun loadUserDetails(email: String) {
        val dao = AppDatabase.getDatabase(this).userDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val user: User? = dao.getUserByEmail(email)

            withContext(Dispatchers.Main) {
                if (user != null) {

                    // Basic info
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
                    binding.tvSem.text = "Semester: ${user.semester}"

                    // ------------------------------
                    // FEES CALCULATION + PROGRESS
                    // ------------------------------
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

                    // Set labels
                    binding.tvPaidAmount.text = "₹$paid"
                    binding.tvTotalAmount.text = "₹$total"
                    binding.tvBalanceAmount.text = "Balance: ₹$balance"

                    // Progress %
                    val percent =
                        if (total > 0) (paid * 100 / total).coerceIn(0, 100)
                        else 0

                    // Smooth animation
                    ObjectAnimator.ofInt(binding.feesProgress, "progress", percent).apply {
                        duration = 800
                        interpolator = DecelerateInterpolator()
                        start()
                    }
                }
            }
        }
    }

    private fun setCourseClickListeners() {

        binding.tvCourseCSE.setOnClickListener {
            openCourse("BE CSE", "75,000")
        }
        binding.tvCourseECE.setOnClickListener {
            openCourse("BE ECE", "70,000")
        }
        binding.tvCourseCIVIL.setOnClickListener {
            openCourse("BE CIVIL", "68,000")
        }
        binding.tvCourseMECH.setOnClickListener {
            openCourse("BE MECH", "72,000")
        }
        binding.tvCourseBTECH.setOnClickListener {
            openCourse("BTECH", "80,000")
        }
        binding.tvCourseBARCH.setOnClickListener {
            openCourse("BARCH", "85,000")
        }
        binding.tvCourseBCA.setOnClickListener {
            openCourse("BCA", "65,000")
        }
    }

    private fun openCourse(name: String, fee: String) {
        val intent = Intent(this, CourseDetailsActivity::class.java)
        intent.putExtra("courseName", name)
        intent.putExtra("courseFee", fee)
        startActivity(intent)
    }
}