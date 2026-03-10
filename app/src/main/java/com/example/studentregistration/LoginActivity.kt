package com.example.studentregistration

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.studentregistration.data.AppDatabase
import com.example.studentregistration.data.LoginViewModel
import com.example.studentregistration.data.LoginViewModelFactory
import com.example.studentregistration.data.UserRepository
import com.example.studentregistration.databinding.ActivityLoginBinding
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    private var lastAppliedMax = 0
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // -------- VIEWMODEL SETUP --------
        val dao = AppDatabase.getDatabase(this).userDao()
        val repo = UserRepository(dao)
        val factory = LoginViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        // -------- PREFILL EMAIL --------
        val prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        binding.etEmail.setText(prefs.getString("last_email", ""))

        // -------- PHONE SETUP --------
        binding.countryCodePicker.registerCarrierNumberEditText(binding.etPhone)
        applyPhoneMaxLengthForCountry()

        binding.countryCodePicker.setOnCountryChangeListener {
            applyPhoneMaxLengthForCountry()
            updatePhoneCounter()
        }

        binding.etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePhoneCounter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // -------- LOGIN BUTTON --------
        binding.btnLogin.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            prefs.edit().putString("last_email", email).apply()
            binding.btnLogin.isEnabled = false

            lifecycleScope.launch {
                viewModel.login(email.lowercase(), password)
            }
        }

        // -------- REGISTER BUTTON --------
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim().lowercase()
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    putExtra("email_from_login", email)
                }
            )
        }

        // -------- LOGIN RESULT --------
        viewModel.loginResult.observe(this) { user ->
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@observe

            if (user != null) {
                if (hasNavigated) return@observe
                hasNavigated = true

                // ⭐ FINAL FIX ⭐
                val nextScreen =
                    if (user.role == "management") DetailsActivity::class.java
                    else LoadingActivity::class.java  // Students go through loading → details

                startActivity(Intent(this, nextScreen).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("email_from_login", user.email)
                })

                finish()

            } else {
                Toast.makeText(this, "Invalid Email or Password", Toast.LENGTH_SHORT).show()
                binding.btnLogin.isEnabled = true
            }
        }

        // FEES LIST
        binding.tvFeesList.setOnClickListener {
            startActivity(Intent(this, FeesListActivity::class.java))
        }
    }

    // -------- VALIDATION --------
    private fun validateInputs(): Boolean {

        binding.etEmail.error = null
        binding.etPhone.error = null
        binding.etPassword.error = null

        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // EMAIL
        if (email.isEmpty()) {
            binding.etEmail.error = "Email required"
            binding.etEmail.requestFocus()
            return false
        }

        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!emailRegex.matches(email)) {
            binding.etEmail.error = "Enter valid email"
            binding.etEmail.requestFocus()
            return false
        }

        // PHONE
        if (phone.isEmpty()) {
            binding.etPhone.error = "Phone required"
            binding.etPhone.requestFocus()
            return false
        }

        if (!binding.countryCodePicker.isValidFullNumber) {
            binding.etPhone.error = "Invalid phone number"
            binding.etPhone.requestFocus()
            return false
        }

        val digits = phone.count { it.isDigit() }
        if (digits < 10) {
            binding.etPhone.error = "Phone must have minimum 10 digits"
            binding.etPhone.requestFocus()
            return false
        }

        // PASSWORD
        if (password.isEmpty()) {
            binding.etPassword.error = "Password required"
            binding.etPassword.requestFocus()
            return false
        }

        return true
    }

    // -------- PHONE LENGTH --------
    private fun applyPhoneMaxLengthForCountry() {
        val phoneUtil = PhoneNumberUtil.getInstance()
        val region = binding.countryCodePicker.selectedCountryNameCode

        val maxDigits = try {
            val sample = phoneUtil.getExampleNumber(region)
            val nsn = sample?.let { phoneUtil.getNationalSignificantNumber(it) }
            nsn?.length ?: 10
        } catch (_: Exception) {
            10
        }

        lastAppliedMax = maxDigits
        binding.etPhone.filters = arrayOf(DigitMaxLengthFilter(maxDigits))
    }

    private fun updatePhoneCounter() {
        val digits = binding.etPhone.text?.count { it.isDigit() } ?: 0
        binding.tvPhoneCounter.text = "$digits / $lastAppliedMax"
    }
}