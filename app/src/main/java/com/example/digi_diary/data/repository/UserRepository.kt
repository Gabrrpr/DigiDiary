package com.example.digi_diary.data.repository

import com.example.digi_diary.data.AppDatabase
import com.example.digi_diary.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UserRepository(private val database: AppDatabase) {
    private val userDao = database.userDao()

    /**
     * Creates a new user profile in the local database
     * @param username The user's chosen username
     * @param email The user's email (must be unique)
     * @return true if the user was created successfully, false if the email or username is already taken
     */
    suspend fun createUserProfile(username: String, email: String): Boolean {
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
                
                // Create and insert new user profile
                val user = User(
                    username = username,
                    email = email
                )
                userDao.insert(user)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    /**
     * Gets a user by their email
     * @param email The email to search for
     * @return The user if found, null otherwise
     */
    suspend fun getUserByEmail(email: String): User? {
        return withContext(Dispatchers.IO) {
            try {
                userDao.getUserByEmail(email)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Checks if a username is already taken
     * @param username The username to check
     * @return true if the username is already taken, false otherwise
     */
    suspend fun isUsernameTaken(username: String): Boolean {
        return withContext(Dispatchers.IO) {
            userDao.getUserByUsername(username) != null
        }
    }
    
    /**
     * Gets all users (for debugging purposes only)
     * @return A list of all users in the database
     */
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
