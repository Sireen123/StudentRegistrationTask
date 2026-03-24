package com.example.studentregistration

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.studentregistration.data.FirebaseRepo
import com.example.studentregistration.data.User
import com.example.studentregistration.databinding.ActivityDetailsBinding
import com.github.gcacace.signaturepad.views.SignaturePad
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private var uid: String? = null
    private var user: User? = null
    private var hasArrears = false
    private var arrearsCount = 0
    private var collegeName = ""
    private var signatureUri: Uri? = null

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.includeBack.tvScreenTitle.text = "My Details"
        binding.includeBack.btnBack.setOnClickListener { finish() }

        user = intent.getParcelableExtra("user")
        hasArrears = intent.getBooleanExtra("hasArrears", false)
        arrearsCount = intent.getIntExtra("arrearsCount", 0)
        collegeName = intent.getStringExtra("collegeName") ?: ""

        uid = intent.getStringExtra(MainActivity.EXTRA_STUDENT_ID)
            ?: FirebaseRepo.auth.currentUser?.uid

        if (uid == null) {
            toast("Session lost.")
            finish()
            return
        }

        binding.tvSignDate.text = "Date: ${dateFormat.format(Date())}"

        if (user == null) loadUserFromFirebase(uid!!)
        else bindUI()

        setupSignature()
    }

    // ✅✅ FIXED FAST FIREBASE LOADING
    private fun loadUserFromFirebase(uid: String) {

        FirebaseRepo.rtdb.child("users").child(uid).get()
            .addOnSuccessListener { snap ->

                if (!snap.exists()) {
                    toast("User not found.")
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

                // ✅ Optional workflow
                FirebaseRepo.rtdb.child("details").child(uid).child("workflow").get()
                    .addOnSuccessListener { wf ->
                        hasArrears = wf.child("certArrear").value as? Boolean ?: false
                        arrearsCount = (snap.child("arrearsCount").value as? Long)?.toInt() ?: 0
                        collegeName = snap.child("collegeName").value as? String ?: ""
                        bindUI()
                    }
                    .addOnFailureListener { bindUI() }
            }
            .addOnFailureListener {
                toast("Failed to load user.")
            }
    }

    private fun bindUI() {
        val u = user ?: return

        if (!u.profilePhoto.isNullOrEmpty()) {
            try { binding.imgProfile.setImageURI(Uri.parse(u.profilePhoto!!)) }
            catch (_: Exception) {}
        }

        binding.tvName.text = "Name: ${u.name}"
        binding.tvReg.text = "Register No: ${u.registerNo}"
        binding.tvRoll.text = "Roll No: ${u.rollNo}"
        binding.tvAddress.text = "Address: ${u.address}"
        binding.tvPhone.text = "Phone: ${u.phone}"
        binding.tvEmail.text = "Email: ${u.email}"
        binding.tvDob.text = "Date of Birth: ${u.dob}"
        binding.tvGender.text = "Gender: ${u.gender}"
        binding.tvParent.text = "Parent/Guardian: ${u.parentName}"
        binding.tvDept.text = "Department: ${u.department}"
        binding.tvSem.text = "Semester: ${u.semester}"
        binding.tvCollege.text = "College: $collegeName"

        updateFeesUI(u)
    }

    private fun updateFeesUI(u: User) {
        val total = when (u.department) {
            "Computer Science" -> 75000
            "Information Technology" -> 75000
            "Electronics & Communication" -> 70000
            "Electrical & Electronics" -> 72000
            "Mechanical" -> 72000
            "Civil" -> 68000
            "AI & Data Science" -> 75000
            "Cyber Security" -> 75000
            "Bio Technology" -> 80000
            else -> 0
        }

        val paid = u.feesPaid.toIntOrNull() ?: 0
        val percent = if (total > 0) paid * 100 / total else 0

        binding.tvPaidAmount.text = "₹$paid"
        binding.tvTotalAmount.text = "₹$total"
        binding.tvBalanceAmount.text = "Balance: ₹${total - paid}"

        ObjectAnimator.ofInt(binding.feesProgress, "progress", percent).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
            start()
        }

        buildDots(percent)
    }

    private fun buildDots(percent: Int) {
        binding.dotsContainer.removeAllViews()
        val filled = percent / 10

        repeat(10) { i ->
            val dot = View(this)
            dot.layoutParams = LinearLayout.LayoutParams(18, 18).apply {
                setMargins(5, 0, 5, 0)
            }
            dot.setBackgroundResource(
                if (i < filled) R.drawable.progress_dot_paid
                else R.drawable.progress_dot_empty
            )
            binding.dotsContainer.addView(dot)
        }
    }

    private fun setupSignature() {

        binding.signPad.setOnSignedListener(object : SignaturePad.OnSignedListener {
            override fun onStartSigning() {}
            override fun onSigned() { binding.btnSubmit.isEnabled = true }
            override fun onClear() { binding.btnSubmit.isEnabled = false }
        })

        binding.btnClearSign.setOnClickListener { binding.signPad.clear() }

        binding.btnSubmit.setOnClickListener {
            if (binding.signPad.isEmpty) {
                toast("Please sign first")
                return@setOnClickListener
            }

            val temp = File(cacheDir, "sig_${System.currentTimeMillis()}.png")
            FileOutputStream(temp).use {
                binding.signPad.signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            signatureUri =
                FileProvider.getUriForFile(this, "$packageName.provider", temp)

            goAcknowledgement()
        }
    }

    private fun goAcknowledgement() {
        val u = user ?: return
        startActivity(Intent(this, AcknowledgementActivity::class.java).apply {
            putExtra("user", u)
            putExtra("hasArrears", hasArrears)
            putExtra("arrearsCount", arrearsCount)
            putExtra("collegeName", collegeName)
            putExtra("signature_uri", signatureUri)
            putExtra(MainActivity.EXTRA_STUDENT_ID, uid)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
        finish()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}