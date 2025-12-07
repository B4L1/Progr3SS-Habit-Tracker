package com.example.lab4.data.local

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private var prefs: SharedPreferences = context.getSharedPreferences(PREFS_TOKEN_FILE, Context.MODE_PRIVATE)

    fun saveAccessToken(token: String) {
        val editor = prefs.edit()
        editor.putString(USER_ACCESS_TOKEN, token)
        editor.apply()
    }

    fun getAccessToken(): String? {
        return prefs.getString(USER_ACCESS_TOKEN, null)
    }

    fun saveRefreshToken(token: String) {
        val editor = prefs.edit()
        editor.putString(USER_REFRESH_TOKEN, token)
        editor.apply()
    }

    fun getRefreshToken(): String? {
        return prefs.getString(USER_REFRESH_TOKEN, null)
    }

    fun saveEmail(email: String) {
        val editor = prefs.edit()
        editor.putString(USER_EMAIL, email)
        editor.apply()
    }

    fun getEmail(): String? {
        return prefs.getString(USER_EMAIL, null)
    }
    
    fun saveApiKey(apiKey: String) {
        val editor = prefs.edit()
        editor.putString(OPENAI_API_KEY, apiKey)
        editor.apply()
    }

    fun getApiKey(): String? {
        return prefs.getString(OPENAI_API_KEY, null)
    }

    fun clearTokens() {
        val editor = prefs.edit()
        editor.remove(USER_ACCESS_TOKEN)
        editor.remove(USER_REFRESH_TOKEN)
        // We usually don't clear the email so it remains pre-filled for next login
        // We might want to keep the API key too, or clear it. For now, let's keep it.
        editor.apply()
    }

    companion object {
        const val PREFS_TOKEN_FILE = "prefs_token_file"
        const val USER_ACCESS_TOKEN = "user_access_token"
        const val USER_REFRESH_TOKEN = "user_refresh_token"
        const val USER_EMAIL = "user_email"
        const val OPENAI_API_KEY = "openai_api_key"
    }
}
