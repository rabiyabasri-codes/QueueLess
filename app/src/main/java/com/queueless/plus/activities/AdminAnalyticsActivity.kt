package com.queueless.plus.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.queueless.plus.databinding.ActivityAdminAnalyticsBinding
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.requireAdminAccess
import com.queueless.plus.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminAnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminAnalyticsBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        if (!requireAdminAccess(session)) return

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Queue Analytics"

        loadAnalytics()
        binding.swipeRefresh.setOnRefreshListener { loadAnalytics() }
    }

    private fun loadAnalytics() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val activeQueues = FirestoreRepository.getActiveQueueCount()
                val todayOrders = FirestoreRepository.getTodayOrders()
                val servedToday = FirestoreRepository.getTodayServedEntriesCount()

                val totalRevenue = todayOrders.sumOf { it.total }
                val avgOrderValue = if (todayOrders.isEmpty()) 0 else totalRevenue / todayOrders.size

                withContext(Dispatchers.Main) {
                    binding.tvActiveQueues.text = activeQueues.toString()
                    binding.tvTodayOrders.text = todayOrders.size.toString()
                    binding.tvServedToday.text = servedToday.toString()
                    binding.tvTodayRevenue.text = "Rs. $totalRevenue"
                    binding.tvAvgOrderValue.text = "Rs. $avgOrderValue"
                    binding.swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                    toast("Failed to load analytics: ${e.message}")
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
