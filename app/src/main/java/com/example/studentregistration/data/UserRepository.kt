package com.example.studentregistration.data

class UserRepository(private val userDao: UserDao) {


    suspend fun saveFullUser(user: User) {
        userDao.insertUser(user)
    }


    suspend fun loginUser(email: String, password: String): User? {
        return userDao.login(email, password)
    }


    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }
}