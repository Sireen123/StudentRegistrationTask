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
import com.google.android.material.bottomsheet.BottomSheetDialog

class DashboardActivity : AppCompatActivity() {

    private var user: User? = null
    private var hasArrears = false
    private var arrearsCount = 0
    private var collegeName = ""
    private var studentId: String? = null
    private var isDashboardReady = false

    private val items = listOf(
        "Fees", "FAQ", "My Details", "Refer a Student",
        "Event Calendar", "Daily Attendance", "Hourly Attendance",
        "CAE Result", "ESE Result", "LMS", "Library",
        "Time Table", "Transport", "Outing"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val subtitle = findViewById<TextView>(R.id.tvSubtitle)
        val progress = findViewById<ProgressBar>(R.id.progressLoading)

        // ✅ 1. GET UID
        studentId = intent.getStringExtra(MainActivity.EXTRA_STUDENT_ID)
        if (studentId.isNullOrEmpty()) {
            val sp = getSharedPreferences("user_session", MODE_PRIVATE)
            studentId = sp.getString("real_uid", null)
        }

        if (studentId.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, StartActivity::class.java))
            finish()
            return
        }

        subtitle.text = "Loading..."
        progress.visibility = View.VISIBLE

        // ✅ 2. LOAD SESSIONPREFS
        val session = SessionPrefs(this)
        val sessionCollege = session.collegeName ?: ""
        val sessionArrears = session.hasArrears
        val sessionArrearCount = session.arrearsCount

        // ✅ 3. READ FROM INTENT (REGISTRATION FLOW)
        val intentCollege = intent.getStringExtra("collegeName")
        val intentHasArrears = intent.getBooleanExtra("hasArrears", sessionArrears)
        val intentArrearCount = intent.getIntExtra("arrearsCount", sessionArrearCount)

        // ✅ Final values
        collegeName = intentCollege ?: sessionCollege
        hasArrears = intentHasArrears
        arrearsCount = intentArrearCount

        user = intent.getParcelableExtra("user")

        // ✅ If came from registration → ready
        if (user != null) {
            subtitle.text = user!!.name
            progress.visibility = View.GONE
            isDashboardReady = true
            setupGrid()
            setupLogout()
            return
        }

        // ✅ Load from Firebase (login)
        loadUserFromFirebase(studentId!!, progress, subtitle)

        setupGrid()
        setupLogout()
    }

    // ✅ FETCH USER DATA
    private fun loadUserFromFirebase(uid: String, progress: ProgressBar, subtitle: TextView) {

        FirebaseRepo.rtdb.child("users").child(uid).get()
            .addOnSuccessListener { snap ->

                if (!snap.exists()) {
                    Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show()
                    progress.visibility = View.GONE
                    isDashboardReady = true
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

                collegeName = snap.child("collegeName").value as? String ?: collegeName
                hasArrears = snap.child("hasArrears").value as? Boolean ?: hasArrears
                arrearsCount = (snap.child("arrearsCount").value as? Long)?.toInt() ?: arrearsCount

                subtitle.text = user!!.name
                progress.visibility = View.GONE
                isDashboardReady = true
            }
            .addOnFailureListener {
                progress.visibility = View.GONE
                isDashboardReady = true
                Toast.makeText(this, "Error loading profile.", Toast.LENGTH_SHORT).show()
            }
    }

    // ✅ GRID CLICK HANDLER
    private fun setupGrid() {
        val rv = findViewById<RecyclerView>(R.id.dashboardRecycler)
        rv.layoutManager = GridLayoutManager(this, 2)

        rv.adapter = DashboardAdapter(items) { pos ->

            if (!isDashboardReady) {
                Toast.makeText(this, "Loading your profile...", Toast.LENGTH_SHORT).show()
                return@DashboardAdapter
            }

            when (pos) {
                0 -> startActivity(Intent(this, FeesListActivity::class.java))
                1 -> showFaq()
                2 -> openDetails()
                else -> Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ LOGOUT
    private fun setupLogout() {
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            FirebaseRepo.auth.signOut()
            startActivity(Intent(this, StartActivity::class.java))
            finish()
        }
    }

    // ✅ OPEN DETAILS
    private fun openDetails() {
        startActivity(Intent(this, DetailsActivity::class.java).apply {
            putExtra("user", user)
            putExtra("hasArrears", hasArrears)
            putExtra("arrearsCount", arrearsCount)
            putExtra("collegeName", collegeName)
            putExtra(MainActivity.EXTRA_STUDENT_ID, studentId)
        })
    }

    // ✅ FIXED FAQ — SAME 5 ITEMS AS StartActivity
    private fun showFaq() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.faq_bottom_sheet, null)
        dialog.setContentView(view)

        view.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }

        val rv = view.findViewById<RecyclerView>(R.id.rvFaq)
        rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        rv.adapter = FaqAdapter(
            listOf(
                FaqItem("How to create account?", "Tap New User and fill details."),
                FaqItem("Why OTP?", "For account security."),
                FaqItem("Invalid input?", "Fill all fields properly."),
                FaqItem("Why select department?", "Required for certificate."),
                FaqItem("Where is PDF saved?", "Downloads folder.")
            )
        )

        dialog.show()
    }
}