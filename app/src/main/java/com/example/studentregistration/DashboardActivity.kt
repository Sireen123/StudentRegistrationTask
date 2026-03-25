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

        //--------------------------------------------
        // ✅ 1. FIX SESSION EXPIRED PROBLEM
        //--------------------------------------------

        // FIRST try to get studentId from Intent (registration flow)
        studentId = intent.getStringExtra(MainActivity.EXTRA_STUDENT_ID)

        // THEN fallback to SharedPreferences (login flow)
        if (studentId.isNullOrEmpty()) {
            val sp = getSharedPreferences("user_session", MODE_PRIVATE)
            studentId = sp.getString("real_uid", null)
        }

        // Last fail-safe → only if both missing
        if (studentId.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, StartActivity::class.java))
            finish()
            return
        }

        subtitle.text = "Loading..."
        progress.visibility = View.VISIBLE

        //--------------------------------------------
        // ✅ 2. READ REGISTRATION DATA IF SENT
        //--------------------------------------------
        user = intent.getParcelableExtra("user")
        hasArrears = intent.getBooleanExtra("hasArrears", false)
        arrearsCount = intent.getIntExtra("arrearsCount", 0)
        collegeName = intent.getStringExtra("collegeName") ?: ""

        //--------------------------------------------
        // ✅ 3. If user came from registration → no Firebase loading needed
        //--------------------------------------------
        if (user != null) {
            subtitle.text = user!!.name
            progress.visibility = View.GONE
            isDashboardReady = true
        } else {
            // ✅ If the user came from login → load from Firebase
            loadUserFromFirebase(studentId!!, progress, subtitle)
        }

        setupGrid()
        setupLogout()
    }

    //--------------------------------------------
    // ✅ Load user data from Firebase
    //--------------------------------------------
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

    //--------------------------------------------
    // ✅ Dashboard Buttons
    //--------------------------------------------
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
                2 -> {
                    if (user == null) return@DashboardAdapter
                    openDetails()
                }
                else -> Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //--------------------------------------------
    // ✅ Logout
    //--------------------------------------------
    private fun setupLogout() {
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            FirebaseRepo.auth.signOut()
            startActivity(Intent(this, StartActivity::class.java))
            finish()
        }
    }

    //--------------------------------------------
    // ✅ Go to Details Screen
    //--------------------------------------------
    private fun openDetails() {
        startActivity(Intent(this, DetailsActivity::class.java).apply {
            putExtra("user", user)
            putExtra("hasArrears", hasArrears)
            putExtra("arrearsCount", arrearsCount)
            putExtra("collegeName", collegeName)
        })
    }

    //--------------------------------------------
    // ✅ FAQ Bottom Sheet
    //--------------------------------------------
    private fun showFaq() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.faq_bottom_sheet, null)
        dialog.setContentView(view)

        view.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }

        val rv = view.findViewById<RecyclerView>(R.id.rvFaq)
        rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        rv.adapter = FaqAdapter(
            listOf(
                FaqItem("How to create account?", "Tap new user."),
                FaqItem("Why OTP?", "For account security."),
                FaqItem("Invalid input?", "Fill all required fields.")
            )
        )

        dialog.show()
    }
}