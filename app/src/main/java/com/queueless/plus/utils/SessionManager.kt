package com.queueless.plus.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("queueless_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID   = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }

    var userId: String
        get() = prefs.getString(KEY_USER_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userRole: String
        get() = prefs.getString(KEY_USER_ROLE, "user") ?: "user"
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value).apply()

    var fcmToken: String
        get() = prefs.getString(KEY_FCM_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FCM_TOKEN, value).apply()

    val isAdmin: Boolean get() = userRole == "admin"

    fun clear() = prefs.edit().clear().apply()
}
