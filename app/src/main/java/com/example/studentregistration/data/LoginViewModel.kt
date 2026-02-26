package com.example.studentregistration.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class LoginViewModel(private val repo: UserRepository) : ViewModel() {

    val loginResult = MutableLiveData<User?>()
    val message = MutableLiveData<String>()

    // LOGIN ONLY (Registration moved to MainActivity)
    fun login(email: String, password: String) {
        viewModelScope.launch {
            val user = repo.loginUser(email, password)
            loginResult.postValue(user)

            if (user == null) {
                message.postValue("Invalid credentials")
            }
        }
    }
}