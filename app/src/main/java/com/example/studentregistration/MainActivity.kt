package com.example.studentregistration

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        session = SessionPrefs(this)
        repo = UserRepository(AppDatabase.getDatabase(this).userDao())

        setupSpinners()
        setupDobPicker()

        // ----------------------------------------------------------
        // PHONE NUMBER SETTINGS (Country max digits; spaces ignored)
        // ----------------------------------------------------------

        // Attach EditText to CCP (do NOT override keyListener)
        binding.countryCodePicker.registerCarrierNumberEditText(binding.etPhone)

        // Apply digit-aware max length now and when country changes
        applyPhoneMaxLengthForCountry()
        binding.countryCodePicker.setOnCountryChangeListener {
            applyPhoneMaxLengthForCountry()
        }

        // Live CCP validation (will consider only valid numbers)
        binding.countryCodePicker.setPhoneNumberValidityChangeListener { isValid ->
            binding.etPhone.error = if (!isValid) "Invalid number" else null
        }

        // ----------------------------------------------------------
        // FEES SWITCH
        // ----------------------------------------------------------
        binding.swPaid.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) showFeesPopup() else feesPaidAmount = "0"
        }

        // ----------------------------------------------------------
        // REGISTER BUTTON
        // ----------------------------------------------------------
        binding.btnRegister.setOnClickListener {

            val name = binding.etName.text.toString().trim()
            val reg = binding.etRegister.text.toString().trim()
            val roll = binding.etRoll.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val dob = binding.etDob.text.toString().trim()
            val parent = binding.etParentName.text.toString().trim()

            val required = listOf(
                binding.etName, binding.etRegister, binding.etRoll,
                binding.etAddress, binding.etPhone, binding.etEmail,
                binding.etDob, binding.etParentName
            )

            for (f in required) {
                if (f.text.toString().trim().isEmpty()) {
                    f.error = "Required"
                    return@setOnClickListener
                }
            }

            // Email validation
            val emailRegex =
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
            if (!emailRegex.matches(email)) {
                binding.etEmail.error = "Enter valid email"
                return@setOnClickListener
            }

            // Phone validation via CCP (only valid national numbers pass)
            if (!binding.countryCodePicker.isValidFullNumber) {
                binding.etPhone.error = "Invalid phone number"
                return@setOnClickListener
            }

            if (binding.swPaid.isChecked && feesPaidAmount == "0") {
                Toast.makeText(this, "Enter fees paid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val gender = when (binding.rgGender.checkedRadioButtonId) {
                binding.rbMale.id -> "Male"
                binding.rbFemale.id -> "Female"
                else -> "Other"
            }

            val department = binding.spDepartment.selectedItem.toString()
            val semester = binding.spSemester.selectedItem.toString()

            val role = if (email == "management@gmail.com") "management" else "student"

            val user = User(
                name = name,
                registerNo = reg,
                rollNo = roll,
                address = address,
                phone = binding.countryCodePicker.fullNumberWithPlus,
                email = email.lowercase(),
                password = "1234",
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
                        startActivity(
                            Intent(this@MainActivity, DetailsActivity::class.java)
                        )
                        finish()
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "User already exists",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------
    // LIMIT PHONE NUMBER BY COUNTRY (Spaces ignored in count)
    // ----------------------------------------------------------
    private fun applyPhoneMaxLengthForCountry() {
        try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val region = binding.countryCodePicker.selectedCountryNameCode

            val example = phoneUtil.getExampleNumber(region)
            val national = phoneUtil.getNationalSignificantNumber(example)

            val maxDigits = national.length  // e.g., IN = 10

            // IMPORTANT: Use the digit-aware filter (spaces ignored)
            binding.etPhone.filters = arrayOf(
                DigitMaxLengthFilter(maxDigits)
            )

        } catch (e: Exception) {
            // Fallback max digit cap
            binding.etPhone.filters = arrayOf(
                DigitMaxLengthFilter(12)
            )
        }
    }

    // ----------------------------------------------------------
    // FEES POPUP
    // ----------------------------------------------------------
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
                if (paid.isEmpty()) {
                    binding.swPaid.isChecked = false
                    return@setPositiveButton
                }
                feesPaidAmount = paid
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
            "I","II","III","IV","V","VI","VII","VIII"
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
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, y, m, d ->
                calendar.set(y, m, d)
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                binding.etDob.setText(sdf.format(calendar.time))
            },
            year, month, day
        ).show()
    }
}
