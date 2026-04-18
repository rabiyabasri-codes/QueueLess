package com.queueless.plus.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.queueless.plus.databinding.ItemManageEntryBinding
import com.queueless.plus.models.QueueEntry

class ManageEntryAdapter(
    private val onServed: (QueueEntry) -> Unit,
    private val onRemove: (QueueEntry) -> Unit,
    private val onPreparing: (QueueEntry) -> Unit,
    private val onReady: (QueueEntry) -> Unit
) : ListAdapter<QueueEntry, ManageEntryAdapter.ManageEntryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageEntryViewHolder {
        val binding = ItemManageEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ManageEntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ManageEntryViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class ManageEntryViewHolder(
        private val binding: ItemManageEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: QueueEntry, position: Int) {

            // 🟢 Position + Name
            binding.tvPosition.text = "#$position"
            binding.tvUserName.text = entry.userName

            // 🍔 Order Details
            val orderText = if (entry.orderDetails.isBlank()) {
                "Not placed"
            } else entry.orderDetails

            binding.tvOrder.text = "Order: $orderText"

            // 🔥 Order Status (formatted)
            val statusText = entry.orderStatus.replaceFirstChar { it.uppercase() }
            binding.tvStatus.text = "Status: $statusText"

            // 🟢 Queue Actions
            binding.btnServed.setOnClickListener { onServed(entry) }
            binding.btnRemove.setOnClickListener { onRemove(entry) }

            // 🔥 Order Actions
            binding.btnPreparing.setOnClickListener { onPreparing(entry) }
            binding.btnReady.setOnClickListener { onReady(entry) }

            // 🚀 SMART UI CONTROL (IMPORTANT)
            when (entry.orderStatus) {

                QueueEntry.ORDER_WAITING -> {
                    binding.btnPreparing.isEnabled = true
                    binding.btnReady.isEnabled = false
                }

                QueueEntry.ORDER_PREPARING -> {
                    binding.btnPreparing.isEnabled = false
                    binding.btnReady.isEnabled = true
                }

                QueueEntry.ORDER_READY -> {
                    binding.btnPreparing.isEnabled = false
                    binding.btnReady.isEnabled = false
                }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<QueueEntry>() {

        override fun areItemsTheSame(old: QueueEntry, new: QueueEntry): Boolean {
            return old.entryId == new.entryId
        }

        override fun areContentsTheSame(old: QueueEntry, new: QueueEntry): Boolean {
            return old == new
        }
    }
}