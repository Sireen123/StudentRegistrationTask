package com.example.studentregistration

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.studentregistration.data.AppDatabase
import com.example.studentregistration.data.LoginViewModel
import com.example.studentregistration.data.LoginViewModelFactory
import com.example.studentregistration.data.UserRepository
import com.example.studentregistration.databinding.ActivityLoginBinding
import com.google.i18n.phonenumbers.PhoneNumberUtil

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    private var lastAppliedMax: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dao = AppDatabase.getDatabase(this).userDao()
        val repo = UserRepository(dao)
        val factory = LoginViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        // Restore last email
        val prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        binding.etEmail.setText(prefs.getString("last_email", ""))

        // --- Country Picker Setup ---
        binding.countryCodePicker.registerCarrierNumberEditText(binding.etPhone)

        applyPhoneMaxLengthForCountry()

        binding.countryCodePicker.setOnCountryChangeListener {
            applyPhoneMaxLengthForCountry()
            updatePhoneCounter()
        }

        // CCP validity indicator
        binding.countryCodePicker.setPhoneNumberValidityChangeListener { isValid ->
            binding.etPhone.error =
                if (!isValid && binding.etPhone.text!!.isNotEmpty())
                    "Invalid number for selected country"
                else null
        }

        // Update digit counter
        binding.etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePhoneCounter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Login
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
            if (!emailRegex.matches(email)) {
                binding.etEmail.error = "Enter valid email"
                return@setOnClickListener
            }

            if (!binding.countryCodePicker.isValidFullNumber) {
                binding.etPhone.error = "Invalid phone number"
                return@setOnClickListener
            }

            val finalPass = if (pass.isEmpty()) "1234" else pass

            prefs.edit().putString("last_email", email).apply()

            viewModel.login(email.lowercase(), finalPass)
        }

        // Login result
        viewModel.loginResult.observe(this) { user ->
            if (user != null) {
                val next =
                    if (user.role == "management") DetailsActivity::class.java
                    else StudentTaskActivity::class.java

                startActivity(Intent(this, next).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            } else {
                Toast.makeText(this, "Invalid Email or Password", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.tvFeesList.setOnClickListener {
            startActivity(Intent(this, FeesListActivity::class.java))
        }
    }

    // --- Country-specific max digits ---
    private fun applyPhoneMaxLengthForCountry() {
        val phoneUtil = PhoneNumberUtil.getInstance()
        val region = binding.countryCodePicker.selectedCountryNameCode

        val maxDigits = try {
            val sample = phoneUtil.getExampleNumber(region)
            phoneUtil.getNationalSignificantNumber(sample).length
        } catch (e: Exception) {
            10
        }

        lastAppliedMax = maxDigits

        binding.etPhone.filters = arrayOf(
            DigitMaxLengthFilter(maxDigits)
        )
    }

    private fun updatePhoneCounter() {
        val digits = binding.etPhone.text?.count { it.isDigit() } ?: 0
        binding.tvPhoneCounter.text = "$digits / $lastAppliedMax"
    }
}