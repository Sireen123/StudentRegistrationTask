package com.example.studentregistration

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class ReferStudentActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etDepartment: EditText
    private lateinit var etNote: EditText
    private lateinit var btnSubmit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_refer_student)

        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        etDepartment = findViewById(R.id.etDepartment)
        etNote = findViewById(R.id.etNote)
        btnSubmit = findViewById(R.id.btnSubmit)

        // ✅ Get the current student ID (referrer)
        val referrerUid = intent.getStringExtra(MainActivity.EXTRA_STUDENT_ID) ?: "unknown"

        btnSubmit.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val dept = etDepartment.text.toString().trim()
            val note = etNote.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || dept.isEmpty()) {
                Toast.makeText(this, "Please fill required fields!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Prepare data
            val referral = mapOf(
                "name" to name,
                "phone" to phone,
                "email" to email,
                "department" to dept,
                "note" to note,
                "referrerUid" to referrerUid,
                "timestamp" to System.currentTimeMillis()
            )

            // ✅ Upload to Firebase
            FirebaseDatabase.getInstance()
                .getReference("referrals")
                .push()
                .setValue(referral)
                .addOnSuccessListener {
                    Toast.makeText(this, "Referral submitted successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}