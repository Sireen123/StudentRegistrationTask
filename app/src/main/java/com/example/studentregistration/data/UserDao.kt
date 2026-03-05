package com.example.studentregistration.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("""
        SELECT * FROM users
        WHERE TRIM(email) = TRIM(:email) COLLATE NOCASE
          AND TRIM(password) = TRIM(:password)
        LIMIT 1
    """)
    suspend fun login(email: String, password: String): User?

    @Query("""
        SELECT * FROM users
        WHERE TRIM(email) = TRIM(:email) COLLATE NOCASE
        LIMIT 1
    """)
    suspend fun getUserByEmail(email: String): User?
}