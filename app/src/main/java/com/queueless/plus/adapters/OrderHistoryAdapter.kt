package com.queueless.plus.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.queueless.plus.databinding.ItemOrderHistoryBinding
import com.queueless.plus.models.Order

class OrderHistoryAdapter(
    private val list: List<Order>
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
        holder.binding.tvTotal.text = "₹${order.total}"
    }

    override fun getItemCount() = list.size
}