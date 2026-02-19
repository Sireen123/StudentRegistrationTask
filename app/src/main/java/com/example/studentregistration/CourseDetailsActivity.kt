package com.example.studentregistration

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.studentregistration.databinding.ActivityCourseDetailsBinding

class CourseDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCourseDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCourseDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val name = intent.getStringExtra("courseName").orEmpty()
        val fee  = intent.getStringExtra("courseFee").orEmpty()

        binding.tvCourseTitle.text = name
        binding.tvCourseFee.text = "Fees: $fee"
        binding.tvCourseDesc.text = courseDescription(name)
    }

    private fun courseDescription(name: String): String = when (name) {
        "BE CSE"   -> "Computer Science & Engineering focuses on algorithms, data structures, OS, DBMS, networks, AI/ML, and full‑stack development."
        "BE ECE"   -> "Electronics & Communication Engineering covers circuits, embedded systems, VLSI, DSP, communications, and IoT."
        "BE CIVIL" -> "Civil Engineering includes structural design, materials, surveying, geotechnical, transportation, and construction management."
        "BE MECH"  -> "Mechanical Engineering spans thermodynamics, manufacturing, design, CAD/CAM, fluid mechanics, and robotics."
        "BTECH"    -> "BTech (general) provides a broad engineering foundation with specializations based on institute curriculum."
        "BARCH"    -> "Bachelor of Architecture blends design, planning, and structural fundamentals with studio projects and portfolios."
        "BCA"      -> "Bachelor of Computer Applications focuses on programming, databases, networking, and software development fundamentals."
        else       -> "Course information not available."
    }
}