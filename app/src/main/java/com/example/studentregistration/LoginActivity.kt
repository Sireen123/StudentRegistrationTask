package com.example.studentregistration

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.studentregistration.data.AppDatabase
import com.example.studentregistration.data.LoginViewModel
import com.example.studentregistration.data.LoginViewModelFactory
import com.example.studentregistration.data.UserRepository
import com.example.studentregistration.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionPrefs
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionPrefs(this)


        val dao = AppDatabase.getDatabase(this).userDao()
        val repo = UserRepository(dao)
        val factory = LoginViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]


        val prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        binding.etEmail.setText(prefs.getString("last_email", ""))


        viewModel.loginResult.observe(this) { user ->
            if (user != null) {
                session.currentUserEmail = user.email
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DetailsActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Invalid Email or Password", Toast.LENGTH_SHORT).show()
            }
        }


        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim().lowercase()
            val pass = binding.etPassword.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Enter valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass.isEmpty()) {
                Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit().putString("last_email", email).apply()

            viewModel.login(email, pass)
        }


        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }


        binding.tvFeesList.setOnClickListener {
            startActivity(Intent(this, FeesListActivity::class.java))
        }
        Toast.makeText(this,"oncreate", Toast.LENGTH_SHORT).show()

    }

    override fun onStart() {
        super.onStart()

        Toast.makeText(this,"onstart", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        Toast.makeText(this,"onResume", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        Toast.makeText(this,"onpause", Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        Toast.makeText(this,"onStop", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this,"onDestor", Toast.LENGTH_SHORT).show()
    }
}