package com.example.studentregistration.data

class UserRepository(private val userDao: UserDao) {

    private fun normEmail(email: String) = email.trim().lowercase()
    private fun normPass(pass: String) = pass.trim()

    suspend fun saveFullUser(user: User) {
        val normalized = user.copy(
            email = normEmail(user.email),
            password = normPass(user.password)
        )
        userDao.insertUser(normalized)
    }

    suspend fun loginUser(email: String, password: String): User? {
        return userDao.login(normEmail(email), normPass(password))
    }

    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(normEmail(email))
    }
}