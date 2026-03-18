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
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.studentregistration.data.AppDatabase
import com.example.studentregistration.data.LoginViewModel
import com.example.studentregistration.data.LoginViewModelFactory
import com.example.studentregistration.data.UserRepository
import com.example.studentregistration.databinding.ActivityLoginBinding
import com.google.i18n.phonenumbers.PhoneNumberUtil

// Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    private var lastAppliedMax = 0
    private var hasNavigated = false
    private var otpVerified = false

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back Bar
        val backBtn = findViewById<ImageButton>(R.id.btnBack)
        val title = findViewById<TextView>(R.id.tvScreenTitle)
        title?.text = "Login"
        backBtn?.setOnClickListener { navigateUpToStart() }
        onBackPressedDispatcher.addCallback(this) { navigateUpToStart() }

        // DB + ViewModel
        val dao = AppDatabase.getDatabase(this).userDao()
        val repo = UserRepository(dao)
        val factory = LoginViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        // Firebase
        auth = FirebaseAuth.getInstance()

        // Prefill email
        val prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        binding.etEmail.setText(prefs.getString("last_email", ""))

        // Phone number — digits only
        binding.etPhone.inputType = InputType.TYPE_CLASS_NUMBER

        // Country picker setup
        binding.countryCodePicker.registerCarrierNumberEditText(binding.etPhone)
        applyPhoneMaxLengthForCountry()
        binding.countryCodePicker.setOnCountryChangeListener {
            applyPhoneMaxLengthForCountry()
            updatePhoneCounter()
            otpVerified = false
        }

        // TextWatcher
        binding.etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePhoneCounter()
                otpVerified = false
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // SEND OTP
        binding.btnSendOtp.setOnClickListener {
            if (!validatePhoneForOtp()) return@setOnClickListener

            val e164 = binding.countryCodePicker.fullNumberWithPlus
            binding.btnSendOtp.isEnabled = false

            sendOtp(
                e164,
                onVerified = {
                    otpVerified = true
                    Toast.makeText(this, "Phone verified", Toast.LENGTH_SHORT).show()
                    binding.btnSendOtp.isEnabled = true
                },
                onFailed = {
                    otpVerified = false
                    binding.btnSendOtp.isEnabled = true
                }
            )
        }

        // LOGIN BUTTON (kept same)
        binding.btnLogin.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener

            if (!otpVerified) {
                Toast.makeText(this, "Verify phone via OTP first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            prefs.edit().putString("last_email", email).apply()
            binding.btnLogin.isEnabled = false

            lifecycleScope.launch {
                viewModel.login(email.lowercase(), password)
            }
        }

        // LOGIN RESULT (add Firestore prefetch; navigation unchanged)
        viewModel.loginResult.observe(this) { user ->
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@observe

            if (user != null) {
                if (hasNavigated) return@observe
                hasNavigated = true

                // Fire-and-forget: sign in to Firebase with email/pass and prefetch Firestore
                val emailNow = binding.etEmail.text.toString().trim().lowercase()
                val passwordNow = binding.etPassword.text.toString().trim()
                signInAndPrefetchFirestore(emailNow, passwordNow)

                // Original navigation
                startActivity(
                    Intent(this, DashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("email_from_login", user.email)
                    }
                )
                finish()

            } else {
                Toast.makeText(this, "Invalid Email or Password", Toast.LENGTH_SHORT).show()
                binding.btnLogin.isEnabled = true
            }
        }
    }

    // BACK
    private fun navigateUpToStart() {
        startActivity(
            Intent(this, StartActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }
        )
        finish()
    }

    // VALIDATION (same)
    private fun validateInputs(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val phoneText = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty()) return err(binding.etEmail, "Email required")
        if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")))
            return err(binding.etEmail, "Enter valid email")

        if (phoneText.isEmpty()) return err(binding.etPhone, "Phone required")
        if (phoneText.count { it.isDigit() } < 10)
            return err(binding.etPhone, "Phone must have minimum 10 digits")

        val region = binding.countryCodePicker.selectedCountryNameCode
        val digits = phoneText.filter { it.isDigit() }

        if (region == "IN") {
            if (!digits.matches(Regex("^[6-9]\\d{9}$")))
                return err(binding.etPhone, "Enter valid Indian number")

            val util = PhoneNumberUtil.getInstance()
            try {
                val proto = util.parse("+91$digits", "IN")
                if (!util.isValidNumberForRegion(proto, "IN"))
                    return err(binding.etPhone, "Enter valid Indian number")
            } catch (_: Exception) {
                return err(binding.etPhone, "Enter valid Indian number")
            }
        }

        if (password.isEmpty()) return err(binding.etPassword, "Password required")
        return true
    }

    private fun validatePhoneForOtp(): Boolean {
        val phoneText = binding.etPhone.text.toString().trim()
        if (phoneText.isEmpty()) return err(binding.etPhone, "Phone required")

        val region = binding.countryCodePicker.selectedCountryNameCode
        val digits = phoneText.filter { it.isDigit() }

        if (region == "IN") {
            if (!digits.matches(Regex("^[6-9]\\d{9}$")))
                return err(binding.etPhone, "Enter valid Indian number")

            val util = PhoneNumberUtil.getInstance()
            return try {
                val proto = util.parse("+91$digits", "IN")
                util.isValidNumberForRegion(proto, "IN")
            } catch (_: Exception) {
                err(binding.etPhone, "Enter valid Indian number")
                false
            }
        }

        if (!binding.countryCodePicker.isValidFullNumber)
            return err(binding.etPhone, "Invalid phone number")

        return true
    }

    private fun err(v: EditText, msg: String): Boolean {
        v.error = msg
        v.requestFocus()
        return false
    }

    private fun applyPhoneMaxLengthForCountry() {
        val util = PhoneNumberUtil.getInstance()
        val region = binding.countryCodePicker.selectedCountryNameCode

        val example = try {
            util.getExampleNumber(region)
        } catch (_: Exception) {
            null
        }

        lastAppliedMax = example?.let { util.getNationalSignificantNumber(it)?.length } ?: 10
        binding.etPhone.filters = arrayOf(DigitMaxLengthFilter(lastAppliedMax))
    }

    private fun updatePhoneCounter() {
        val digits = binding.etPhone.text?.count { it.isDigit() } ?: 0
        binding.tvPhoneCounter.text = "$digits / $lastAppliedMax"
    }

    // OTP send & verify
    private fun sendOtp(
        e164: String,
        onVerified: () -> Unit,
        onFailed: () -> Unit
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneCredential(credential, onVerified, onFailed)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(
                    this@LoginActivity,
                    "OTP failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                onFailed()
            }

            override fun onCodeSent(
                id: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = id
                resendToken = token

                showOtpDialog(
                    onSubmit = { code ->
                        val cred = PhoneAuthProvider.getCredential(id, code)
                        signInWithPhoneCredential(cred, onVerified, onFailed)
                    },
                    onCancel = { onFailed() }
                )
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

    private fun signInWithPhoneCredential(
        credential: PhoneAuthCredential,
        onVerified: () -> Unit,
        onFailed: () -> Unit
    ) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener { onVerified() }
            .addOnFailureListener {
                Toast.makeText(this, "OTP invalid", Toast.LENGTH_SHORT).show()
                onFailed()
            }
    }

    private fun showOtpDialog(
        onSubmit: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        val et = EditText(this).apply {
            hint = "Enter OTP"
            inputType = InputType.TYPE_CLASS_NUMBER
            maxLines = 1
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Verify Phone")
            .setView(et)
            .setPositiveButton("Verify") { _, _ ->
                val code = et.text.toString().trim()
                if (code.length < 4)
                    Toast.makeText(this, "Enter valid OTP", Toast.LENGTH_SHORT).show()
                else
                    onSubmit(code)
            }
            .setNegativeButton("Cancel") { _, _ -> onCancel() }
            .setCancelable(false)
            .show()
    }

    // Email/Password sign-in + Firestore prefetch (doesn't block navigation)
    private fun signInAndPrefetchFirestore(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) return

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { res ->
                val uid = res.user?.uid ?: return@addOnSuccessListener
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        val sp = getSharedPreferences("user", Context.MODE_PRIVATE)
                        sp.edit()
                            .putString("uid", uid)
                            .putString("name", doc.getString("name"))
                            .putString("email", doc.getString("email"))
                            .putString("phone", doc.getString("phone"))
                            .putString("department", doc.getString("department"))
                            .putString("semester", doc.getString("semester"))
                            .putString("role", doc.getString("role"))
                            .apply()
                    }
                    .addOnFailureListener { /* ignore */ }
            }
            .addOnFailureListener { /* user may not exist in Firebase yet */ }
    }
}