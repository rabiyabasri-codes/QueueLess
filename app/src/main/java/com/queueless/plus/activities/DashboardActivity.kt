package com.queueless.plus.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
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
        setupAdminControls()
        setupActions()
    }

    override fun onStart() {
        super.onStart()
        attachQueueListener()
    }

    override fun onStop() {
        super.onStop()
        queueListener?.remove()
    }

    // 🔝 Toolbar
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Hi, ${session.userName}"

        binding.btnLogout.setOnClickListener { logout() }

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
        finish()
    }

    // 📜 RecyclerView
    private fun setupRecyclerView() {
        adapter = QueueAdapter { queue ->
            openQueueDetail(queue)
        }

        binding.rvQueues.layoutManager = LinearLayoutManager(this)
        binding.rvQueues.adapter = adapter
    }

    // 🔥 ADMIN CONTROLS (FINAL FIX)
    private fun setupAdminControls() {

        if (session.isAdmin) {

            // ✅ Show FAB
            binding.fabCreateQueue.show()

            // ✅ Show Manage Menu button
            binding.btnManageMenu.visibility = View.VISIBLE

            // 🔥 Button → direct open menu
            binding.btnManageMenu.setOnClickListener {
                startActivity(Intent(this, AdminMenuActivity::class.java))
            }

            // 🔥 FAB → chooser dialog
            binding.fabCreateQueue.setOnClickListener {

                val options = arrayOf("Create Queue", "Manage Menu")

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Admin Actions")
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> startActivity(Intent(this, CreateQueueActivity::class.java))
                            1 -> startActivity(Intent(this, AdminMenuActivity::class.java))
                        }
                    }
                    .show()
            }

        } else {
            binding.fabCreateQueue.hide()
            binding.btnManageMenu.visibility = View.GONE
        }
    }

    // 🔄 Swipe Refresh
    private fun setupActions() {
        binding.swipeRefresh.setOnRefreshListener {
            attachQueueListener()
        }
    }

    // 🔄 Real-time queues
    private fun attachQueueListener() {

        binding.progressBar.show()
        queueListener?.remove()

        queueListener = FirestoreRepository.listenToQueues { queues ->

            if (isFinishing || isDestroyed) return@listenToQueues

            binding.progressBar.hide()
            binding.swipeRefresh.isRefreshing = false

            adapter.submitList(queues)

            if (queues.isEmpty()) binding.tvEmpty.show()
            else binding.tvEmpty.hide()
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