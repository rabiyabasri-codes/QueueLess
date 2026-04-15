package com.queueless.plus.models

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "user",   // "user" or "admin"
    val fcmToken: String = ""
) {
    fun isAdmin(): Boolean = role == "admin"

    fun toMap(): Map<String, Any> = mapOf(
        "userId"   to userId,
        "name"     to name,
        "email"    to email,
        "role"     to role,
        "fcmToken" to fcmToken
    )
}
