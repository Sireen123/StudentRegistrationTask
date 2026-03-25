package com.example.studentregistration

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.studentregistration.data.*
import com.example.studentregistration.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STUDENT_ID = "EXTRA_STUDENT_ID"
        const val EXTRA_NAV_TARGET = "NAV_TARGET"
        const val EXTRA_FORCE_DASHBOARD = "FORCE_DASHBOARD"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var session: SessionPrefs
    private lateinit var userRepo: UserRepository
    private val calendar = Calendar.getInstance()

    private var feesPaidAmount: String = "0"
    private var selectedImageUri: Uri? = null

    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var collegeName: String

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { srcUri ->
            if (srcUri != null) {
                try {
                    selectedImageUri = copyToAppCache(srcUri)
                    binding.imgProfile.setImageURI(selectedImageUri)
                } catch (_: Exception) {
                    selectedImageUri = null
                    toast("Unable to load image")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionPrefs(this)
        userRepo = UserRepository(AppDatabase.getDatabase(this).userDao())

        // ✅ FIX: Back button works now
        binding.includeBack.btnBack.setOnClickListener {
            startActivity(Intent(this, StartActivity::class.java))
            finish()
        }

        binding.btnUploadPhoto.setOnClickListener { pickImage.launch(arrayOf("image/*")) }

        // ✅ Student name → letters only
        binding.etName.filters = arrayOf(InputFilter { src, _, _, _, _, _ ->
            if (src.matches(Regex("[a-zA-Z ]+"))) src else ""
        })

        // ✅ Parent name → letters only
        binding.etParentName.filters = arrayOf(InputFilter { src, _, _, _, _, _ ->
            if (src.matches(Regex("[a-zA-Z ]+"))) src else ""
        })

        // ✅ Roll No → letters + numbers
        binding.etRoll.filters = arrayOf(InputFilter { src, _, _, _, _, _ ->
            if (src.matches(Regex("[a-zA-Z0-9]+"))) src else ""
        })

        // ✅ College edit text
        binding.etCollege.addTextChangedListener {
            collegeName = it.toString().trim()
        }

        setupSpinners()
        setupDobPicker()
        setupArrearUI()

        binding.countryCodePicker.registerCarrierNumberEditText(binding.etPhone)
        applyPhoneMaxLengthForCountry()
        binding.countryCodePicker.setOnCountryChangeListener { applyPhoneMaxLengthForCountry() }

        binding.swPaid.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) showFeesPopup() else feesPaidAmount = "0"
        }

        binding.btnRegister.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener
            if (!isOnline()) {
                toast("No internet connection")
                return@setOnClickListener
            }

            val original = binding.btnRegister.text
            binding.btnRegister.isEnabled = false
            binding.btnRegister.text = "Please wait…"

            val user = buildUser()
            val hasArrears = binding.swHasArrears.isChecked
            val arrearsCount =
                if (hasArrears) binding.etArrearsCount.text.toString().toIntOrNull() ?: 0 else 0

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    userRepo.saveFullUser(user)
                    session.currentUserEmail = user.email
                    session.collegeName = collegeName

                    withContext(Dispatchers.Main) {
                        pushToFirebase(user.email, user.password, user, hasArrears, arrearsCount)
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.btnRegister.isEnabled = true
                        binding.btnRegister.text = original
                        toast("Error: ${e.message}")
                    }
                }
            }
        }
    }

    private fun pushToFirebase(
        email: String,
        password: String,
        user: User,
        hasArrears: Boolean,
        arrearsCount: Int
    ) {

        auth.fetchSignInMethodsForEmail(email)
            .addOnSuccessListener { methods ->

                val exists = !methods.signInMethods.isNullOrEmpty()

                val finishFlow: (String) -> Unit = { uid ->
                    upsertUser(uid, user, hasArrears, arrearsCount)
                }

                if (exists) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { finishFlow(it.user!!.uid) }
                        .addOnFailureListener { toast("Login failed") }

                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { finishFlow(it.user!!.uid) }
                        .addOnFailureListener { toast("Registration failed") }
                }
            }
    }

    private fun upsertUser(uid: String, user: User, hasArrears: Boolean, arrearsCount: Int) {

        val map = mapOf(
            "uid" to uid,
            "name" to user.name,
            "email" to user.email,
            "phone" to user.phone,
            "registerNo" to user.registerNo,
            "rollNo" to user.rollNo,
            "address" to user.address,
            "dob" to user.dob,
            "gender" to user.gender,
            "parentName" to user.parentName,
            "department" to user.department,
            "semester" to user.semester,
            "role" to user.role,
            "feesPaid" to user.feesPaid,
            "profilePhoto" to (user.profilePhoto ?: ""),
            "collegeName" to session.collegeName,
            "hasArrears" to hasArrears,
            "arrearsCount" to arrearsCount,
            "createdAt" to System.currentTimeMillis()
        )

        FirebaseRepo.rtdb.child("users").child(uid)
            .setValue(map)
            .addOnSuccessListener {
                goLoader(uid, user, hasArrears, arrearsCount)
            }
            .addOnFailureListener {
                toast("Failed: ${it.message}")
            }
    }

    private fun goLoader(uid: String, user: User, hasArrears: Boolean, arrearsCount: Int) {
        startActivity(Intent(this, LoadingActivity::class.java).apply {
            putExtra(EXTRA_NAV_TARGET, "DASHBOARD")
            putExtra(EXTRA_FORCE_DASHBOARD, true)
            putExtra(EXTRA_STUDENT_ID, uid)
            putExtra("user", user)
            putExtra("hasArrears", hasArrears)
            putExtra("arrearsCount", arrearsCount)
            putExtra("collegeName", session.collegeName ?: "")
        })
        finish()
    }

    private fun copyToAppCache(src: Uri): Uri? {
        return try {
            val dir = File(cacheDir, "images").apply { if (!exists()) mkdirs() }
            val file = File(dir, "profile_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(src)?.use { input ->
                FileOutputStream(file).use { input.copyTo(it) }
            }
            FileProvider.getUriForFile(this, "${packageName}.provider", file)
        } catch (_: Exception) {
            null
        }
    }

    private fun buildUser(): User {

        val gender = when (binding.rgGender.checkedRadioButtonId) {
            binding.rbMale.id -> "Male"
            binding.rbFemale.id -> "Female"
            else -> "Other"
        }

        return User(
            name = binding.etName.text.toString().trim(),
            registerNo = binding.etRegister.text.toString().trim(),
            rollNo = binding.etRoll.text.toString().trim(),
            address = binding.etAddress.text.toString().trim(),
            phone = binding.countryCodePicker.fullNumberWithPlus,
            email = binding.etEmail.text.toString().trim().lowercase(Locale.getDefault()),
            password = binding.etPassword.text.toString().trim(),
            dob = binding.etDob.text.toString().trim(),
            gender = gender,
            parentName = binding.etParentName.text.toString().trim(),
            department = binding.spDepartment.selectedItem.toString(),
            semester = binding.spSemester.selectedItem.toString(),
            role = if (binding.etEmail.text.toString() == "management@gmail.com") "management" else "student",
            feesPaid = feesPaidAmount,
            profilePhoto = selectedImageUri?.toString()
        )
    }

    private fun validateInputs(): Boolean {
        fun err(v: EditText, msg: String): Boolean {
            v.error = msg
            v.requestFocus()
            return false
        }

        if (binding.etName.text!!.isEmpty()) return err(binding.etName, "Required")
        if (binding.etRegister.text!!.isEmpty()) return err(binding.etRegister, "Required")
        if (binding.etRoll.text!!.isEmpty()) return err(binding.etRoll, "Required")
        if (binding.etAddress.text!!.isEmpty()) return err(binding.etAddress, "Required")
        if (binding.etPhone.text!!.isEmpty()) return err(binding.etPhone, "Required")
        if (binding.etEmail.text!!.isEmpty()) return err(binding.etEmail, "Required")
        if (binding.etPassword.text!!.length < 6) return err(binding.etPassword, "Min 6 chars")
        if (binding.etDob.text!!.isEmpty()) return err(binding.etDob, "Required")
        if (binding.etParentName.text!!.isEmpty()) return err(binding.etParentName, "Required")
        if (binding.etCollege.text!!.isEmpty()) return err(binding.etCollege, "Required")

        if (binding.rgGender.checkedRadioButtonId == -1) {
            toast("Select Gender")
            return false
        }

        if (binding.spDepartment.selectedItemPosition == 0) {
            toast("Select Department")
            return false
        }

        if (binding.spSemester.selectedItemPosition == 0) {
            toast("Select Semester")
            return false
        }

        if (binding.swHasArrears.isChecked) {
            val c = binding.etArrearsCount.text.toString().toIntOrNull()
            if (c == null || c < 1) return err(binding.etArrearsCount, "Invalid")
        }

        return true
    }

    private fun setupDobPicker() {
        binding.tilDob.setStartIconOnClickListener { showDOB() }
        binding.etDob.setOnClickListener { showDOB() }
    }

    private fun showDOB() {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH)
        val d = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, yy, mm, dd ->
            calendar.set(yy, mm, dd)
            binding.etDob.setText(SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.time))
        }, y, m, d).apply {
            datePicker.maxDate = System.currentTimeMillis()
            show()
        }
    }

    private fun showFeesPopup() {
        val dept = binding.spDepartment.selectedItem.toString()
        val total = departmentTotalFee(dept)

        if (total <= 0) {
            toast("Select valid department")
            binding.swPaid.isChecked = false
            return
        }

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Enter amount"
        }

        AlertDialog.Builder(this)
            .setTitle("Paid Amount")
            .setMessage("Total Fee for $dept: ₹$total")
            .setView(input)
            .setPositiveButton("OK") { d, _ ->
                val amt = input.text.toString().toIntOrNull()

                when {
                    amt == null -> toast("Invalid amount")
                    amt < 0 -> toast("Can't be negative")
                    amt > total -> toast("Cannot exceed ₹$total")
                    else -> feesPaidAmount = amt.toString()
                }
                d.dismiss()
            }
            .setNegativeButton("Cancel") { d, _ ->
                binding.swPaid.isChecked = false
                d.dismiss()
            }
            .show()
    }

    private fun setupSpinners() {

        val depts = arrayOf(
            "-- Select Department --",
            "Computer Science",
            "Information Technology",
            "Electronics & Communication",
            "Electrical & Electronics",
            "Mechanical",
            "Civil",
            "AI & Data Science",
            "Cyber Security",
            "Bio Technology"
        )

        val sems = arrayOf(
            "-- Select Semester --",
            "I", "II", "III", "IV", "V", "VI", "VII", "VIII"
        )

        binding.spDepartment.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, depts)

        binding.spSemester.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sems)
    }

    private fun setupArrearUI() {
        binding.llArrears.isVisible = false
        binding.tilArrearsCount.isVisible = false

        binding.spSemester.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    val show = pos > 0
                    binding.llArrears.isVisible = show
                    if (!show) {
                        binding.swHasArrears.isChecked = false
                        binding.tilArrearsCount.isVisible = false
                        binding.etArrearsCount.setText("")
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        binding.swHasArrears.setOnCheckedChangeListener { _, checked ->
            binding.tilArrearsCount.isVisible = checked
        }
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun applyPhoneMaxLengthForCountry() {
        binding.etPhone.filters = arrayOf(DigitMaxLengthFilter(10))
    }

    private fun departmentTotalFee(dept: String): Int {
        return when (dept) {
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
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
