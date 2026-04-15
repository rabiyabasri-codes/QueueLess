package com.queueless.plus.models

import com.google.firebase.Timestamp

data class QueueEntry(
    val entryId: String = "",
    val userId: String = "",
    val queueId: String = "",
    val userName: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val status: String = STATUS_WAITING,   // waiting / completed / left
    val notified: Boolean = false
) {
    companion object {
        const val STATUS_WAITING   = "waiting"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_LEFT      = "left"
    }

    fun toMap(): Map<String, Any> = mapOf(
        "entryId"   to entryId,
        "userId"    to userId,
        "queueId"   to queueId,
        "userName"  to userName,
        "timestamp" to timestamp,
        "status"    to status,
        "notified"  to notified
    )
}
