package com.diploma.roadsideassistance.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.diploma.roadsideassistance.core.Constants
import com.diploma.roadsideassistance.data.remote.dto.UserDto
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = Constants.DATASTORE_NAME)

// Локално съхранение на JWT токена и текущия потребител (DataStore Preferences),
// за да остане потребителят логнат между стартиранията на приложението.
class TokenManager(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("auth_token")
        val USER_JSON = stringPreferencesKey("auth_user")
    }

    private val gson = Gson()

    val tokenFlow: Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[Keys.TOKEN] }

    val userFlow: Flow<UserDto?> =
        context.dataStore.data.map { prefs ->
            prefs[Keys.USER_JSON]?.let { json -> gson.fromJson(json, UserDto::class.java) }
        }

    suspend fun getTokenBlocking(): String? = tokenFlow.first()

    suspend fun getUserBlocking(): UserDto? = userFlow.first()

    suspend fun saveSession(token: String, user: UserDto) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USER_JSON] = gson.toJson(user)
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.TOKEN)
            prefs.remove(Keys.USER_JSON)
        }
    }
}
