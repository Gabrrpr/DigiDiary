package com.example.digi_diary.data.repository

import com.example.digi_diary.data.AppDatabase
import com.example.digi_diary.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val database: AppDatabase) {
    private val userDao = database.userDao()

    suspend fun registerUser(username: String, email: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Check if user with this email already exists
                val existingUser = userDao.getUserByEmail(email)
                if (existingUser != null) {
                    return@withContext false
                }
                
                // Check if username is already taken
                val existingUsername = userDao.getUserByUsername(username)
                if (existingUsername != null) {
                    return@withContext false
                }
                
                // Create and insert new user
                val user = User(
                    username = username,
                    email = email,
                    password = password // In a real app, you should hash the password!
                )
                userDao.insert(user)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun loginUser(email: String, password: String): User? {
        return withContext(Dispatchers.IO) {
            try {
                userDao.getUser(email, password)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun isEmailTaken(email: String): Boolean {
        return withContext(Dispatchers.IO) {
            userDao.getUserByEmail(email) != null
        }
    }

    suspend fun isUsernameTaken(username: String): Boolean {
        return withContext(Dispatchers.IO) {
            userDao.getUserByUsername(username) != null
        }
    }
    
    // Debug method to get all users
    suspend fun getAllUsersForDebug(): List<User> {
        return withContext(Dispatchers.IO) {
            try {
                userDao.getAllUsers()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}
