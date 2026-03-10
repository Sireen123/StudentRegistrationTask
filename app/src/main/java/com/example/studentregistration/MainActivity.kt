package com.example.studentregistration

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.studentregistration.data.AppDatabase
import com.example.studentregistration.data.User
import com.example.studentregistration.data.UserRepository
import com.example.studentregistration.databinding.ActivityMainBinding
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
    private lateinit var repo: UserRepository
    private val calendar = Calendar.getInstance()
    private var feesPaidAmount: String = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        session = SessionPrefs(this)
        binding.root.findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener { finish() }

        repo = UserRepository(AppDatabase.getDatabase(this).userDao())

        // Prefill email if coming from LoginActivity
        val incomingEmail = intent.getStringExtra("email_from_login")?.trim()?.lowercase() ?: ""
        if (incomingEmail.isNotEmpty()) binding.etEmail.setText(incomingEmail)

        setupSpinners()
        setupDobPicker()
        setupArrearUI()

        binding.countryCodePicker.registerCarrierNumberEditText(binding.etPhone)
        applyPhoneMaxLengthForCountry()
        binding.countryCodePicker.setOnCountryChangeListener { applyPhoneMaxLengthForCountry() }

        binding.countryCodePicker.setPhoneNumberValidityChangeListener { isValid ->
            binding.etPhone.error = if (!isValid && binding.etPhone.text!!.isNotEmpty())
                "Invalid number" else null
        }

        binding.swPaid.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) showFeesPopup() else feesPaidAmount = "0"
        }

        binding.btnRegister.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener

            binding.btnRegister.isEnabled = false

            val collegeName = binding.etCollege.text.toString().trim()
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
            val arrearsCount =
                if (hasArrears) binding.etArrearsCount.text.toString().trim() else "0"

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
                feesPaid = feesPaidAmount
            )

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    repo.saveFullUser(user)
                    session.currentUserEmail = email

                    withContext(Dispatchers.Main) {

                        session.collegeName = collegeName

                        Toast.makeText(
                            this@MainActivity,
                            "Registered successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        startActivity(
                            Intent(this@MainActivity, LoadingActivity::class.java).apply {
                                putExtra("email", email)
                                putExtra("hasArrears", hasArrears)
                                putExtra("arrearsCount", arrearsCount)
                                putExtra("selectedSemester", semester)
                                putExtra("collegeName", collegeName)
                            }
                        )

                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "User already exists",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.btnRegister.isEnabled = true
                    }
                }
            }
        }
    }

    private fun validateInputs(): Boolean {

        fun err(v: EditText, msg: String): Boolean {
            v.error = msg
            v.requestFocus()
            return false
        }

        if (binding.etCollege.text!!.isEmpty()) return err(binding.etCollege, "Required")
        if (binding.etName.text!!.isEmpty()) return err(binding.etName, "Required")
        if (binding.etRegister.text!!.isEmpty()) return err(binding.etRegister, "Required")
        if (binding.etRoll.text!!.isEmpty()) return err(binding.etRoll, "Required")
        if (binding.etAddress.text!!.isEmpty()) return err(binding.etAddress, "Required")
        if (binding.etPhone.text!!.isEmpty()) return err(binding.etPhone, "Required")
        if (binding.etEmail.text!!.isEmpty()) return err(binding.etEmail, "Required")
        if (binding.etPassword.text!!.isEmpty()) return err(binding.etPassword, "Password required")
        if (binding.etDob.text!!.isEmpty()) return err(binding.etDob, "Required")
        if (binding.etParentName.text!!.isEmpty()) return err(binding.etParentName, "Required")

        val pwd = binding.etPassword.text.toString().trim()
        if (pwd.length < 4) return err(binding.etPassword, "Min 4 characters")

        val email = binding.etEmail.text.toString().trim()
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
        if (!emailRegex.matches(email)) return err(binding.etEmail, "Enter valid email")

        if (!binding.countryCodePicker.isValidFullNumber)
            return err(binding.etPhone, "Invalid phone number")

        if (binding.spDepartment.selectedItem.toString().contains("--")) {
            Toast.makeText(this, "Select Department", Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.spSemester.selectedItem.toString().contains("--")) {
            Toast.makeText(this, "Select Semester", Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.swHasArrears.isChecked &&
            binding.etArrearsCount.text!!.isEmpty()
        )
            return err(binding.etArrearsCount, "Required")

        if (binding.swPaid.isChecked && feesPaidAmount == "0") {
            Toast.makeText(this, "Enter fees amount", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun applyPhoneMaxLengthForCountry() {
        val phoneUtil = PhoneNumberUtil.getInstance()
        val region = binding.countryCodePicker.selectedCountryNameCode

        val maxDigits = try {
            val sample = phoneUtil.getExampleNumber(region)
            val nsn = phoneUtil.getNationalSignificantNumber(sample)
            nsn.length
        } catch (_: Exception) {
            10
        }

        binding.etPhone.filters = arrayOf(DigitMaxLengthFilter(maxDigits))
    }

    private fun showFeesPopup() {
        val dept = binding.spDepartment.selectedItem.toString()
        val feeMap = mapOf(
            "Computer Science" to 75000,
            "Information Technology" to 75000,
            "Electronics & Communication" to 70000,
            "Electrical & Electronics" to 72000,
            "Mechanical" to 72000,
            "Civil" to 68000,
            "AI & Data Science" to 75000,
            "Cyber Security" to 75000,
            "Bio Technology" to 80000
        )
        val total = feeMap[dept] ?: 0

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
                if (paid.isEmpty()) binding.swPaid.isChecked = false
                else feesPaidAmount = paid
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.swPaid.isChecked = false
                feesPaidAmount = "0"
            }
            .show()
    }

    private fun setupSpinners() {
        val departments = arrayOf(
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

        val semesters = arrayOf(
            "-- Select Semester --",
            "I", "II", "III", "IV", "V", "VI", "VII", "VIII"
        )

        binding.spDepartment.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, departments)
        binding.spSemester.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, semesters)
    }

    private fun setupDobPicker() {
        binding.tilDob.setStartIconOnClickListener { showPicker() }
        binding.etDob.setOnClickListener { showPicker() }
    }

    private fun showPicker() {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH)
        val d = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, yy, mm, dd ->
                calendar.set(yy, mm, dd)
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                binding.etDob.setText(sdf.format(calendar.time))
            },
            y, m, d
        ).show()
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

    override fun onSupportNavigateUp(): Boolean {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        return true
    }
}