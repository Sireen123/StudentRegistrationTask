package com.example.studentregistration

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.studentregistration.api.DatasetNetworkModule
import com.example.studentregistration.data.AppDatabase
import com.example.studentregistration.data.FirebaseRepo
import com.example.studentregistration.data.User
import com.example.studentregistration.data.UserRepository
import com.example.studentregistration.databinding.ActivityMainBinding
import com.example.studentregistration.model.University
import com.google.firebase.auth.FirebaseAuth
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var session: SessionPrefs
    private lateinit var userRepo: UserRepository
    private val calendar = Calendar.getInstance()
    private var feesPaidAmount: String = "0"
    private var selectedImageUri: Uri? = null

    private val auth by lazy { FirebaseAuth.getInstance() }

    // College picker
    private lateinit var collegeAdapter: ArrayAdapter<String>
    private val collegeNames = mutableListOf<String>()
    private var allUniversities: List<University> = emptyList()
    private var selectedCollegeName: String = ""

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) { }
                selectedImageUri = uri
                binding.imgProfile.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        session = SessionPrefs(this)
        binding.root.findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener { finish() }

        userRepo = UserRepository(AppDatabase.getDatabase(this).userDao())

        binding.btnUploadPhoto.setOnClickListener { pickImage.launch(arrayOf("image/*")) }

        setupCollegePicker()
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

            val originalText = binding.btnRegister.text
            binding.btnRegister.isEnabled = false
            binding.btnRegister.text = "Please wait…"

            val collegeName = selectedCollegeName.trim()
            val name = binding.etName.text.toString().trim()
            val reg = binding.etRegister.text.toString().trim()
            val roll = binding.etRoll.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            val phone = binding.countryCodePicker.fullNumberWithPlus
            val email = binding.etEmail.text.toString().trim().lowercase()
            val password = binding.etPassword.text.toString().trim()
            val dob = binding.etDob.text.toString().trim()
            val parent = binding.etParentName.text.toString().trim()

            val gender = when (binding.rgGender.checkedRadioButtonId) {
                binding.rbMale.id -> "Male"
                binding.rbFemale.id -> "Female"
                else -> "Other"
            }

            val department = binding.spDepartment.selectedItem.toString()
            val semester = binding.spSemester.selectedItem.toString()
            val role = if (email == "management@gmail.com") "management" else "student"

            val hasArrears = binding.swHasArrears.isChecked
            val arrearsCount = if (hasArrears) binding.etArrearsCount.text.toString().trim() else "0"

            val user = User(
                name = name,
                registerNo = reg,
                rollNo = roll,
                address = address,
                phone = phone,
                email = email,
                password = password,
                dob = dob,
                gender = gender,
                parentName = parent,
                department = department,
                semester = semester,
                role = role,
                feesPaid = feesPaidAmount,
                profilePhoto = selectedImageUri?.toString()
            )

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    userRepo.saveFullUser(user)
                    session.currentUserEmail = email
                    session.collegeName = collegeName
                    withContext(Dispatchers.Main) {
                        pushToFirebase(email, password, user)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.btnRegister.isEnabled = true
                        binding.btnRegister.text = originalText
                        Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /** Colleges: dataset + manual merge */
    private fun setupCollegePicker() {

        collegeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            collegeNames
        )
        binding.spCollege.adapter = collegeAdapter

        lifecycleScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    DatasetNetworkModule.api.getAll(DatasetNetworkModule.DATASET_URL)
                }

                allUniversities = list
                    .filter { it.name.isNotBlank() }
                    .distinctBy { it.name.lowercase(Locale.getDefault()) }
                    .sortedBy { it.name }

                val manualColleges = listOf(
                    University("Stella Maris College", "India"),
                    University("Madras Christian College (MCC)", "India"),
                    University("Vels Institute of Science and Technology", "India"),
                    University("Vellore Institute of Technology (VIT)", "India"),
                    University("Women's Christian College", "India"),
                    University("SSN College of Engineering and Technology", "India"),
                    University("Anna University MIT Campus", "India"),
                )

                allUniversities = (allUniversities + manualColleges)
                    .distinctBy { it.name.lowercase(Locale.getDefault()) }
                    .sortedBy { it.name }

                collegeNames.clear()
                collegeNames.addAll(allUniversities.map { it.name })
                collegeAdapter.notifyDataSetChanged()

                if (collegeNames.isNotEmpty()) {
                    binding.spCollege.setSelection(0)
                    selectedCollegeName = collegeNames[0]
                }

            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        binding.spCollege.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, v: View?, pos: Int, id: Long
                ) {
                    selectedCollegeName = collegeNames.getOrNull(pos).orEmpty()
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        binding.etCollegeSearch.addTextChangedListener { text ->
            val q = text?.toString()?.trim()?.lowercase(Locale.getDefault()).orEmpty()
            val filtered = if (q.isEmpty()) {
                allUniversities
            } else {
                allUniversities.filter {
                    it.name.lowercase(Locale.getDefault()).contains(q)
                }
            }

            collegeNames.clear()
            collegeNames.addAll(filtered.map { it.name })
            collegeAdapter.notifyDataSetChanged()

            selectedCollegeName =
                if (collegeNames.isNotEmpty()) collegeNames[0] else ""
        }
    }

    /** Save to Firebase Auth + RTDB, then go loader */
    private fun pushToFirebase(email: String, password: String, user: User) {

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { res ->
                val uid = res.user!!.uid

                val userMap = mapOf(
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
                    "collegeName" to (session.collegeName ?: ""),
                    "createdAt" to System.currentTimeMillis()
                )

                FirebaseRepo.rtdb.child("users").child(uid)
                    .setValue(userMap)
                    .addOnSuccessListener {
                        // → LoadingActivity (then Dashboard deterministically)
                        val intent = Intent(this, LoadingActivity::class.java).apply {
                            putExtra("NAV_TARGET", "DASHBOARD")
                            putExtra("FORCE_DASHBOARD", true) // force direct to Dashboard
                        }
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Save failed!", Toast.LENGTH_LONG).show()
                        binding.btnRegister.isEnabled = true
                        binding.btnRegister.text = "Register"
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Auth failed: ${it.message}", Toast.LENGTH_LONG).show()
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "Register"
            }
    }

    override fun onResume() {
        super.onResume()
        binding.btnRegister.isEnabled = true
        binding.btnRegister.text = "Register"
    }

    private fun validateInputs(): Boolean {

        fun err(v: EditText, msg: String): Boolean {
            v.error = msg
            v.requestFocus()
            showKeyboard(v)
            return false
        }

        if (selectedCollegeName.isBlank()) {
            Toast.makeText(this, "Please select College", Toast.LENGTH_SHORT).show()
            binding.spCollege.performClick()
            return false
        }

        if (binding.etName.text!!.isEmpty()) return err(binding.etName, "Required")
        if (binding.etRegister.text!!.isEmpty()) return err(binding.etRegister, "Required")
        if (binding.etRoll.text!!.isEmpty()) return err(binding.etRoll, "Required")
        if (binding.etAddress.text!!.isEmpty()) return err(binding.etAddress, "Required")
        if (binding.etPhone.text!!.isEmpty()) return err(binding.etPhone, "Required")
        if (binding.etEmail.text!!.isEmpty()) return err(binding.etEmail, "Required")

        val pwd = binding.etPassword.text.toString().trim()
        if (pwd.isEmpty()) return err(binding.etPassword, "Required")
        if (pwd.length < 6) return err(binding.etPassword, "Min 6 chars")

        if (binding.etDob.text!!.isEmpty()) return err(binding.etDob, "Required")
        if (binding.etParentName.text!!.isEmpty()) return err(binding.etParentName, "Required")

        if (binding.rgGender.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Select Gender", Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.spDepartment.selectedItemPosition == 0) {
            Toast.makeText(this, "Select Department", Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.spSemester.selectedItemPosition == 0) {
            Toast.makeText(this, "Select Semester", Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.swHasArrears.isChecked) {
            val ac = binding.etArrearsCount.text.toString()
            val acNum = ac.toIntOrNull()
            if (acNum == null || acNum < 1) return err(binding.etArrearsCount, "Invalid")
        }

        return true
    }

    private fun showKeyboard(v: EditText) {
        v.post {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun applyPhoneMaxLengthForCountry() {
        val phoneUtil = PhoneNumberUtil.getInstance()
        val region = binding.countryCodePicker.selectedCountryNameCode
        val maxDigits = if (region == "IN") 10 else 10
        binding.etPhone.filters = arrayOf(DigitMaxLengthFilter(maxDigits))
    }

    private fun showFeesPopup() {
        val dept = binding.spDepartment.selectedItem.toString()
        val total = departmentTotalFee(dept)

        val view = layoutInflater.inflate(R.layout.dialog_fees, null)
        val tvDept = view.findViewById<TextView>(R.id.tvDeptName)
        val tvFee = view.findViewById<TextView>(R.id.tvTotalFee)
        val etPaid = view.findViewById<EditText>(R.id.etPaidAmount)

        tvDept.text = "Department: $dept"
        tvFee.text = "Total Fees: ₹$total"

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Fees Payment")
            .setView(view)
            .setPositiveButton("OK") { _, _ ->
                val paid = etPaid.text.toString().trim()
                feesPaidAmount = if (paid.isEmpty()) "0" else paid
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.swPaid.isChecked = false
                feesPaidAmount = "0"
            }
            .show()
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
            "-- Select Semester --", "I", "II", "III", "IV", "V", "VI", "VII", "VIII"
        )

        binding.spDepartment.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, depts).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        binding.spSemester.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, sems).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
    }

    private fun setupDobPicker() {
        binding.tilDob.setStartIconOnClickListener { showPicker() }
        binding.etDob.setOnClickListener { showPicker() }
    }

    private fun showPicker() {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH)
        val d = calendar.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(
            this,
            { _, yy, mm, dd ->
                calendar.set(yy, mm, dd)
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                binding.etDob.setText(sdf.format(calendar.time))
            },
            y, m, d
        )
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.show()
    }

    private fun setupArrearUI() {
        binding.llArrears.isVisible = false
        binding.tilArrearsCount.isVisible = false

        binding.spSemester.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, pos: Int, id: Long
                ) {
                    val show = pos > 0
                    binding.llArrears.isVisible = show
                    if (!show) {
                        binding.swHasArrears.isChecked = false
                        binding.etArrearsCount.setText("")
                        binding.tilArrearsCount.isVisible = false
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        binding.swHasArrears.setOnCheckedChangeListener { _, isChecked ->
            binding.tilArrearsCount.isVisible = isChecked
        }
    }
}
