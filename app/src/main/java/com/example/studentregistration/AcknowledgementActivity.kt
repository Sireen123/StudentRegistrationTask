package com.example.studentregistration

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studentregistration.data.FirebaseRepo
import com.example.studentregistration.data.User
import com.example.studentregistration.databinding.ActivityAcknowledgementBinding
import java.text.SimpleDateFormat
import java.util.*

class AcknowledgementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAcknowledgementBinding

    private var user: User? = null
    private var hasArrears = false
    private var arrearsCount = 0
    private var collegeName = ""
    private var signatureUri: Uri? = null
    private var uid: String? = null

    private val dateFormat =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val today = dateFormat.format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAcknowledgementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back bar
        binding.includeBack.tvScreenTitle.text = "Acknowledgement"
        binding.includeBack.btnBack.setOnClickListener { finish() }

        readIntentData()
        setupUI()
        setupListeners()
    }

    private fun readIntentData() {
        user = intent.getParcelableExtra("user")
        hasArrears = intent.getBooleanExtra("hasArrears", false)
        arrearsCount = intent.getIntExtra("arrearsCount", 0)
        collegeName = intent.getStringExtra("collegeName") ?: ""

        signatureUri =
            if (Build.VERSION.SDK_INT >= 33)
                intent.getParcelableExtra("signature_uri", Uri::class.java)
            else @Suppress("DEPRECATION")
            intent.getParcelableExtra("signature_uri")

        uid = intent.getStringExtra(MainActivity.EXTRA_STUDENT_ID)
            ?: FirebaseRepo.auth.currentUser?.uid
    }

    private fun setupUI() {
        val u = user ?: return

        binding.tvName.text = u.name
        binding.tvReg.text = u.registerNo
        binding.tvRoll.text = u.rollNo
        binding.tvDept.text = u.department
        binding.tvSem.text = u.semester
        binding.tvParent.text = u.parentName
        binding.tvCollege.text = collegeName

        binding.tvArrearFlag.text =
            if (hasArrears || arrearsCount > 0) "Yes ($arrearsCount)" else "No"
    }

    private fun setupListeners() {
        binding.btnSubmit.setOnClickListener {
            if (!binding.cbAcknowledge.isChecked) {
                Toast.makeText(this, "Please confirm details first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateWorkflowAndGoCertificate()
        }
    }

    private fun updateWorkflowAndGoCertificate() {
        val id = uid ?: return

        val updates = mapOf(
            "detailsSaved" to true,
            "ackPending" to false,
            "ackCompleted" to true,
            "ackSignedAt" to System.currentTimeMillis()
        )

        FirebaseRepo.rtdb.child("details").child(id).child("workflow")
            .updateChildren(updates)
            .addOnSuccessListener { goCertificate() }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update workflow", Toast.LENGTH_SHORT).show()
            }
    }

    private fun goCertificate() {
        val u = user ?: return

        val i = Intent(this, CertificateActivity::class.java)
        i.putExtra("user", u)
        i.putExtra("hasArrears", hasArrears)
        i.putExtra("arrearsCount", arrearsCount)
        i.putExtra("collegeName", collegeName)
        i.putExtra("signature_uri", signatureUri)
        i.putExtra("signed_on", today)
        i.putExtra(MainActivity.EXTRA_STUDENT_ID, uid)
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivity(i)
        finish()
    }
}