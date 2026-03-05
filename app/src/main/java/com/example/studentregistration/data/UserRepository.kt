package com.example.studentregistration.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val userDao: UserDao) {

    private fun normEmail(email: String) = email.trim().lowercase()
    private fun normPass(pass: String) = pass.trim()

    suspend fun saveFullUser(user: User) = withContext(Dispatchers.IO) {
        val normalized = user.copy(
            email = normEmail(user.email),
            password = normPass(user.password)
        )
        userDao.insertUser(normalized) // REPLACE strategy in DAO avoids conflicts
    }

    suspend fun loginUser(email: String, password: String): User? = withContext(Dispatchers.IO) {
        userDao.login(normEmail(email), normPass(password))
    }

    suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByEmail(normEmail(email))
    }
}