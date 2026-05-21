package com.queueless.plus.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.queueless.plus.adapters.AdminQueueAdapter
import com.queueless.plus.databinding.ActivityAdminDashboardBinding
import com.queueless.plus.models.Queue
import com.queueless.plus.utils.AuthManager
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.NetworkUtils
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.hide
import com.queueless.plus.utils.show
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var session: SessionManager
    private lateinit var adapter: AdminQueueAdapter
    private var queueListener: ListenerRegistration? = null
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Admin: ${session.userName}"

        binding.tvAdminName.text = "Welcome, ${session.userName} 👋"

        setupButtons()
        setupRecyclerView()
        setupSwipeRefresh()
        setupNetworkMonitor()
        attachQueueListener()
        loadStats()
    }

    private fun setupButtons() {
        binding.btnLogout.setOnClickListener { logout() }

        binding.btnManageQueues.setOnClickListener {
            startActivity(Intent(this, AdminPanelActivity::class.java))
        }

        binding.btnScanQR.setOnClickListener {
            startActivity(Intent(this, QRScanActivity::class.java))
        }

        binding.btnManageMenu.setOnClickListener {
            startActivity(Intent(this, AdminMenuActivity::class.java))
        }

        binding.btnManageUsers.setOnClickListener {
            startActivity(Intent(this, UserManagementActivity::class.java))
        }

        binding.fabCreateQueue.setOnClickListener {
            startActivity(Intent(this, CreateQueueActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = AdminQueueAdapter(
            onManage = { queue -> openManageQueue(queue) },
            onDelete = { queue -> confirmDeleteQueue(queue) },
            onGenerateQr = { queue -> showQueueQr(queue) }
        )
        binding.rvQueues.layoutManager = LinearLayoutManager(this)
        binding.rvQueues.adapter = adapter
    }

    private fun setupNetworkMonitor() {
        updateAdminBanner(NetworkUtils.isConnected(this))
        networkCallback = NetworkUtils.registerNetworkCallback(
            context = this,
            onAvailable = { runOnUiThread { updateAdminBanner(true) } },
            onLost      = { runOnUiThread { updateAdminBanner(false) } }
        )
        binding.btnRetryAdmin.setOnClickListener {
            updateAdminBanner(NetworkUtils.isConnected(this))
            if (NetworkUtils.isConnected(this)) attachQueueListener()
        }
    }

    private fun updateAdminBanner(connected: Boolean) {
        binding.bannerNoInternet.visibility = if (connected) View.GONE else View.VISIBLE
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            attachQueueListener()
        }
    }

    private fun attachQueueListener() {
        binding.progressBar.show()
        queueListener?.remove()

        queueListener = FirestoreRepository.listenToQueues { queues ->
            if (isFinishing || isDestroyed) return@listenToQueues
            binding.progressBar.hide()
            binding.swipeRefresh.isRefreshing = false

            binding.tvStatQueues.text  = queues.size.toString()
            binding.tvStatWaiting.text = queues.sumOf { it.currentCount }.toString()

            adapter.submitList(queues)
            binding.tvEmpty.visibility = if (queues.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun loadStats() {
        lifecycleScope.launch {
            try {
                val todayOrders = FirestoreRepository.getTodayOrders()
                binding.tvStatOrders.text = todayOrders.size.toString()
            } catch (e: Exception) {
                binding.tvStatOrders.text = "—"
            }
        }
    }

    private fun openManageQueue(queue: Queue) {
        startActivity(
            Intent(this, ManageQueueActivity::class.java)
                .putExtra(ManageQueueActivity.EXTRA_QUEUE_ID, queue.queueId)
        )
    }

    private fun confirmDeleteQueue(queue: Queue) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Queue")
            .setMessage("Delete '${queue.queueName}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        FirestoreRepository.deleteQueue(queue.queueId)
                    } catch (e: Exception) { /* ignore */ }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showQueueQr(queue: Queue) {
        val qrContent = "queueless://join?queueId=${queue.queueId}"
        try {
            val size = 500
            val bitMatrix = com.google.zxing.MultiFormatWriter().encode(qrContent, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
            val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.RGB_565)
            for (x in 0 until size) for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
            val imageView = android.widget.ImageView(this).apply {
                setImageBitmap(bitmap); setPadding(24, 24, 24, 24)
            }
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("QR: ${queue.queueName}")
                .setView(imageView)
                .setPositiveButton("Close", null)
                .show()
        } catch (e: Exception) { /* ignore */ }
    }

    private fun logout() {
        AuthManager.logout()
        session.clear()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        attachQueueListener()
    }

    override fun onStop() {
        super.onStop()
        queueListener?.remove()
    }

    override fun onDestroy() {
        super.onDestroy()
        queueListener?.remove()
        networkCallback?.let { NetworkUtils.unregisterNetworkCallback(this, it) }
    }
}
