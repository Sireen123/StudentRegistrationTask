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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AcknowledgementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAcknowledgementBinding

    private var user: User? = null
    private var hasArrears: Boolean = false
    private var arrearsCount: Int = 0
    private var collegeName: String = ""

    private var signatureUri: Uri? = null
    private var profilePhotoUri: Uri? = null
    private var studentId: String? = null

    private val storage by lazy { FirebaseStorage.getInstance() }
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAcknowledgementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ FIXED BACK BAR
        binding.includeBack.btnBack.setOnClickListener {
            finishAffinity()   // No return to Dashboard
        }
        binding.includeBack.tvScreenTitle.text = "Acknowledgement"
        binding.tvTitle.text = "ACKNOWLEDGEMENT"

        // ✅ Get Data
        user = intent.getParcelableExtra("user")
        hasArrears = intent.getBooleanExtra("hasArrears", false)
        arrearsCount = intent.getIntExtra("arrearsCount", 0)
        collegeName = intent.getStringExtra("collegeName") ?: ""

        signatureUri =
            if (Build.VERSION.SDK_INT >= 33)
                intent.getParcelableExtra("signature_uri", Uri::class.java)
            else @Suppress("DEPRECATION")
            intent.getParcelableExtra("signature_uri")

        profilePhotoUri = intent.getStringExtra("profile_photo_uri")?.let { Uri.parse(it) }

        studentId = intent.getStringExtra(MainActivity.EXTRA_STUDENT_ID)
            ?: FirebaseRepo.auth.currentUser?.uid

        if (user == null || studentId == null) {
            Toast.makeText(this, "Invalid acknowledgement data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindUI()
        setupSubmit()
    }

    private fun bindUI() {
        val u = user!!

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

    private fun setupSubmit() {
        binding.btnSubmit.setOnClickListener {

            if (!binding.cbAcknowledge.isChecked) {
                Toast.makeText(this, "Please confirm the details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val u = user!!
            val finalHasArrears = hasArrears || arrearsCount > 0
            val signedOn = dateFormat.format(Date())

            saveAckFirebase(
                u = u,
                hasArrears = finalHasArrears,
                arrears = arrearsCount,
                signedOn = signedOn,
                sigUri = signatureUri
            )

            updateWorkflow(finalHasArrears)

            goToCertificate(
                user = u,
                hasArrears = finalHasArrears,
                arrearsCount = arrearsCount,
                signedOn = signedOn
            )
        }
    }

    private fun saveAckFirebase(
        u: User,
        hasArrears: Boolean,
        arrears: Int,
        signedOn: String,
        sigUri: Uri?
    ) {
        val uid = studentId ?: return

        if (sigUri != null) {
            val fileName = "ack_${System.currentTimeMillis()}.png"
            val ref = storage.reference.child("users/$uid/ack/$fileName")

            ref.putFile(sigUri)
                .continueWithTask { ref.downloadUrl }
                .addOnSuccessListener { url ->
                    writeAckDoc(u, url.toString(), hasArrears, arrears, signedOn)
                }
                .addOnFailureListener {
                    writeAckDoc(u, "", hasArrears, arrears, signedOn)
                }
        } else {
            writeAckDoc(u, "", hasArrears, arrears, signedOn)
        }
    }

    private fun writeAckDoc(
        u: User,
        sigUrl: String,
        hasArrears: Boolean,
        arrears: Int,
        signedOn: String
    ) {
        val data = hashMapOf(
            "name" to u.name,
            "registerNo" to u.registerNo,
            "rollNo" to u.rollNo,
            "department" to u.department,
            "semester" to u.semester,
            "parentName" to u.parentName,
            "collegeName" to collegeName,
            "hasArrears" to hasArrears,
            "arrearsCount" to arrears,
            "signatureUrl" to sigUrl,
            "profilePhotoUri" to (profilePhotoUri?.toString() ?: ""),
            "signedOn" to signedOn,
            "createdAt" to FieldValue.serverTimestamp()
        )

        FirebaseRepo.db.collection("users")
            .document(studentId!!)
            .collection("acknowledgements")
            .add(data)
    }

    private fun updateWorkflow(isArrear: Boolean) {
        val uid = studentId ?: return
        val certType = if (isArrear) "ARREAR" else "FINAL"

        val workflow = mapOf(
            "detailsSaved" to true,
            "ackPending" to false,
            "ackCompleted" to true,
            "ackCompletedAt" to System.currentTimeMillis(),
            "certPending" to true,
            "certType" to certType
        )

        FirebaseRepo.rtdb.child("details").child(uid).child("workflow").updateChildren(workflow)
    }

    private fun goToCertificate(
        user: User,
        hasArrears: Boolean,
        arrearsCount: Int,
        signedOn: String
    ) {
        startActivity(Intent(this, CertificateActivity::class.java).apply {
            putExtra("user", user)
            putExtra("hasArrears", hasArrears)
            putExtra("arrearsCount", arrearsCount)
            putExtra("collegeName", collegeName)
            putExtra("signature_uri", signatureUri)
            putExtra("signed_on", signedOn)
            putExtra(MainActivity.EXTRA_STUDENT_ID, studentId)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })

        finish() // ✅ closes acknowledgement
    }
}