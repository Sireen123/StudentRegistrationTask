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
import androidx.lifecycle.lifecycleScope
import com.example.studentregistration.data.AppDatabase
import com.example.studentregistration.data.User
import com.example.studentregistration.databinding.ActivityDetailsBinding
import com.github.gcacace.signaturepad.views.SignaturePad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ✅ Firebase RTDB
import com.example.studentregistration.data.FirebaseRepo

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private lateinit var session: SessionPrefs
    private var profilePhotoUri: Uri? = null
    private var currentCollegeName: String = ""   // ✅ holds the chosen college for UI & next screens

    private val dateFormatter by lazy { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionPrefs(this)

        setCurrentDate()

        binding.includeBack.btnBack.setOnClickListener { finish() }
        binding.includeBack.tvScreenTitle.text = "Student Details"

        // ✅ Source the college name from: Intent → Session (in that order)
        val intentCollege = intent.getStringExtra("collegeName")
        currentCollegeName = when {
            !intentCollege.isNullOrBlank() -> intentCollege
            !session.collegeName.isNullOrBlank() -> session.collegeName!!
            else -> ""
        }
        // show immediately (will be updated again after RTDB/local load if needed)
        binding.tvCollege.text = "College: ${currentCollegeName.ifBlank { "N/A" }}"

        val emailFromRegister = intent.getStringExtra("email")
        if (emailFromRegister != null) session.currentUserEmail = emailFromRegister

        val hasArrears  = intent.getBooleanExtra("hasArrears", false)
        val arrearsCount = intent.getStringExtra("arrearsCount") ?: "0"
        val selectedSem  = intent.getStringExtra("selectedSemester")

        setLoading(true)

        // ✅ Try cloud (RTDB) first, then fallback to local Room
        val uid = FirebaseRepo.auth.currentUser?.uid
        if (uid != null) {
            loadUserDetailsFromFirebase(
                uid = uid,
                selectedSem = selectedSem,
                hasArrears = hasArrears,
                arrearsCount = arrearsCount
            )
        } else {
            session.currentUserEmail?.let {
                loadUserDetailsLocal(
                    email = it,
                    selectedSem = selectedSem,
                    hasArrears = hasArrears,
                    arrearsCount = arrearsCount
                )
            } ?: run { setLoading(false) }
        }

        setupCourseClickListeners()
        // NOTE: setupSignature now uses the up-to-date currentCollegeName each time you tap Submit
        setupSignature(hasArrears, arrearsCount)
    }

    override fun onResume() {
        super.onResume()
        setCurrentDate()
    }

    // ===================== RTDB LOAD (no toast on failure) =====================
    private fun loadUserDetailsFromFirebase(
        uid: String,
        selectedSem: String?,
        hasArrears: Boolean,
        arrearsCount: String
    ) {
        FirebaseRepo.rtdb.child("users").child(uid).get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    val name        = snap.child("name").getValue(String::class.java) ?: ""
                    val registerNo  = snap.child("registerNo").getValue(String::class.java) ?: ""
                    val rollNo      = snap.child("rollNo").getValue(String::class.java) ?: ""
                    val address     = snap.child("address").getValue(String::class.java) ?: ""
                    val phone       = snap.child("phone").getValue(String::class.java) ?: ""
                    val email       = snap.child("email").getValue(String::class.java) ?: ""
                    val dob         = snap.child("dob").getValue(String::class.java) ?: ""
                    val gender      = snap.child("gender").getValue(String::class.java) ?: ""
                    val parentName  = snap.child("parentName").getValue(String::class.java) ?: ""
                    val department  = snap.child("department").getValue(String::class.java) ?: ""
                    val semester    = snap.child("semester").getValue(String::class.java) ?: ""
                    val role        = snap.child("role").getValue(String::class.java) ?: "student"
                    val feesPaid    = snap.child("feesPaid").getValue(String::class.java) ?: "0"
                    val profilePhoto= snap.child("profilePhoto").getValue(String::class.java).orEmpty()

                    // ✅ Fetch saved collegeName from RTDB (preferred)
                    val collegeSaved = snap.child("collegeName").getValue(String::class.java)
                    if (!collegeSaved.isNullOrBlank()) {
                        currentCollegeName = collegeSaved
                    }
                    binding.tvCollege.text = "College: ${currentCollegeName.ifBlank { "N/A" }}"

                    // Bind to UI
                    binding.tvName.text = "Name: $name"
                    binding.tvReg.text = "Register No: $registerNo"
                    binding.tvRoll.text = "Roll No: $rollNo"
                    binding.tvAddress.text = "Address: $address"
                    binding.tvPhone.text = "Phone: $phone"
                    binding.tvEmail.text = "Email: $email"
                    binding.tvDob.text = "Date of Birth: $dob"
                    binding.tvGender.text = "Gender: $gender"
                    binding.tvParent.text = "Parent/Guardian: $parentName"
                    binding.tvDept.text = "Department: $department"
                    binding.tvSem.text = "Semester: ${selectedSem ?: semester}"
                    binding.tvSignedByLabel.text = "Signed by: $name"

                    if (profilePhoto.isNotBlank()) {
                        profilePhotoUri = Uri.parse(profilePhoto)
                        binding.imgProfile.setImageURI(profilePhotoUri)
                    }

                    // Reuse existing fees UI using temp User
                    updateFeesUI(
                        User(
                            name = name,
                            registerNo = registerNo,
                            rollNo = rollNo,
                            address = address,
                            phone = phone,
                            email = email,
                            password = "",
                            dob = dob,
                            gender = gender,
                            parentName = parentName,
                            department = department,
                            semester = semester,
                            role = role,
                            feesPaid = feesPaid,
                            profilePhoto = profilePhoto.ifBlank { null }
                        )
                    )
                    setLoading(false)
                } else {
                    // Fallback to local silently
                    val emailLocal = session.currentUserEmail
                    if (emailLocal != null) {
                        loadUserDetailsLocal(emailLocal, selectedSem, hasArrears, arrearsCount)
                    } else {
                        setLoading(false)
                    }
                }
            }
            .addOnFailureListener {
                // No toast; fallback silently
                val emailLocal = session.currentUserEmail
                if (emailLocal != null) {
                    loadUserDetailsLocal(emailLocal, selectedSem, hasArrears, arrearsCount)
                } else {
                    setLoading(false)
                }
            }
    }

    // ===================== LOCAL ROOM LOAD (fallback) =====================
    private fun loadUserDetailsLocal(
        email: String,
        selectedSem: String?,
        hasArrears: Boolean,
        arrearsCount: String
    ) {
        val dao = AppDatabase.getDatabase(this).userDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val user = dao.getUserByEmail(email)
            withContext(Dispatchers.Main) {
                if (user != null) {
                    binding.tvName.text = "Name: ${user.name}"
                    binding.tvReg.text = "Register No: ${user.registerNo}"
                    binding.tvRoll.text = "Roll No: ${user.rollNo}"
                    binding.tvAddress.text = "Address: ${user.address}"
                    binding.tvPhone.text = "Phone: ${user.phone}"
                    binding.tvEmail.text = "Email: ${user.email}"
                    binding.tvDob.text = "Date of Birth: ${user.dob}"
                    binding.tvGender.text = "Gender: ${user.gender}"
                    binding.tvParent.text = "Parent/Guardian: ${user.parentName}"
                    binding.tvDept.text = "Department: ${user.department}"
                    binding.tvSem.text = "Semester: ${selectedSem ?: user.semester}"
                    binding.tvSignedByLabel.text = "Signed by: ${user.name}"

                    // ✅ We keep currentCollegeName as set earlier (Intent/Session)
                    binding.tvCollege.text = "College: ${currentCollegeName.ifBlank { "N/A" }}"

                    if (!user.profilePhoto.isNullOrBlank()) {
                        profilePhotoUri = Uri.parse(user.profilePhoto)
                        binding.imgProfile.setImageURI(profilePhotoUri)
                    }

                    updateFeesUI(user)
                }
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSubmit.isEnabled = !isLoading
        binding.signPad.isEnabled = !isLoading
    }

    private fun updateFeesUI(user: User) {
        val feeMap = mapOf(
            "Computer Science" to 75000,
            "Information Technology" to 75000,
            "Electronics &amp; Communication" to 70000,
            "Electrical &amp; Electronics" to 72000,
            "Mechanical" to 72000,
            "Civil" to 68000,
            "AI &amp; Data Science" to 75000,
            "Cyber Security" to 75000,
            "Bio Technology" to 80000
        )

        val total = feeMap[user.department] ?: 0
        val paid = user.feesPaid.toIntOrNull() ?: 0
        val percent = if (total == 0) 0 else (paid.toFloat() / total * 100).toInt()

        binding.tvPaidAmount.text = "₹$paid"
        binding.tvTotalAmount.text = "₹$total"
        binding.tvBalanceAmount.text = "Balance: ₹${total - paid}"

        ObjectAnimator.ofInt(binding.feesProgress, "progress", percent).apply {
            duration = 1200
            interpolator = DecelerateInterpolator()
            start()
        }

        buildDots(percent)
    }

    private fun buildDots(percent: Int) {
        binding.dotsContainer.removeAllViews()

        val dotCount = 10
        val filled = percent / 10

        for (i in 1..dotCount) {
            val dot = View(this)
            dot.layoutParams = LinearLayout.LayoutParams(18, 18).apply {
                setMargins(6, 0, 6, 0)
            }
            dot.setBackgroundResource(
                if (i <= filled) R.drawable.progress_dot_paid
                else R.drawable.progress_dot_empty
            )
            binding.dotsContainer.addView(dot)
        }
    }

    // ====== COURSE CLICK LISTENERS ======
    private fun setupCourseClickListeners() {

        fun setClick(view: View, courseName: String, fee: String) {
            view.setOnClickListener {
                val intent = Intent(this, CourseDetailsActivity::class.java)
                intent.putExtra("course_name", courseName)
                intent.putExtra("course_fee", fee)
                startActivity(intent)
            }
        }

        setClick(binding.tvCourseCSE, "BE CSE", "75000")
        setClick(binding.tvCourseECE, "BE ECE", "70000")
        setClick(binding.tvCourseCIVIL, "BE CIVIL", "68000")
        setClick(binding.tvCourseMECH, "BE MECH", "72000")
        setClick(binding.tvCourseBTECH, "BTECH", "80000")
        setClick(binding.tvCourseBARCH, "BARCH", "85000")
        setClick(binding.tvCourseBCA, "BCA", "65000")
    }

    private fun setupSignature(hasArrears: Boolean, arrearCount: String) {

        binding.signPad.setOnSignedListener(object : SignaturePad.OnSignedListener {
            override fun onStartSigning() {}
            override fun onSigned() { binding.btnSubmit.isEnabled = true }
            override fun onClear() { binding.btnSubmit.isEnabled = false }
        })

        binding.btnClearSign.setOnClickListener { binding.signPad.clear() }

        binding.btnSubmit.setOnClickListener {

            if (binding.signPad.isEmpty) {
                Toast.makeText(this, "Please add signature", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val file = File(cacheDir, "sig_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use {
                binding.signPad.signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            val sigUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )

            startActivity(Intent(this, AcknowledgementActivity::class.java).apply {
                putExtra("name", binding.tvName.text.toString())
                putExtra("reg", binding.tvReg.text.toString())
                putExtra("roll", binding.tvRoll.text.toString())
                putExtra("dept", binding.tvDept.text.toString().replace("Department: ", ""))
                putExtra("sem", binding.tvSem.text.toString().replace("Semester: ", ""))
                putExtra("address", binding.tvAddress.text.toString())
                putExtra("phone", binding.tvPhone.text.toString())
                putExtra("email", binding.tvEmail.text.toString())
                putExtra("parent", binding.tvParent.text.toString())
                putExtra("signature_uri", sigUri)

                putExtra("hasArrears", hasArrears)
                putExtra("arrearsCount", arrearCount)

                // ✅ always pass the latest college name forward
                putExtra("collegeName", currentCollegeName)

                putExtra("profile_photo_uri", profilePhotoUri?.toString())
            })
        }
    }

    private fun setCurrentDate() {
        val today = dateFormatter.format(Date())
        binding.tvSignDate.text = "Date: $today"
    }
}
