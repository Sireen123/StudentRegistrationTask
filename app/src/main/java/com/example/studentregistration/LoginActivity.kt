package com.example.studentregistration

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.studentregistration.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding


    private companion object {
        private const val STATIC_PASSWORD = "#Sireen123"
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_LAST_EMAIL = "last_email"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)


        prefs.getString(KEY_LAST_EMAIL, "")?.let { last ->
            if (last.isNotBlank()) {
                binding.etEmail.setText(last)
                binding.etEmail.setSelection(last.length)
            }
        }


        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            // Email validation
            if (email.isEmpty()) {
                binding.etEmail.error = "Email is required"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Enter a valid email address"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }


            if (password.isEmpty()) {
                binding.etPassword.error = "Password is required"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }
            if (password != STATIC_PASSWORD) {
                binding.etPassword.error = "Incorrect password"
                binding.etPassword.requestFocus()
                Toast.makeText(this, "Login failed: wrong password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            prefs.edit {
                putString(KEY_LAST_EMAIL, email)
            }


            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }
}