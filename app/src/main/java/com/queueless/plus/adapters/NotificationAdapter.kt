package com.queueless.plus.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.queueless.plus.databinding.ItemNotificationBinding
import com.queueless.plus.models.AppNotification

class NotificationAdapter(
    private var items: List<AppNotification>,
    private val onTap: (AppNotification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvTitle.text = item.title
        holder.binding.tvMessage.text = item.message
        holder.binding.tvTime.text = java.text.SimpleDateFormat(
            "dd MMM, hh:mm a",
            java.util.Locale.getDefault()
        ).format(java.util.Date(item.timestamp))
        holder.binding.root.alpha = if (item.read) 0.7f else 1f
        holder.binding.root.setOnClickListener { onTap(item) }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<AppNotification>) {
        items = newItems
        notifyDataSetChanged()
    }
}
