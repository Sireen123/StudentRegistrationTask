package com.example.studentregistration

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentregistration.adapter.DashboardAdapter
import com.example.studentregistration.data.FirebaseRepo
import com.example.studentregistration.data.User

class DashboardActivity : AppCompatActivity() {

    private val items = listOf(
        "Fees", "FAQ", "My Details", "Refer a Student",
        "Event Calendar", "Daily Attendance", "Hourly Attendance",
        "CAE Result", "ESE Result", "LMS",
        "Library", "Time Table", "Transport", "Outing"
    )

    private var user: User? = null
    private var hasArrears = false
    private var arrearsCount = 0
    private var collegeName = ""
    private var studentId: String? = null
    private var loadingUser = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // From registration
        user = intent.getParcelableExtra("user")
        hasArrears = intent.getBooleanExtra("hasArrears", false)
        arrearsCount = intent.getIntExtra("arrearsCount", 0)
        collegeName = intent.getStringExtra("collegeName") ?: ""

        // Login path → user not passed
        studentId = intent.getStringExtra(MainActivity.EXTRA_STUDENT_ID)
            ?: FirebaseRepo.auth.currentUser?.uid

        findViewById<TextView>(R.id.tvSubtitle).text =
            user?.name ?: "Loading..."

        // Load full user data when login
        if (user == null && studentId != null) {
            loadUser(studentId!!)
        }

        setupDashboardGrid()
        setupLogout()
    }

    private fun loadUser(uid: String) {
        if (loadingUser) return
        loadingUser = true

        FirebaseRepo.rtdb.child("users").child(uid).get()
            .addOnSuccessListener { snap ->

                if (!snap.exists()) {
                    Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show()
                    loadingUser = false
                    return@addOnSuccessListener
                }

                user = User(
                    name = snap.child("name").value.toString(),
                    registerNo = snap.child("registerNo").value.toString(),
                    rollNo = snap.child("rollNo").value.toString(),
                    address = snap.child("address").value.toString(),
                    phone = snap.child("phone").value.toString(),
                    email = snap.child("email").value.toString(),
                    password = "",
                    dob = snap.child("dob").value.toString(),
                    gender = snap.child("gender").value.toString(),
                    parentName = snap.child("parentName").value.toString(),
                    department = snap.child("department").value.toString(),
                    semester = snap.child("semester").value.toString(),
                    role = snap.child("role").value.toString(),
                    feesPaid = snap.child("feesPaid").value.toString(),
                    profilePhoto = snap.child("profilePhoto").value?.toString()
                )

                hasArrears =
                    snap.child("hasArrears").value as? Boolean ?: false

                arrearsCount =
                    (snap.child("arrearsCount").value as? Long)?.toInt() ?: 0

                collegeName =
                    snap.child("collegeName").value?.toString() ?: ""

                findViewById<TextView>(R.id.tvSubtitle).text = user?.name ?: "Welcome"

                loadingUser = false
            }
            .addOnFailureListener {
                loadingUser = false
                Toast.makeText(this, "Unable to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupDashboardGrid() {
        val rv = findViewById<RecyclerView>(R.id.dashboardRecycler)
        rv.layoutManager = GridLayoutManager(this, 2)
        rv.adapter = DashboardAdapter(items) { pos -> handleClick(pos) }
    }

    private fun setupLogout() {
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            FirebaseRepo.auth.signOut()
            startActivity(Intent(this, StartActivity::class.java))
            finish()
        }
    }

    private fun handleClick(pos: Int) {
        when (pos) {

            2 -> {   // ✅ MY DETAILS
                val uid = studentId ?: FirebaseRepo.auth.currentUser?.uid
                if (uid == null) {
                    Toast.makeText(this, "Login required.", Toast.LENGTH_SHORT).show()
                    return
                }

                // still loading?
                if (user == null) {
                    Toast.makeText(this, "Loading profile…", Toast.LENGTH_SHORT).show()
                    loadUser(uid)
                    return
                }

                openDetails()
            }

            else -> Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openDetails() {
        startActivity(Intent(this, DetailsActivity::class.java).apply {
            putExtra("user", user)
            putExtra("hasArrears", hasArrears)
            putExtra("arrearsCount", arrearsCount)
            putExtra("collegeName", collegeName)
            putExtra(MainActivity.EXTRA_STUDENT_ID, studentId)
        })
    }
}
