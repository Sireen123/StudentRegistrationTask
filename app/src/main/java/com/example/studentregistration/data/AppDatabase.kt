package com.example.studentregistration.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [User::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {

                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)

                            // FIXED: INSTANCE is used here (NOT tempInstance)
                            CoroutineScope(Dispatchers.IO).launch {

                                INSTANCE?.userDao()?.insertUser(
                                    User(
                                        name = "Admin",
                                        registerNo = "",
                                        rollNo = "",
                                        address = "",
                                        phone = "",
                                        email = "management@gmail.com",
                                        password = "1234",
                                        dob = "",
                                        gender = "Other",
                                        parentName = "",
                                        department = "",
                                        semester = "",
                                        role = "management",
                                        feesPaid = "0"
                                    )
                                )
                            }
                        }
                    })
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}