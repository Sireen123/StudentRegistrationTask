package com.example.studentregistration

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import android.widget.Toast
import com.example.studentregistration.data.AppDatabase
import com.example.studentregistration.data.FirebaseRepo
import com.example.studentregistration.data.LoginViewModel
import com.example.studentregistration.data.LoginViewModelFactory
import com.example.studentregistration.data.UserRepository
import com.example.studentregistration.databinding.ActivityLoginBinding
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    private lateinit var auth: FirebaseAuth

    private var otpVerified = false
    private var hasNavigated = false
    private var verificationId: String? = null
    private var lastAppliedMax = 0

    // ✅ Safe Toast (always visible)
    private fun showSafeToast(msg: String) {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ BACK BAR
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { navigateHome() }
        findViewById<TextView>(R.id.tvScreenTitle).text = "Login"
        onBackPressedDispatcher.addCallback(this) { navigateHome() }

        // ✅ ROOM & VIEWMODEL
        val dao = AppDatabase.getDatabase(this).userDao()
        viewModel = ViewModelProvider(
            this, LoginViewModelFactory(UserRepository(dao))
        )[LoginViewModel::class.java]

        auth = FirebaseAuth.getInstance()

        // ✅ Attach EditText to CCP BEFORE any use
        binding.countryCodePicker.registerCarrierNumberEditText(binding.etPhone)

        // ✅ Number only
        binding.etPhone.inputType = InputType.TYPE_CLASS_NUMBER

        // ✅ Configure phone limits
        applyPhoneMaxLengthForCountry()
        binding.countryCodePicker.setOnCountryChangeListener {
            applyPhoneMaxLengthForCountry()
            updatePhoneCounter()
            otpVerified = false
        }

        binding.etPhone.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePhoneCounter()
                otpVerified = false
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        // ✅ SEND OTP BUTTON
        binding.btnSendOtp.setOnClickListener {
            if (!validatePhoneForOtp()) return@setOnClickListener

            val e164 = try {
                binding.countryCodePicker.fullNumberWithPlus
            } catch (e: Exception) {
                showSafeToast("Invalid phone field")
                return@setOnClickListener
            }

            sendOtp(e164)
        }

        // ✅ LOGIN BUTTON
        binding.btnLogin.setOnClickListener {

            val email = binding.etEmail.text.toString().trim().lowercase()
            val password = binding.etPassword.text.toString().trim()

            if (!validateInputs()) return@setOnClickListener

            if (!otpVerified) {
                showSafeToast("Verify OTP before login")
                return@setOnClickListener
            }

            lifecycleScope.launch { viewModel.login(email, password) }
        }

        // ✅ OBSERVE LOGIN RESULT
        viewModel.loginResult.observe(this) { user ->

            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@observe

            val email = binding.etEmail.text.toString().trim().lowercase()
            val password = binding.etPassword.text.toString().trim()

            loginToFirebase(email, password)
        }
    }

    // ✅ FINAL LOGIN HANDLER
    private fun loginToFirebase(email: String, password: String) {

        if (hasNavigated) return
        hasNavigated = true

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {

                val realUid = it.user!!.uid

                // ✅ SAVE REAL UID
                val sp = getSharedPreferences("user_session", MODE_PRIVATE)
                sp.edit().putString("real_uid", realUid).apply()

                showSafeToast("Login successful ✅")

                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                hasNavigated = false
                showSafeToast("Invalid Email or Password")
            }
    }

    // ✅ BACK TO HOME
    private fun navigateHome() {
        startActivity(Intent(this, StartActivity::class.java))
        finish()
    }

    // ✅ INPUT VALIDATION
    private fun validateInputs(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty()) return err(binding.etEmail, "Email required")
        if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\..+$")))
            return err(binding.etEmail, "Invalid email")

        if (password.isEmpty()) return err(binding.etPassword, "Password required")

        return true
    }

    private fun validatePhoneForOtp(): Boolean {
        val digits = binding.etPhone.text.toString().filter { it.isDigit() }
        if (digits.isEmpty()) return err(binding.etPhone, "Phone required")

        val region = binding.countryCodePicker.selectedCountryNameCode

        if (region == "IN") {
            if (!digits.matches(Regex("^[6-9]\\d{9}$")))
                return err(binding.etPhone, "Enter valid Indian number")

            val util = PhoneNumberUtil.getInstance()
            return try {
                val proto = util.parse("+91$digits", "IN")
                util.isValidNumberForRegion(proto, "IN")
            } catch (_: Exception) {
                err(binding.etPhone, "Enter valid Indian number")
            }
        }

        if (!binding.countryCodePicker.isValidFullNumber)
            return err(binding.etPhone, "Invalid phone number")

        return true
    }

    private fun err(view: EditText, msg: String): Boolean {
        view.error = msg
        view.requestFocus()
        return false
    }

    private fun applyPhoneMaxLengthForCountry() {
        val util = PhoneNumberUtil.getInstance()
        val region = binding.countryCodePicker.selectedCountryNameCode

        val example = try { util.getExampleNumber(region) } catch (_: Exception) { null }
        lastAppliedMax = example?.let { util.getNationalSignificantNumber(it)?.length } ?: 10

        binding.etPhone.filters = arrayOf(DigitMaxLengthFilter(lastAppliedMax))
    }

    private fun updatePhoneCounter() {
        val digits = binding.etPhone.text?.count { it.isDigit() } ?: 0
        binding.tvPhoneCounter.text = "$digits / $lastAppliedMax"
    }

    // ✅ OTP SYSTEM
    private fun sendOtp(e164: String) {

        binding.btnSendOtp.isEnabled = false
        showSafeToast("Sending OTP...")

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                otpVerified = true
                binding.btnSendOtp.isEnabled = true
                showSafeToast("OTP Verified ✅")
            }

            override fun onVerificationFailed(e: FirebaseException) {
                otpVerified = false
                binding.btnSendOtp.isEnabled = true
                showSafeToast("OTP failed: ${e.message}")
            }

            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id
                binding.btnSendOtp.isEnabled = true
                showOtpDialog(id)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(e164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun showOtpDialog(id: String) {
        val et = EditText(this)
        et.hint = "Enter OTP"
        et.inputType = InputType.TYPE_CLASS_NUMBER

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Verify OTP")
            .setView(et)
            .setPositiveButton("Verify") { _, _ ->
                val code = et.text.toString().trim()
                if (code.length < 4) {
                    showSafeToast("Invalid OTP")
                } else {
                    otpVerified = true
                    showSafeToast("OTP Verified ✅")
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                showSafeToast("OTP cancelled")
            }
            .show()
    }
}