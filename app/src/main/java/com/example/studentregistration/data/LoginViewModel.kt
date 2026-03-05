package com.example.studentregistration.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class LoginViewModel(private val repo: UserRepository) : ViewModel() {

    private val _loginResult = MutableLiveData<User?>()
    val loginResult: LiveData<User?> = _loginResult

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun login(email: String, password: String) {
        viewModelScope.launch {
            val user = repo.loginUser(email, password)
            _loginResult.postValue(user)
            if (user == null) {
                _message.postValue("Invalid credentials")
            }
        }
    }
}