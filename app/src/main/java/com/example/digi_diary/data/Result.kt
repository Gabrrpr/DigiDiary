package com.example.digi_diary.data

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>() {
        val isSuccess = true
    }
    
    data class Error(val exception: Exception) : Result<Nothing>() {
        val isError = true
        val message: String = exception.message ?: "An unknown error occurred"
    }
    
    object Loading : Result<Nothing>()
    
    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun error(exception: Exception): Result<Nothing> = Error(exception)
        fun <T> error(message: String): Result<T> = Error(Exception(message))
        fun loading(): Result<Nothing> = Loading
    }
}

// Extension function to transform Result<T> to Result<R>
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> this
        is Result.Loading -> this
    }
}

// Extension function to handle success and error cases
inline fun <T> Result<T>.onSuccess(block: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        block(data)
    }
    return this
}

inline fun <T> Result<T>.onError(block: (String) -> Unit): Result<T> {
    if (this is Result.Error) {
        block(message)
    }
    return this
}

inline fun <T> Result<T>.onLoading(block: () -> Unit): Result<T> {
    if (this is Result.Loading) {
        block()
    }
    return this
}
