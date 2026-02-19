package com.example.studentregistration

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.studentregistration.databinding.ActivityDetailsBinding

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val b = intent.extras
        binding.tvName.text = "Name: ${b?.getString("name").orEmpty()}"
        binding.tvReg.text = "Register No: ${b?.getString("reg").orEmpty()}"
        binding.tvRoll.text = "Roll No: ${b?.getString("roll").orEmpty()}"
        binding.tvAddress.text = "Address: ${b?.getString("address").orEmpty()}"
        binding.tvPhone.text = "Phone: ${b?.getString("phone").orEmpty()}"
        binding.tvEmail.text = "Email: ${b?.getString("email").orEmpty()}"
        binding.tvDob.text = "Date of Birth: ${b?.getString("dob").orEmpty()}"


        listOf(
            binding.tvCourseCSE,
            binding.tvCourseECE,
            binding.tvCourseCIVIL,
            binding.tvCourseMECH,
            binding.tvCourseBTECH,
            binding.tvCourseBARCH,
            binding.tvCourseBCA
        ).forEach { tv ->
            tv.paintFlags = tv.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            tv.isClickable = true
            tv.isFocusable = true
        }


        binding.tvCourseCSE.setOnClickListener {
            openCourse("BE CSE", binding.tvFeeCSE.text.toString())
        }
        binding.tvCourseECE.setOnClickListener {
            openCourse("BE ECE", binding.tvFeeECE.text.toString())
        }
        binding.tvCourseCIVIL.setOnClickListener {
            openCourse("BE CIVIL", binding.tvFeeCIVIL.text.toString())
        }
        binding.tvCourseMECH.setOnClickListener {
            openCourse("BE MECH", binding.tvFeeMECH.text.toString())
        }
        binding.tvCourseBTECH.setOnClickListener {
            openCourse("BTECH", binding.tvFeeBTECH.text.toString())
        }
        binding.tvCourseBARCH.setOnClickListener {
            openCourse("BARCH", binding.tvFeeBARCH.text.toString())
        }
        binding.tvCourseBCA.setOnClickListener {
            openCourse("BCA", binding.tvFeeBCA.text.toString())
        }
    }

    private fun openCourse(course: String, fee: String) {
        val intent = Intent(this, CourseDetailsActivity::class.java).apply {
            putExtra("courseName", course)
            putExtra("courseFee", fee)
        }
        startActivity(intent)
    }
}