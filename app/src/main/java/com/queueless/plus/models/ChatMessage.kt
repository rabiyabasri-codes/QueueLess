package com.queueless.plus.models

data class ChatMessage(
    val message: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Long = 0L
)