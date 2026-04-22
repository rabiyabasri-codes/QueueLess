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
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_FAVORITES = "favorite_items"
        private const val KEY_RECENT_ORDERS = "recent_orders"
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

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    val isAdmin: Boolean get() = userRole == "admin"

    fun isFavoriteItem(name: String): Boolean {
        val key = name.trim().lowercase()
        return getFavoriteSet().contains(key)
    }

    fun toggleFavoriteItem(name: String): Boolean {
        val key = name.trim().lowercase()
        val mutable = getFavoriteSet().toMutableSet()
        val nowFavorite = if (mutable.contains(key)) {
            mutable.remove(key)
            false
        } else {
            mutable.add(key)
            true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, mutable).apply()
        return nowFavorite
    }

    fun addRecentOrder(orderText: String, total: Int) {
        val value = "Rs. $total - ${orderText.replace("\n", ", ")}"
        val updated = mutableListOf(value).apply {
            addAll(getRecentOrders())
        }.distinct().take(10)
        prefs.edit().putString(KEY_RECENT_ORDERS, updated.joinToString("||")).apply()
    }

    fun getRecentOrders(): List<String> {
        val raw = prefs.getString(KEY_RECENT_ORDERS, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("||").filter { it.isNotBlank() }
    }

    fun getFavoriteItems(): List<String> {
        return getFavoriteSet().map { it.replaceFirstChar { ch -> ch.uppercase() } }.sorted()
    }

    private fun getFavoriteSet(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun clear() = prefs.edit().clear().apply()
}
