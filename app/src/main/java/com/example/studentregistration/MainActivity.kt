package com.example.studentregistration

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.studentregistration.data.AppDatabase
import com.example.studentregistration.data.User
import com.example.studentregistration.data.UserRepository
import com.example.studentregistration.databinding.ActivityMainBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionPrefs(this)
        repo = UserRepository(AppDatabase.getDatabase(this).userDao())

        setupSpinners()
        setupDobPicker()


        binding.btnRegister.setOnClickListener {

            val selectedId = SessionStudentPrefs(this).selectedStudentId

            if (selectedId == -1) {
                Toast.makeText(this, "Please select a student from Students Fees List first", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }



            val isPaid = binding.swPaid.isChecked


            val statusPrefs = StudentStatusPrefs(this)

            statusPrefs.setPaid(selectedId, isPaid)

            val name    = binding.etName.text.toString().trim()
            val reg     = binding.etRegister.text.toString().trim()
            val roll    = binding.etRoll.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            val phone   = binding.etPhone.text.toString().trim()
            val email   = binding.etEmail.text.toString().trim().lowercase()
            val dob     = binding.etDob.text.toString().trim()
            val parent  = binding.etParentName.text.toString().trim()


            val requiredFields: List<Pair<EditText, String>> = listOf(
                binding.etName to "Name",
                binding.etRegister to "Register No",
                binding.etRoll to "Roll No",
                binding.etAddress to "Address",
                binding.etPhone to "Phone",
                binding.etEmail to "Email",
                binding.etDob to "Date of Birth",
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
                return@setOnClickListener
            }
            val gender = when (binding.rgGender.checkedRadioButtonId) {
                binding.rbMale.id -> "Male"
                binding.rbFemale.id -> "Female"
                else -> "Other"
            }


            val department = binding.spDepartment.selectedItem.toString()
            if (department.startsWith("--")) {
                Toast.makeText(this, "Select Department", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val semester = binding.spSemester.selectedItem.toString()
            if (semester.startsWith("--")) {
                Toast.makeText(this, "Select Semester", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val user = User(
                name = name,
                registerNo = reg,
                rollNo = roll,
                address = address,
                phone = phone,
                email = email,
                password = "1234",
                dob = dob,
                gender = gender,
                parentName = parent,
                department = department,
                semester = semester
            )


            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    repo.saveFullUser(user)
                    session.currentUserEmail = email
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Registration Successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@MainActivity, DetailsActivity::class.java))
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Registration failed (maybe already exists).",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
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

        ArrayAdapter(this, android.R.layout.simple_spinner_item, departments).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spDepartment.adapter = adapter
        }
        ArrayAdapter(this, android.R.layout.simple_spinner_item, semesters).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spSemester.adapter = adapter
        }
    }

    private fun setupDobPicker() {
        binding.tilDob.setStartIconOnClickListener { showDobPicker() }
        binding.etDob.setOnClickListener { showDobPicker() }
    }

    private fun showDobPicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(Calendar.YEAR, selectedYear)
                calendar.set(Calendar.MONTH, selectedMonth)
                calendar.set(Calendar.DAY_OF_MONTH, selectedDay)

                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                binding.etDob.setText(sdf.format(calendar.time))
            },
            year, month, day
        )
        datePicker.datePicker.maxDate = System.currentTimeMillis() // past dates only
        datePicker.show()
    }
}