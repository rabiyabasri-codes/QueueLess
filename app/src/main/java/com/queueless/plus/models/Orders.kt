package com.queueless.plus.models

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val items: String = "",
    val total: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)