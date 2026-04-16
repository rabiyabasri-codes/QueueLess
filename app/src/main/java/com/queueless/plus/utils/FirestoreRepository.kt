package com.queueless.plus.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.queueless.plus.models.Queue
import com.queueless.plus.models.QueueEntry
import com.queueless.plus.models.User
import kotlinx.coroutines.tasks.await

object FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    // ─── Collection references ─────────────────────────────
    private val usersRef        = db.collection("users")
    private val queuesRef       = db.collection("queues")
    private val queueEntriesRef = db.collection("queueEntries")

    // ═════════════════ USER OPERATIONS ═════════════════════

    suspend fun saveUser(user: User) {
        usersRef.document(user.userId).set(user.toMap()).await()
    }

    suspend fun getUser(userId: String): User? {
        val snap = usersRef.document(userId).get().await()
        return if (snap.exists()) snap.toObject(User::class.java) else null
    }

    suspend fun updateFcmToken(userId: String, token: String) {
        usersRef.document(userId).update("fcmToken", token).await()
    }

    // ═════════════════ QUEUE OPERATIONS ════════════════════

    suspend fun createQueue(queue: Queue): String {
        val docRef = queuesRef.document()
        val newQueue = queue.copy(queueId = docRef.id)
        docRef.set(newQueue.toMap()).await()
        return docRef.id
    }

    suspend fun getQueues(): List<Queue> {
        val snap = queuesRef.whereEqualTo("isActive", true).get().await()
        return snap.toObjects(Queue::class.java)
    }

    suspend fun getQueue(queueId: String): Queue? {
        val snap = queuesRef.document(queueId).get().await()
        return if (snap.exists()) snap.toObject(Queue::class.java) else null
    }

    suspend fun updateQueue(queueId: String, updates: Map<String, Any>) {
        queuesRef.document(queueId).update(updates).await()
    }

    suspend fun deleteQueue(queueId: String) {
        queuesRef.document(queueId).update("isActive", false).await()
    }

    fun listenToQueues(onUpdate: (List<Queue>) -> Unit): ListenerRegistration {
        return queuesRef
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snap, _ ->
                snap?.let { onUpdate(it.toObjects(Queue::class.java)) }
            }
    }

    // ═══════════════ QUEUE ENTRY OPERATIONS ════════════════

    suspend fun joinQueue(entry: QueueEntry): String {
        val docRef = queueEntriesRef.document()
        val newEntry = entry.copy(entryId = docRef.id)
        docRef.set(newEntry.toMap()).await()
        return docRef.id
    }

    suspend fun getWaitingEntries(queueId: String): List<QueueEntry> {
        val snap = queueEntriesRef
            .whereEqualTo("queueId", queueId)
            .whereEqualTo("status", QueueEntry.STATUS_WAITING)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .await()
        return snap.toObjects(QueueEntry::class.java)
    }

    fun listenToQueueEntries(
        queueId: String,
        onUpdate: (List<QueueEntry>) -> Unit
    ): ListenerRegistration {
        return queueEntriesRef
            .whereEqualTo("queueId", queueId)
            .whereEqualTo("status", QueueEntry.STATUS_WAITING)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                snap?.let { onUpdate(it.toObjects(QueueEntry::class.java)) }
            }
    }

    fun listenToUserEntry(
        userId: String,
        queueId: String,
        onUpdate: (QueueEntry?) -> Unit
    ): ListenerRegistration {
        return queueEntriesRef
            .whereEqualTo("userId", userId)
            .whereEqualTo("queueId", queueId)
            .whereEqualTo("status", QueueEntry.STATUS_WAITING)
            .addSnapshotListener { snap, _ ->
                val entry = snap?.documents?.firstOrNull()
                    ?.toObject(QueueEntry::class.java)
                onUpdate(entry)
            }
    }

    suspend fun updateEntryStatus(entryId: String, status: String) {
        queueEntriesRef.document(entryId)
            .update("status", status)
            .await()
    }

    suspend fun markEntryNotified(entryId: String) {
        queueEntriesRef.document(entryId)
            .update("notified", true)
            .await()
    }

    suspend fun isUserInQueue(userId: String, queueId: String): Boolean {
        val snap = queueEntriesRef
            .whereEqualTo("userId", userId)
            .whereEqualTo("queueId", queueId)
            .whereEqualTo("status", QueueEntry.STATUS_WAITING)
            .get()
            .await()
        return !snap.isEmpty
    }

    // ═══════════════ 🔥 NEW ORDER FUNCTION ════════════════

    suspend fun updateOrderStatus(entryId: String, status: String) {
        queueEntriesRef.document(entryId)
            .update("orderStatus", status)
            .await()
    }

    // ═══════════════ LOGIC FUNCTIONS ══════════════════════

    suspend fun getUserPosition(userId: String, queueId: String): Int {
        val entries = getWaitingEntries(queueId)
        val index = entries.indexOfFirst { it.userId == userId }
        return if (index >= 0) index + 1 else -1
    }

    suspend fun getEstimatedWaitTime(userId: String, queue: Queue): Int {
        val position = getUserPosition(userId, queue.queueId)
        if (position <= 0) return 0
        val usersAhead = position - 1
        return usersAhead * queue.avgServiceTime
    }
}