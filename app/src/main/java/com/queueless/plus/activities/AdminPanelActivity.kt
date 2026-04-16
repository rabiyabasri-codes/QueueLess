package com.queueless.plus.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.ListenerRegistration
import com.queueless.plus.adapters.AdminQueueAdapter
import com.queueless.plus.databinding.ActivityAdminPanelBinding
import com.queueless.plus.models.Queue
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.hide
import com.queueless.plus.utils.show
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminPanelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminPanelBinding
    private lateinit var session: SessionManager
    private lateinit var adapter: AdminQueueAdapter
    private var queueListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Admin Panel"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()

        binding.fabCreateQueue.setOnClickListener {
            startActivity(Intent(this, CreateQueueActivity::class.java))
        }

        attachQueueListener()
    }

    private fun setupRecyclerView() {
        adapter = AdminQueueAdapter(
            onManage = { queue -> openManageQueue(queue) },
            onDelete = { queue -> deleteQueue(queue) }
        )
        binding.rvAdminQueues.adapter = adapter
    }

    private fun attachQueueListener() {
        binding.progressBar.show()

        queueListener = FirestoreRepository.listenToQueues { queues ->
            binding.progressBar.hide()
            adapter.submitList(queues)

            if (queues.isEmpty()) {
                binding.tvEmpty.show()
            } else {
                binding.tvEmpty.hide()
            }
        }
    }

    private fun openManageQueue(queue: Queue) {
        val intent = Intent(this, ManageQueueActivity::class.java).apply {
            putExtra(ManageQueueActivity.EXTRA_QUEUE_ID, queue.queueId)
        }
        startActivity(intent)
    }

    private fun deleteQueue(queue: Queue) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Queue")
            .setMessage("Are you sure you want to delete '${queue.queueName}'?")
            .setPositiveButton("Delete") { _, _ ->

                // ✅ FIXED coroutine usage
                lifecycleScope.launch(Dispatchers.IO) {
                    FirestoreRepository.deleteQueue(queue.queueId)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        queueListener?.remove()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}