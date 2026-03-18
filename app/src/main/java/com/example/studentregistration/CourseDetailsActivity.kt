package com.example.studentregistration

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.studentregistration.databinding.ActivityCourseDetailsBinding

// ✅ Firebase (optional usage)
import com.example.studentregistration.data.FirebaseRepo
import com.google.firebase.firestore.FieldValue

class CourseDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCourseDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCourseDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Top bar
        binding.includeBack.btnBack.setOnClickListener { finish() }
        binding.includeBack.tvScreenTitle.text = "Course Details"

        // ✅ Correct keys
        val courseName = intent.getStringExtra("course_name").orEmpty()
        val courseFee  = intent.getStringExtra("course_fee").orEmpty()

        // ✅ Set UI
        binding.tvCourseTitle.text = courseName
        binding.tvCourseFee.text = "Fees: ₹$courseFee"
        binding.tvCourseDesc.text = getDescription(courseName)

        // ✅ OPTIONAL: Save selection to Firebase (only if you want)
        // saveCourseToFirebase(courseName, courseFee)
    }

    // ✅ OPTIONAL Firebase save
    private fun saveCourseToFirebase(name: String, fee: String) {
        val uid = FirebaseRepo.auth.currentUser?.uid ?: return

        val data = mapOf(
            "course" to name,
            "fee" to fee,
            "selectedAt" to FieldValue.serverTimestamp()
        )

        FirebaseRepo.db.collection("users")
            .document(uid)
            .collection("courseSelections")
            .add(data)
    }

    // ✅ Course descriptions
    private fun getDescription(name: String): String {
        return when (name) {
            "BE CSE"   -> "Computer Science Engineering focuses on algorithms, OS, DBMS, networks, AI/ML."
            "BE ECE"   -> "Electronics & Communication Engineering covers circuits, embedded systems, IoT."
            "BE CIVIL" -> "Civil Engineering includes structural design and construction management."
            "BE MECH"  -> "Mechanical Engineering covers thermodynamics, robotics, and design."
            "BTECH"    -> "General engineering foundation with multiple technical subjects."
            "BARCH"    -> "Architecture focusing on design, drawing, construction and planning."
            "BCA"      -> "Computer Applications focusing on programming, database, and networking."
            else       -> "Course information not available."
        }
    }
}