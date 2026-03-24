package com.example.studentregistration

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
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
        "Fees",
        "FAQ",
        "My Details",
        "Refer a Student",
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

    private var isDashboardReady = false   // ✅ prevents tapping before loading

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val subtitle = findViewById<TextView>(R.id.tvSubtitle)
        val progress = findViewById<ProgressBar>(R.id.progressLoading)

        progress.visibility = View.VISIBLE
        subtitle.text = "Loading..."

        // ✅ Get UID
        studentId = intent.getStringExtra(MainActivity.EXTRA_STUDENT_ID)
            ?: FirebaseRepo.auth.currentUser?.uid

        if (studentId == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, StartActivity::class.java))
            finish()
            return
        }

        // ✅ Get user from registration OR fetch from Firebase
        user = intent.getParcelableExtra("user")
        hasArrears = intent.getBooleanExtra("hasArrears", false)
        arrearsCount = intent.getIntExtra("arrearsCount", 0)
        collegeName = intent.getStringExtra("collegeName") ?: ""

        if (user == null) {
            loadUserFromFirebase(studentId!!, progress, subtitle)
        } else {
            subtitle.text = user?.name
            progress.visibility = View.GONE
            isDashboardReady = true
        }

        setupDashboardGrid()
        setupLogout()
    }

    // ✅ FIXED — Always load profile from /users
    private fun loadUserFromFirebase(uid: String, progress: ProgressBar, subtitle: TextView) {

        FirebaseRepo.rtdb.child("users").child(uid).get()
            .addOnSuccessListener { snap ->

                if (!snap.exists()) {
                    Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show()
                    progress.visibility = View.GONE
                    return@addOnSuccessListener
                }

                user = User(
                    name = snap.child("name").value as? String ?: "",
                    registerNo = snap.child("registerNo").value as? String ?: "",
                    rollNo = snap.child("rollNo").value as? String ?: "",
                    address = snap.child("address").value as? String ?: "",
                    phone = snap.child("phone").value as? String ?: "",
                    email = snap.child("email").value as? String ?: "",
                    password = "",
                    dob = snap.child("dob").value as? String ?: "",
                    gender = snap.child("gender").value as? String ?: "",
                    parentName = snap.child("parentName").value as? String ?: "",
                    department = snap.child("department").value as? String ?: "",
                    semester = snap.child("semester").value as? String ?: "",
                    role = snap.child("role").value as? String ?: "",
                    feesPaid = snap.child("feesPaid").value as? String ?: "",
                    profilePhoto = snap.child("profilePhoto").value as? String
                )

                hasArrears = snap.child("hasArrears").value as? Boolean ?: false
                arrearsCount = (snap.child("arrearsCount").value as? Long)?.toInt() ?: 0
                collegeName = snap.child("collegeName").value as? String ?: ""

                subtitle.text = user?.name
                progress.visibility = View.GONE
                isDashboardReady = true
            }
            .addOnFailureListener {
                progress.visibility = View.GONE
                Toast.makeText(this, "Unable to load profile.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupDashboardGrid() {
        val rv = findViewById<RecyclerView>(R.id.dashboardRecycler)
        rv.layoutManager = GridLayoutManager(this, 2)

        rv.adapter = DashboardAdapter(items) { pos ->

            // ✅ Prevent clicks while loading
            if (!isDashboardReady) {
                Toast.makeText(this, "Loading your profile...", Toast.LENGTH_SHORT).show()
                return@DashboardAdapter
            }

            handleClick(pos)
        }
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

            0 -> startActivity(Intent(this, FeesListActivity::class.java))

            1 -> showFaqBottomSheet()

            2 -> {
                if (user == null) {
                    Toast.makeText(this, "Profile loading...", Toast.LENGTH_SHORT).show()
                    return
                }
                openDetails()
            }

            3 -> startActivity(Intent(this, ReferStudentActivity::class.java))

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

    private fun showFaqBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.faq_bottom_sheet, null)
        dialog.setContentView(view)

        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.65f).toInt()
            }
        }

        view.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }

        val rv = view.findViewById<RecyclerView>(R.id.rvFaq)
        rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

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