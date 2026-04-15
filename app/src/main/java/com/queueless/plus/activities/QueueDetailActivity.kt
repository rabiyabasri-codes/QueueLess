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
import com.queueless.plus.utils.hide
import com.queueless.plus.utils.show
import com.queueless.plus.utils.toast
import kotlinx.coroutines.launch

class QueueDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QUEUE_ID = "extra_queue_id"
    }

    private lateinit var binding: ActivityQueueDetailBinding
    private lateinit var session: SessionManager
    private var queue: Queue? = null
    private var isUserInQueue = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQueueDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)

        val queueId = intent.getStringExtra(EXTRA_QUEUE_ID) ?: run { finish(); return }

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

                    isUserInQueue = FirestoreRepository.isUserInQueue(session.userId, q.queueId)
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

            // Update estimated wait time for the whole queue
            queue?.let { q ->
                val totalWait = (entries.size) * q.avgServiceTime
                binding.tvEstimatedWait.text = "Est. total wait: ${totalWait.formatWaitTime()}"
            }
        }
    }

    private fun updateJoinButton() {
        if (isUserInQueue) {
            binding.btnJoinQueue.text = "View My Status"
            binding.btnJoinQueue.setOnClickListener { openUserStatus() }
        } else {
            binding.btnJoinQueue.text = "Join Queue"
            binding.btnJoinQueue.setOnClickListener { joinQueue() }
        }
    }

    private fun joinQueue() {
        val q = queue ?: return
        binding.btnJoinQueue.isEnabled = false

        lifecycleScope.launch {
            try {
                val entry = QueueEntry(
                    userId    = session.userId,
                    queueId   = q.queueId,
                    userName  = session.userName,
                    timestamp = Timestamp.now(),
                    status    = QueueEntry.STATUS_WAITING
                )
                FirestoreRepository.joinQueue(entry)
                isUserInQueue = true
                toast("You've joined the queue!")
                openUserStatus()
            } catch (e: Exception) {
                toast("Failed to join: ${e.message}")
                binding.btnJoinQueue.isEnabled = true
            }
        }
    }

    private fun openUserStatus() {
        val intent = Intent(this, UserStatusActivity::class.java).apply {
            putExtra(UserStatusActivity.EXTRA_QUEUE_ID, queue?.queueId)
        }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
}
