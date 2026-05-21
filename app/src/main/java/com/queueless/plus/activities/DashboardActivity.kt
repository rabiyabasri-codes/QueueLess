package com.queueless.plus.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.queueless.plus.R
import com.queueless.plus.adapters.QueueAdapter
import com.queueless.plus.databinding.ActivityDashboardBinding
import com.queueless.plus.models.Queue
import com.queueless.plus.utils.AuthManager
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.NetworkUtils
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.hide
import com.queueless.plus.utils.show
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var session: SessionManager
    private lateinit var adapter: QueueAdapter
    private var allQueues: List<Queue> = emptyList()
    private var queueListener: ListenerRegistration? = null
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupAdminControls()
        setupSwipeRefresh()
        loadDashboardStats()
        showOfflineIndicator()

        binding.fabChatbot.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }

        // Animate summary card
        binding.cardSummary.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.slide_in_from_top)
        )
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            val query = text?.toString().orEmpty()
            filterQueues(query)
        }

        // Chip listeners
        binding.chipShortWait.setOnCheckedChangeListener { _, _ -> filterQueues(binding.etSearch.text?.toString().orEmpty()) }
        binding.chipFood.setOnCheckedChangeListener { _, _ -> filterQueues(binding.etSearch.text?.toString().orEmpty()) }
        binding.chipRetail.setOnCheckedChangeListener { _, _ -> filterQueues(binding.etSearch.text?.toString().orEmpty()) }
        binding.chipNearby.setOnCheckedChangeListener { _, _ -> filterQueues(binding.etSearch.text?.toString().orEmpty()) }
    }

    private fun loadDashboardStats() {
        lifecycleScope.launch {
            try {
                val todayOrders = FirestoreRepository.getTodayOrders()
                binding.tvTodayOrders.text = todayOrders.size.toString()
            } catch (e: Exception) {
                binding.tvTodayOrders.text = "0"
            }
        }
    }

    private fun updateStats(queues: List<Queue>) {
        binding.tvActiveQueues.text = queues.size.toString()
        val totalWaiting = queues.sumOf { it.currentCount }
        binding.tvWaitingUsers.text = totalWaiting.toString()
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
        supportActionBar?.title = "Welcome back, ${session.userName}"

        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }

        binding.btnNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationCenterActivity::class.java))
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

    // Admin controls — admins are routed to AdminDashboardActivity at login
    private fun setupAdminControls() {
        // No admin buttons shown in user dashboard
    }

    // Swipe refresh
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            attachQueueListener()
        }
    }

    // Firebase listener
    private fun attachQueueListener() {

        binding.progressBar.show()
        queueListener?.remove()

        queueListener = FirestoreRepository.listenToQueues { queues ->

            if (isFinishing || isDestroyed) return@listenToQueues

            binding.progressBar.hide()
            binding.swipeRefresh.isRefreshing = false

            val sorted = queues.sortedBy { it.currentCount * it.avgServiceTime }
            allQueues = sorted
            updateStats(sorted)
            filterQueues(binding.etSearch.text?.toString().orEmpty())
        }
    }

    private fun filterQueues(query: String) {
        var filtered = allQueues

        // Search filter
        if (query.isNotEmpty()) {
            filtered = filtered.filter { queue ->
                queue.queueName.contains(query, ignoreCase = true) ||
                queue.description.contains(query, ignoreCase = true) ||
                queue.location.contains(query, ignoreCase = true)
            }
        }

        // Chip filters
        if (binding.chipShortWait.isChecked) {
            filtered = filtered.filter { it.currentCount * it.avgServiceTime < 30 } // Less than 30 min wait
        }
        if (binding.chipFood.isChecked) {
            filtered = filtered.filter { it.description.contains("food", ignoreCase = true) }
        }
        if (binding.chipRetail.isChecked) {
            filtered = filtered.filter { it.description.contains("retail", ignoreCase = true) || it.description.contains("store", ignoreCase = true) }
        }
        // Nearby would require location, for now skip

        adapter.submitList(filtered)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    // 📡 No-internet banner
    private fun showOfflineIndicator() {
        // Set initial state
        updateBanner(NetworkUtils.isConnected(this))

        // Listen for changes in real-time
        networkCallback = NetworkUtils.registerNetworkCallback(
            context = this,
            onAvailable = { runOnUiThread { updateBanner(true) } },
            onLost      = { runOnUiThread { updateBanner(false) } }
        )

        // RETRY tap
        binding.btnRetry.setOnClickListener {
            updateBanner(NetworkUtils.isConnected(this))
            if (NetworkUtils.isConnected(this)) attachQueueListener()
        }
    }

    private fun updateBanner(connected: Boolean) {
        binding.bannerNoInternet.visibility = if (connected) View.GONE else View.VISIBLE
    }

    // Open queue
    private fun openQueueDetail(queue: Queue) {
        val intent = Intent(this, QueueDetailActivity::class.java)
        intent.putExtra(QueueDetailActivity.EXTRA_QUEUE_ID, queue.queueId)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        queueListener?.remove()
        networkCallback?.let { NetworkUtils.unregisterNetworkCallback(this, it) }
    }
}