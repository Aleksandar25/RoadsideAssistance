package com.diploma.roadsideassistance.util

import com.diploma.roadsideassistance.data.remote.dto.ApiErrorBody
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException

// Централизирана обработка на Retrofit Response<T> -> Resource<T>,
// за да не се повтаря try/catch логиката във всеки repository метод.
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Resource<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            response.body()?.let { Resource.Success(it) }
                ?: Resource.Error("Празен отговор от сървъра")
        } else {
            Resource.Error(parseErrorMessage(response.errorBody()?.string()))
        }
    } catch (e: IOException) {
        Resource.Error("Няма връзка със сървъра. Провери дали бекендът работи и интернет връзката.")
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Неочаквана грешка")
    }
}

private fun parseErrorMessage(rawErrorBody: String?): String {
    if (rawErrorBody.isNullOrBlank()) return "Възникна грешка"
    return try {
        Gson().fromJson(rawErrorBody, ApiErrorBody::class.java)?.message ?: "Възникна грешка"
    } catch (e: Exception) {
        "Възникна грешка"
    }
}
