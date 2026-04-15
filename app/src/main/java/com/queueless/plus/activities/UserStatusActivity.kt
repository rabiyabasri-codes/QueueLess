package com.queueless.plus.activities

import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ListenerRegistration
import com.queueless.plus.databinding.ActivityUserStatusBinding
import com.queueless.plus.models.QueueEntry
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.formatWaitTime
import com.queueless.plus.utils.hide
import com.queueless.plus.utils.show
import com.queueless.plus.utils.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserStatusActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QUEUE_ID = "extra_queue_id"
    }

    private lateinit var binding: ActivityUserStatusBinding
    private lateinit var session: SessionManager

    private var entryListener: ListenerRegistration? = null
    private var entriesListener: ListenerRegistration? = null
    private var countdownTimer: CountDownTimer? = null
    private var currentEntry: QueueEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)

        val queueId = intent.getStringExtra(EXTRA_QUEUE_ID) ?: run { finish(); return }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Queue Status"

        attachListeners(queueId)

        binding.btnLeaveQueue.setOnClickListener { leaveQueue() }
    }

    /**
     * Two listeners run simultaneously:
     *   1. listenToUserEntry  – tracks this user's entry document (for status changes)
     *   2. listenToQueueEntries – tracks the full sorted queue (for position + wait time)
     */
    private fun attachListeners(queueId: String) {
        // Listener 1 – user's own entry
        entryListener = FirestoreRepository.listenToUserEntry(
            userId  = session.userId,
            queueId = queueId
        ) { entry ->
            currentEntry = entry
            if (entry == null) {
                // User's entry is gone — they've been served or removed
                toast("Your turn is complete!")
                finish()
            }
        }

        // Listener 2 – full queue (position + estimated wait)
        entriesListener = FirestoreRepository.listenToQueueEntries(queueId) { entries ->
            val position = entries.indexOfFirst { it.userId == session.userId } + 1
            if (position == 0) return@listenToQueueEntries   // not in list yet

            val usersAhead   = position - 1
            val avgTime      = 5   // fallback; ideally load from queue object
            val waitMinutes  = usersAhead * avgTime

            binding.tvPosition.text     = "#$position"
            binding.tvUsersAhead.text   = "$usersAhead ahead of you"
            binding.tvWaitTime.text     = waitMinutes.formatWaitTime()

            startCountdown(waitMinutes)

            // Algorithm 3 – Notify when position ≤ 2
            if (position <= 2 && currentEntry?.notified == false) {
                binding.tvNotifyBanner.show()
                binding.tvNotifyBanner.text = "🔔 Almost your turn!"
                currentEntry?.entryId?.let { id ->
                    CoroutineScope(Dispatchers.IO).launch {
                        FirestoreRepository.markEntryNotified(id)
                    }
                }
            }
        }
    }

    private fun startCountdown(waitMinutes: Int) {
        countdownTimer?.cancel()
        val totalMs = waitMinutes * 60 * 1000L
        countdownTimer = object : CountDownTimer(totalMs, 1000) {
            override fun onTick(millisLeft: Long) {
                val m = millisLeft / 60000
                val s = (millisLeft % 60000) / 1000
                binding.tvCountdown.text = String.format("%02d:%02d", m, s)
            }
            override fun onFinish() {
                binding.tvCountdown.text = "00:00"
            }
        }.start()
    }

    private fun leaveQueue() {
        val entry = currentEntry ?: return
        CoroutineScope(Dispatchers.IO).launch {
            FirestoreRepository.updateEntryStatus(entry.entryId, QueueEntry.STATUS_LEFT)
        }
        toast("You have left the queue.")
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        entryListener?.remove()
        entriesListener?.remove()
        countdownTimer?.cancel()
    }

    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
}
