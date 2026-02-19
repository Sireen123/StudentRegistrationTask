package com.example.studentregistration

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studentregistration.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val calendar: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etDob.setOnClickListener { showDobPicker() }


        setupSpinners()


        binding.btnRegister.setOnClickListener {
            val name    = binding.etName.text.toString().trim()
            val reg     = binding.etRegister.text.toString().trim()
            val roll    = binding.etRoll.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            val phone   = binding.etPhone.text.toString().trim()
            val email   = binding.etEmail.text.toString().trim()
            val dob     = binding.etDob.text.toString().trim()
            val parent  = binding.etParentName.text.toString().trim()


            val requiredFields: List<Pair<EditText, String>> = listOf(
                binding.etName       to "Name",
                binding.etRegister   to "Register No",
                binding.etRoll       to "Roll No",
                binding.etAddress    to "Address",
                binding.etPhone      to "Phone",
                binding.etEmail      to "Email",
                binding.etDob        to "Date of Birth",
                binding.etParentName to "Parent/Guardian Name"
            )

            for ((et, label) in requiredFields) {
                if (et.text.toString().trim().isEmpty()) {
                    et.error = "$label is required"
                    et.requestFocus()
                    return@setOnClickListener
                }
            }


            if (binding.rgGender.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Please select Gender", Toast.LENGTH_SHORT).show()
                binding.rgGender.requestFocus()
                return@setOnClickListener
            }
            val gender = when (binding.rgGender.checkedRadioButtonId) {
                binding.rbMale.id -> "Male"
                binding.rbFemale.id -> "Female"
                binding.rbOther.id -> "Other"
                else -> ""
            }


            if (binding.spDepartment.selectedItemPosition == 0) {
                Toast.makeText(this, "Please select Department", Toast.LENGTH_SHORT).show()
                binding.spDepartment.requestFocus()
                return@setOnClickListener
            }
            val department = binding.spDepartment.selectedItem.toString()


            if (binding.spSemester.selectedItemPosition == 0) {
                Toast.makeText(this, "Please select Semester", Toast.LENGTH_SHORT).show()
                binding.spSemester.requestFocus()
                return@setOnClickListener
            }
            val semester = binding.spSemester.selectedItem.toString()


            val intent = Intent(this, DetailsActivity::class.java).apply {
                putExtras(Bundle().apply {
                    putString("name", name)
                    putString("reg", reg)
                    putString("roll", roll)
                    putString("address", address)
                    putString("phone", phone)
                    putString("email", email)
                    putString("dob", dob)
                    putString("gender", gender)
                    putString("parentName", parent)
                    putString("department", department)
                    putString("semester", semester)
                })
            }
            startActivity(intent)
        }
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

        val deptAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            departments
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spDepartment.adapter = deptAdapter

        val semAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            semesters
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spSemester.adapter = semAdapter
    }

    private fun showDobPicker() {
        preselectExistingDobIfAny()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val picker = DatePickerDialog(
            this,
            { _, y, m, d ->
                // Update calendar to chosen date
                calendar.set(Calendar.YEAR, y)
                calendar.set(Calendar.MONTH, m)
                calendar.set(Calendar.DAY_OF_MONTH, d)
                // Set formatted date
                binding.etDob.setText(formatDate(calendar))
            },
            year, month, day
        )


        picker.datePicker.maxDate = System.currentTimeMillis()
        picker.show()
    }

    private fun formatDate(cal: Calendar): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return sdf.format(cal.time)
    }

    private fun preselectExistingDobIfAny() {
        val current = binding.etDob.text?.toString()?.trim().orEmpty()
        if (current.isEmpty()) return

        try {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val date = sdf.parse(current)
            if (date != null) {
                calendar.time = date
            }
        } catch (_: Exception) {

        }
    }
}