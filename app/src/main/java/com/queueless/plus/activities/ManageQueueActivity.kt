package com.queueless.plus.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.ListenerRegistration
import com.queueless.plus.adapters.ManageEntryAdapter
import com.queueless.plus.databinding.ActivityManageQueueBinding
import com.queueless.plus.models.Queue
import com.queueless.plus.models.QueueEntry
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.hide
import com.queueless.plus.utils.show
import com.queueless.plus.utils.toast
import kotlinx.coroutines.launch

class ManageQueueActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QUEUE_ID = "extra_queue_id"
    }

    private lateinit var binding: ActivityManageQueueBinding
    private lateinit var adapter: ManageEntryAdapter
    private var entriesListener: ListenerRegistration? = null
    private var queue: Queue? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val queueId = intent.getStringExtra(EXTRA_QUEUE_ID) ?: run {
            finish(); return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadQueue(queueId)
        setupRecyclerView()
        attachEntriesListener(queueId)
    }

    private fun loadQueue(queueId: String) {
        lifecycleScope.launch {
            queue = FirestoreRepository.getQueue(queueId)
            supportActionBar?.title = "Manage: ${queue?.queueName}"
        }
    }

    private fun setupRecyclerView() {
        adapter = ManageEntryAdapter(

            // 🟢 EXISTING
            onServed = { entry -> markServed(entry) },
            onRemove = { entry -> removeEntry(entry) },

            // 🔥 NEW ORDER CONTROLS
            onPreparing = { entry -> updateOrder(entry, QueueEntry.ORDER_PREPARING) },
            onReady = { entry -> updateOrder(entry, QueueEntry.ORDER_READY) }

        )

        binding.rvEntries.adapter = adapter
    }

    private fun attachEntriesListener(queueId: String) {
        binding.progressBar.show()

        entriesListener = FirestoreRepository.listenToQueueEntries(queueId) { entries ->
            binding.progressBar.hide()

            adapter.submitList(entries)

            binding.tvCount.text = "${entries.size} people waiting"

            if (entries.isEmpty()) binding.tvEmpty.show()
            else binding.tvEmpty.hide()
        }
    }

    // 🔥 NEW FUNCTION
    private fun updateOrder(entry: QueueEntry, status: String) {
        lifecycleScope.launch {
            try {
                FirestoreRepository.updateOrderStatus(entry.entryId, status)
                toast("Order updated to $status")
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            }
        }
    }

    private fun markServed(entry: QueueEntry) {
        lifecycleScope.launch {
            try {
                FirestoreRepository.updateEntryStatus(
                    entry.entryId,
                    QueueEntry.STATUS_COMPLETED
                )
                toast("${entry.userName} marked as served")
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            }
        }
    }

    private fun removeEntry(entry: QueueEntry) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Remove from Queue")
            .setMessage("Remove ${entry.userName} from the queue?")
            .setPositiveButton("Remove") { _, _ ->
                lifecycleScope.launch {
                    FirestoreRepository.updateEntryStatus(
                        entry.entryId,
                        QueueEntry.STATUS_LEFT
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        entriesListener?.remove()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}