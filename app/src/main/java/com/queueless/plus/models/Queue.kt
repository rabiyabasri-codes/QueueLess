package com.queueless.plus.models

data class Queue(
    val queueId: String = "",
    val queueName: String = "",
    val description: String = "",
    val avgServiceTime: Int = 5,   // in minutes
    val createdBy: String = "",    // admin userId
    val isActive: Boolean = true,
    val isPaused: Boolean = false,
    val pauseReason: String = "",
    val broadcastMessage: String = "",
    val location: String = "",
    val currentCount: Int = 0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "queueId"        to queueId,
        "queueName"      to queueName,
        "description"    to description,
        "avgServiceTime" to avgServiceTime,
        "createdBy"      to createdBy,
        "isActive"       to isActive,
        "isPaused"       to isPaused,
        "pauseReason"    to pauseReason,
        "broadcastMessage" to broadcastMessage,
        "location"       to location,
        "currentCount"   to currentCount
    )
}
