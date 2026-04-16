package com.queueless.plus.activities

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.ListenerRegistration
import com.queueless.plus.databinding.ActivityUserStatusBinding
import com.queueless.plus.models.QueueEntry
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.formatWaitTime
import com.queueless.plus.utils.hide
import com.queueless.plus.utils.show
import com.queueless.plus.utils.toast
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

        val queueId = intent.getStringExtra(EXTRA_QUEUE_ID) ?: run {
            finish(); return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Queue Status"

        attachListeners(queueId)

        // 🔥 Leave Queue
        binding.btnLeaveQueue.setOnClickListener {
            leaveQueue()
        }

        // 🔥 Open Order Screen
        binding.btnOrder.setOnClickListener {
            val entryId = currentEntry?.entryId ?: return@setOnClickListener
            val intent = Intent(this, OrderActivity::class.java)
            intent.putExtra("ENTRY_ID", entryId)
            startActivity(intent)
        }
    }

    private fun attachListeners(queueId: String) {

        // 🔹 Listen to THIS user's entry
        entryListener = FirestoreRepository.listenToUserEntry(
            userId = session.userId,
            queueId = queueId
        ) { entry ->

            currentEntry = entry

            if (entry == null) {
                toast("Your turn is complete!")
                finish()
                return@listenToUserEntry
            }

            // 🔥 SHOW ORDER DETAILS
            binding.tvOrder.text =
                "Order: ${if (entry.orderDetails.isEmpty()) "Not placed" else entry.orderDetails}"

            binding.tvOrderStatus.text =
                "Status: ${entry.orderStatus}"

            // 🔔 If order ready
            if (entry.orderStatus == QueueEntry.ORDER_READY) {
                toast("Your order is ready! 🍔")
            }
        }

        // 🔹 Listen to queue for position
        entriesListener = FirestoreRepository.listenToQueueEntries(queueId) { entries ->

            val position = entries.indexOfFirst { it.userId == session.userId } + 1
            if (position == 0) return@listenToQueueEntries

            val usersAhead = position - 1
            val waitMinutes = usersAhead * 5

            binding.tvPosition.text = "#$position"
            binding.tvUsersAhead.text = "$usersAhead ahead of you"
            binding.tvWaitTime.text = waitMinutes.formatWaitTime()

            startCountdown(waitMinutes)

            // 🔔 Notify when near turn
            if (position <= 2 && currentEntry?.notified == false) {

                binding.tvNotifyBanner.show()
                binding.tvBannerText.text = "🔔 Almost your turn!"

                currentEntry?.entryId?.let { id ->
                    lifecycleScope.launch(Dispatchers.IO) {
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

        lifecycleScope.launch(Dispatchers.IO) {
            FirestoreRepository.updateEntryStatus(
                entry.entryId,
                QueueEntry.STATUS_LEFT
            )
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}