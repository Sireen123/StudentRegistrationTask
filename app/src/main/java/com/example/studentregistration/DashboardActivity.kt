package com.example.studentregistration

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentregistration.adapter.DashboardAdapter
import com.example.studentregistration.adapter.FaqAdapter
import com.example.studentregistration.data.FirebaseRepo
import com.example.studentregistration.data.User
import com.example.studentregistration.model.FaqItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class DashboardActivity : AppCompatActivity() {

    private val items = listOf(
        "Fees",            // 0
        "FAQ",             // 1
        "My Details",      // 2
        "Refer a Student", // 3
        "Event Calendar",
        "Daily Attendance",
        "Hourly Attendance",
        "CAE Result",
        "ESE Result",
        "LMS",
        "Library",
        "Time Table",
        "Transport",
        "Outing"
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

        // ✅ Get UID from Login/Registration
        studentId = intent.getStringExtra(MainActivity.EXTRA_STUDENT_ID)
            ?: FirebaseRepo.auth.currentUser?.uid

        if (studentId == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, StartActivity::class.java))
            finish()
            return
        }

        findViewById<TextView>(R.id.tvSubtitle).text = "Loading..."

        // ✅ If user came from registration
        user = intent.getParcelableExtra("user")
        hasArrears = intent.getBooleanExtra("hasArrears", false)
        arrearsCount = intent.getIntExtra("arrearsCount", 0)
        collegeName = intent.getStringExtra("collegeName") ?: ""

        // ✅ If user is null (existing login) → load from Firebase
        if (user == null) loadUser(studentId!!)
        else findViewById<TextView>(R.id.tvSubtitle).text = user?.name

        setupDashboardGrid()
        setupLogout()
    }

    private fun loadUser(uid: String) {
        if (loadingUser) return
        loadingUser = true

        FirebaseRepo.rtdb.child("users").child(uid).get()
            .addOnSuccessListener { snap ->

                loadingUser = false

                if (!snap.exists()) {
                    Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show()
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

                hasArrears = snap.child("hasArrears").value as? Boolean ?: false
                arrearsCount = (snap.child("arrearsCount").value as? Long)?.toInt() ?: 0
                collegeName = snap.child("collegeName").value?.toString() ?: ""

                findViewById<TextView>(R.id.tvSubtitle).text = user?.name
            }
            .addOnFailureListener {
                loadingUser = false
                Toast.makeText(this, "Unable to load profile.", Toast.LENGTH_SHORT).show()
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

            0 -> startActivity(Intent(this, FeesListActivity::class.java)) // ✅ Fees

            1 -> showFaqBottomSheet()                                      // ✅ FAQ bottom sheet

            2 -> {                                                         // ✅ My Details
                if (user == null) {
                    Toast.makeText(this, "Profile loading... try again.", Toast.LENGTH_SHORT).show()
                    return
                }
                openDetails()
            }

            3 -> startActivity(Intent(this, ReferStudentActivity::class.java)) // ✅ Refer Student

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

    // ✅ **FAQ Bottom Sheet — SAME LOGIC AS StartActivity**
    private fun showFaqBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.faq_bottom_sheet, null)
        dialog.setContentView(view)

        // Expand on open
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.65f).toInt()
            }
        }

        // Close button
        view.findViewById<ImageView>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }

        // ✅ FAQ LIST (same as StartActivity)
        val rv = view.findViewById<RecyclerView>(R.id.rvFaq)
        rv.layoutManager = LinearLayoutManager(this)

        rv.adapter = FaqAdapter(
            listOf(
                FaqItem("How to create account?", "Tap New User and fill details."),
                FaqItem("Why OTP?", "For account security."),
                FaqItem("Invalid input?", "Fill all fields properly."),
                FaqItem("Why select department?", "Required for certificate generation."),
                FaqItem("Where is PDF saved?", "Downloads folder.")
            )
        )

        dialog.show()
    }
}