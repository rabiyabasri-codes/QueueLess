package com.queueless.plus.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.queueless.plus.adapters.OrderHistoryAdapter
import com.queueless.plus.databinding.ActivityOrderHistoryBinding
import com.queueless.plus.models.Order
import com.queueless.plus.utils.SessionManager

class OrderHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderHistoryBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        binding.rvOrders.layoutManager = LinearLayoutManager(this)

        loadOrders()
    }

    private fun loadOrders() {
        db.collection("orders")
            .whereEqualTo("userId", session.userId)
            .get()
            .addOnSuccessListener { result ->
                val list = result.toObjects(Order::class.java)
                binding.rvOrders.adapter = OrderHistoryAdapter(list)
            }
    }
}