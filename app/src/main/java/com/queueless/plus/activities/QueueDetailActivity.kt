package com.queueless.plus.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Timestamp
import com.queueless.plus.adapters.QueueEntryAdapter
import com.queueless.plus.databinding.ActivityQueueDetailBinding
import com.queueless.plus.models.Queue
import com.queueless.plus.models.QueueEntry
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.formatWaitTime
import com.queueless.plus.utils.toast
import kotlinx.coroutines.launch

class QueueDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QUEUE_ID = "extra_queue_id"
        const val EXTRA_ENTRY_ID = "extra_entry_id"
    }

    private lateinit var binding: ActivityQueueDetailBinding
    private lateinit var session: SessionManager
    private var queue: Queue? = null
    private var isUserInQueue = false
    private var currentEntryId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQueueDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        val queueId = intent.getStringExtra(EXTRA_QUEUE_ID) ?: run {
            finish(); return
        }

        setupToolbar()
        loadQueueData(queueId)
        setupEntryListener(queueId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun loadQueueData(queueId: String) {
        lifecycleScope.launch {
            try {
                queue = FirestoreRepository.getQueue(queueId) ?: return@launch

                queue?.let { q ->
                    supportActionBar?.title = q.queueName
                    binding.tvDescription.text = q.description
                    binding.tvServiceTime.text = "Avg. service time: ${q.avgServiceTime} min"
                    binding.tvLocation.text = q.location
                    binding.tvQueuePaused.visibility =
                        if (q.isPaused) android.view.View.VISIBLE else android.view.View.GONE

                    if (q.broadcastMessage.isNotBlank()) {
                        binding.tvQueueNotice.text = "Notice: ${q.broadcastMessage}"
                        binding.tvQueueNotice.visibility = android.view.View.VISIBLE
                    } else {
                        binding.tvQueueNotice.visibility = android.view.View.GONE
                    }

                    isUserInQueue =
                        FirestoreRepository.isUserInQueue(session.userId, q.queueId)

                    val entry = FirestoreRepository.getUserEntry(session.userId, q.queueId)
                    currentEntryId = entry?.entryId ?: ""

                    updateJoinButton()
                }

            } catch (e: Exception) {
                toast("Error loading queue: ${e.message}")
            }
        }
    }

    private fun setupEntryListener(queueId: String) {
        val adapter = QueueEntryAdapter()
        binding.rvQueueEntries.adapter = adapter

        FirestoreRepository.listenToQueueEntries(queueId) { entries ->
            adapter.submitList(entries)
            binding.tvQueueCount.text = "${entries.size} waiting"

            queue?.let { q ->
                val totalWait = entries.size * q.avgServiceTime
                binding.tvEstimatedWait.text =
                    "Est. total wait: ${totalWait.formatWaitTime()}"
            }
        }
    }

    private fun updateJoinButton() {
        if (isUserInQueue) {
            binding.btnJoinQueue.text = "View My Status"
            binding.btnJoinQueue.isEnabled = true
            binding.btnJoinQueue.setOnClickListener {
                openUserStatus()
            }
        } else if (queue?.isPaused == true) {
            binding.btnJoinQueue.text = "Queue Paused"
            binding.btnJoinQueue.isEnabled = false
            binding.btnJoinQueue.setOnClickListener(null)
        } else {
            binding.btnJoinQueue.text = "Join Queue"
            binding.btnJoinQueue.isEnabled = true
            binding.btnJoinQueue.setOnClickListener {
                joinQueue()
            }
        }
    }

    private fun joinQueue() {
        val q = queue ?: return
        if (q.isPaused) {
            toast("Queue is paused. Please try later.")
            return
        }
        binding.btnJoinQueue.isEnabled = false

        lifecycleScope.launch {
            try {

                val entry = QueueEntry(
                    userId = session.userId,
                    queueId = q.queueId,
                    userName = session.userName,
                    timestamp = Timestamp.now(),
                    status = QueueEntry.STATUS_WAITING
                )

                val entryId = FirestoreRepository.joinQueue(entry)
                FirestoreRepository.pushNotification(
                    userId = session.userId,
                    title = "Queue joined",
                    message = "You joined ${q.queueName}. Track your live status now."
                )

                currentEntryId = entryId
                isUserInQueue = true

                toast("Joined queue!")

                val intent = Intent(this@QueueDetailActivity, OrderActivity::class.java).apply {
                    putExtra("ENTRY_ID", entryId)
                    putExtra("QUEUE_ID", q.queueId)
                }

                startActivity(intent)

            } catch (e: Exception) {
                toast("Failed: ${e.message}")
                binding.btnJoinQueue.isEnabled = true
            }
        }
    }

    private fun openUserStatus() {

        if (currentEntryId.isEmpty()) {
            toast("Try again")
            return
        }

        val intent = Intent(this, UserStatusActivity::class.java).apply {
            putExtra(UserStatusActivity.EXTRA_QUEUE_ID, queue?.queueId)
            putExtra(UserStatusActivity.EXTRA_ENTRY_ID, currentEntryId)
        }

        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}