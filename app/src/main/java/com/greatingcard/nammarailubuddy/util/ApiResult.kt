package com.greatingcard.nammarailubuddy.util

sealed class ApiResult<out T> {
    data object Loading : ApiResult<Nothing>()
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val retryable: Boolean = true) : ApiResult<Nothing>()
}
