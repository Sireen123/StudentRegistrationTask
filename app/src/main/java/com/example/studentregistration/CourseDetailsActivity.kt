package com.example.studentregistration

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.studentregistration.databinding.ActivityCourseDetailsBinding

class CourseDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCourseDetailsBinding
    private lateinit var session: SessionPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCourseDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionPrefs(this)


        val name = intent.getStringExtra("courseName").orEmpty()
        val fee  = intent.getStringExtra("courseFee").orEmpty()


        binding.tvCourseTitle.text = name
        binding.tvCourseFee.text = "Fees: $fee"
        binding.tvCourseDesc.text = courseDescription(name)

        // Logout button
        binding.btnLogout.setOnClickListener {
            logoutNow()
        }

      val details =   FakeData.getStudents()
    }

    private fun logoutNow() {
        session.logout()
        val i = Intent(this, LoginActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(i)
        finish()
    }

    private fun courseDescription(name: String): String = when (name) {
        "BE CSE"   -> "Computer Science & Engineering focuses on algorithms, OS, DBMS, networks, and AI/ML."
        "BE ECE"   -> "Electronics & Communication Engineering covers circuits, embedded systems and IoT."
        "BE CIVIL" -> "Civil Engineering includes structural design and construction management."
        "BE MECH"  -> "Mechanical Engineering covers design, thermodynamics and robotics."
        "BTECH"    -> "General engineering foundation."
        "BARCH"    -> "Architecture with design and planning."
        "BCA"      -> "Computer Applications focusing on programming and database."
        else       -> "Course information not available."
    }
}