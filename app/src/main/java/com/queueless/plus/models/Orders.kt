package com.queueless.plus.models

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val queueId: String = "",
    val items: String = "",
    val total: Int = 0,
    val paymentMethod: String = "Cash on Delivery",
    val status: String = "Placed", // Placed, Preparing, Ready, Completed
    val timestamp: Long = System.currentTimeMillis()
)