package com.queueless.plus.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.queueless.plus.adapters.QueueAdapter
import com.queueless.plus.databinding.ActivityDashboardBinding
import com.queueless.plus.models.Queue
import com.queueless.plus.utils.AuthManager
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
        setupActions()
        attachQueueListener()
    }

    // 🔝 Toolbar
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Hi, ${session.userName}"

        // 🚪 Logout
        binding.btnLogout.setOnClickListener {
            logout()
        }

        // 📜 Order History
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }
    }

    private fun logout() {
        AuthManager.logout()
        session.clear()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    // 📜 RecyclerView
    private fun setupRecyclerView() {
        adapter = QueueAdapter { queue ->
            openQueueDetail(queue)
        }

        binding.rvQueues.layoutManager = LinearLayoutManager(this)
        binding.rvQueues.adapter = adapter
    }

    // 🔄 Swipe Refresh
    private fun setupActions() {
        binding.swipeRefresh.setOnRefreshListener {
            attachQueueListener()
        }
    }

    // ➕ Admin FAB
    private fun setupFab() {
        if (session.isAdmin) {
            binding.fabCreateQueue.show()

            binding.fabCreateQueue.setOnClickListener {
                startActivity(Intent(this, CreateQueueActivity::class.java))
            }
        } else {
            binding.fabCreateQueue.hide()
        }
    }

    // 🔄 Real-time queues
    private fun attachQueueListener() {

        binding.progressBar.show()

        queueListener?.remove() // 🔥 prevent duplicate listeners

        queueListener = FirestoreRepository.listenToQueues { queues ->

            if (isFinishing) return@listenToQueues

            binding.progressBar.hide()
            binding.swipeRefresh.isRefreshing = false

            adapter.submitList(queues)

            if (queues.isEmpty()) {
                binding.tvEmpty.show()
            } else {
                binding.tvEmpty.hide()
            }
        }
    }

    // 📍 Navigate
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