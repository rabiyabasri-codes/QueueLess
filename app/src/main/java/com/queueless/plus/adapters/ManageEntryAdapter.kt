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
    private val onRemove: (QueueEntry) -> Unit
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
            binding.tvPosition.text = "#$position"
            binding.tvUserName.text = entry.userName
            binding.btnServed.setOnClickListener { onServed(entry) }
            binding.btnRemove.setOnClickListener { onRemove(entry) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<QueueEntry>() {
        override fun areItemsTheSame(old: QueueEntry, new: QueueEntry) = old.entryId == new.entryId
        override fun areContentsTheSame(old: QueueEntry, new: QueueEntry) = old == new
    }
}
