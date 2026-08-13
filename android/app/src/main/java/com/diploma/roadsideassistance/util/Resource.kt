package com.diploma.roadsideassistance.util

// Обвивка за UI състояние на асинхронна операция (мрежова заявка)
sealed class Resource<out T> {
    data object Idle : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}
