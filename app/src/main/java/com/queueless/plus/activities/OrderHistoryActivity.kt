package com.queueless.plus.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.queueless.plus.adapters.OrderHistoryAdapter
import com.queueless.plus.databinding.ActivityOrderHistoryBinding
import com.queueless.plus.models.Order
import com.queueless.plus.models.Review
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.toast
import kotlinx.coroutines.launch

class OrderHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderHistoryBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Order History"

        binding.rvOrders.layoutManager = LinearLayoutManager(this)

        loadOrders()
    }

    private fun loadOrders() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvOrders.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val orders = FirestoreRepository.getOrdersForUser(session.userId)

                binding.progressBar.visibility = View.GONE

                if (orders.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                } else {
                    binding.rvOrders.visibility = View.VISIBLE
                    binding.rvOrders.adapter = OrderHistoryAdapter(orders) { order ->
                        openReviewDialog(order)
                    }
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
                toast("Could not load orders: ${e.message}")
            }
        }
    }

    private fun openReviewDialog(order: Order) {
        val input = android.widget.EditText(this).apply { hint = "Write your review..." }
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Leave a Review")
            .setMessage("Rate this order:")
            .setView(input)
            .setPositiveButton("Submit") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val review = Review(
                            userId = session.userId,
                            queueId = order.queueId,
                            rating = 5,
                            comment = input.text?.toString()?.trim().orEmpty().ifBlank { "Great service!" }
                        )
                        FirestoreRepository.addReview(review)
                        toast("Review submitted!")
                    } catch (e: Exception) {
                        toast("Failed: ${e.message}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}