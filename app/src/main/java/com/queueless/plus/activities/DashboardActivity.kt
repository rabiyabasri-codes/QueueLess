package com.queueless.plus.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ListenerRegistration
import com.queueless.plus.adapters.QueueAdapter
import com.queueless.plus.databinding.ActivityDashboardBinding
import com.queueless.plus.models.Queue
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.hide
import com.queueless.plus.utils.show

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var session: SessionManager
    private lateinit var adapter: QueueAdapter
    private var queueListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        attachQueueListener()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Welcome, ${session.userName}"

        binding.btnLogout.setOnClickListener {
            com.queueless.plus.utils.AuthManager.logout()
            session.clear()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    private fun setupRecyclerView() {
        adapter = QueueAdapter { queue -> openQueueDetail(queue) }
        binding.rvQueues.adapter = adapter
    }

    private fun setupFab() {
        // Admin-only: FAB to create a new queue
        if (session.isAdmin) {
            binding.fabCreateQueue.show()
            binding.fabCreateQueue.setOnClickListener {
                startActivity(Intent(this, CreateQueueActivity::class.java))
            }
        } else {
            binding.fabCreateQueue.hide()
        }
    }

    // ─── Real-time queue updates ──────────────────────────────────────────────

    private fun attachQueueListener() {
        binding.progressBar.show()
        queueListener = FirestoreRepository.listenToQueues { queues ->
            binding.progressBar.hide()
            adapter.submitList(queues)
            if (queues.isEmpty()) binding.tvEmpty.show() else binding.tvEmpty.hide()
        }
    }

    private fun openQueueDetail(queue: Queue) {
        val intent = Intent(this, QueueDetailActivity::class.java).apply {
            putExtra(QueueDetailActivity.EXTRA_QUEUE_ID, queue.queueId)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        queueListener?.remove()
    }
}
