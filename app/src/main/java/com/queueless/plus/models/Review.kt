package com.queueless.plus.models

data class Review(
    val reviewId: String = "",
    val userId: String = "",
    val queueId: String = "",
    val rating: Int = 5, // 1-5 stars
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)