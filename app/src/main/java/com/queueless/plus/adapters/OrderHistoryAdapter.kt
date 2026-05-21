package com.queueless.plus.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.queueless.plus.databinding.ItemOrderHistoryBinding
import com.queueless.plus.models.Order

class OrderHistoryAdapter(
    private val list: List<Order>,
    private val onReviewClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemOrderHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = list[position]
        holder.binding.tvItems.text = order.items
        holder.binding.tvPayment.text = "Payment: ${order.paymentMethod}"
        holder.binding.tvTotal.text = "₹${order.total}"
        holder.binding.tvStatus.text = "Status: ${order.status}"
        holder.binding.tvStatus.setTextColor(getStatusColor(order.status))
        holder.binding.btnReview.setOnClickListener { onReviewClick(order) }
    }

    override fun getItemCount() = list.size

    private fun getStatusColor(status: String): Int {
        return when (status) {
            "Placed" -> Color.BLUE
            "Preparing" -> Color.YELLOW
            "Ready" -> Color.GREEN
            "Completed" -> Color.GRAY
            else -> Color.BLACK
        }
    }
}