package com.example.studentregistration

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.studentregistration.data.FirebaseRepo
import com.example.studentregistration.data.User

class CertificateActivity : AppCompatActivity() {

    private var studentId: String? = null
    private var user: User? = null
    private var hasArrears = false
    private var arrearsCount = 0
    private var collegeName = ""
    private var signatureUri: Uri? = null
    private var signedOn = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_certificate)

        // ✅ Read parcelable user
        user = intent.getParcelableExtra("user")
        hasArrears = intent.getBooleanExtra("hasArrears", false)
        arrearsCount = intent.getIntExtra("arrearsCount", 0)
        collegeName = intent.getStringExtra("collegeName") ?: ""
        signedOn = intent.getStringExtra("signed_on") ?: ""

        signatureUri =
            if (Build.VERSION.SDK_INT >= 33)
                intent.getParcelableExtra("signature_uri", Uri::class.java)
            else @Suppress("DEPRECATION")
            intent.getParcelableExtra("signature_uri")

        studentId = intent.getStringExtra(MainActivity.EXTRA_STUDENT_ID)
            ?: FirebaseRepo.auth.currentUser?.uid

        // Back logic
        findViewById<Button>(R.id.btnBackToDashboard).setOnClickListener { goDashboard() }
        onBackPressedDispatcher.addCallback(this) { goDashboard() }

        if (user == null) {
            goDashboard()
            return
        }

        if (savedInstanceState == null) loadFragment()
    }

    private fun loadFragment() {
        val u = user!!

        // Final decision
        val isArrear = hasArrears || arrearsCount > 0

        // ✅ Update workflow
        updateWorkflow(isArrear)

        // ✅ Bundle for fragment
        val args = Bundle().apply {
            putParcelable("user", u)
            putString("collegeName", collegeName)
            putBoolean("hasArrears", isArrear)
            putInt("arrearsCount", arrearsCount)
            putParcelable("signature_uri", signatureUri)
            putString("signed_on", signedOn)
        }

        val fragment =
            if (isArrear)
                ArrearCertificateFragment().apply { arguments = args }
            else
                FinalCertificateFragment().apply { arguments = args }

        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun updateWorkflow(isArrear: Boolean) {
        val uid = studentId ?: return

        val updates = mapOf(
            "detailsSaved" to true,
            "ackPending" to false,
            "ackCompleted" to true,
            "certPending" to false,
            "certIssuedAt" to System.currentTimeMillis(),
            "certType" to if (isArrear) "ARREAR" else "FINAL",
            "certArrear" to isArrear,
            "certFinal" to !isArrear
        )

        FirebaseRepo.rtdb
            .child("details")
            .child(uid)
            .child("workflow")
            .updateChildren(updates)
    }

    private fun goDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_STUDENT_ID, studentId)
        })
        finish()
    }
}
