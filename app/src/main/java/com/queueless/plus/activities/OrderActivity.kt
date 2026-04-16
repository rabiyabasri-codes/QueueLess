package com.queueless.plus.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.queueless.plus.databinding.ActivityOrderBinding
import com.queueless.plus.utils.toast

class OrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderBinding
    private var entryId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        entryId = intent.getStringExtra("ENTRY_ID") ?: ""

        binding.btnPlaceOrder.setOnClickListener {
            val order = binding.etOrder.text.toString()

            if (order.isEmpty()) {
                toast("Enter your order")
                return@setOnClickListener
            }

            FirebaseFirestore.getInstance()
                .collection("entries")
                .document(entryId)
                .update(
                    mapOf(
                        "orderDetails" to order,
                        "orderStatus" to "waiting"
                    )
                )
                .addOnSuccessListener {
                    toast("Order placed successfully 🍔")
                    finish()
                }
                .addOnFailureListener {
                    toast("Failed to place order")
                }
        }
    }
}