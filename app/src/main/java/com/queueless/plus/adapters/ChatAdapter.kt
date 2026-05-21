package com.queueless.plus.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.queueless.plus.databinding.ItemChatMessageBinding
import com.queueless.plus.models.ChatMessage

class ChatAdapter(private val currentUserId: String) :
    ListAdapter<ChatMessage, ChatAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.tvMessage.text = message.message
            binding.tvSender.text = message.senderName
            binding.tvTime.text = formatTime(message.timestamp)

            // Align message based on sender
            val params = binding.root.layoutParams as ViewGroup.MarginLayoutParams
            if (message.senderId == currentUserId) {
                // Right align for current user
                params.marginStart = 100
                params.marginEnd = 16
                binding.cardMessage.setCardBackgroundColor(
                    binding.root.context.getColor(android.R.color.holo_blue_light)
                )
            } else {
                // Left align for others
                params.marginStart = 16
                params.marginEnd = 100
                binding.cardMessage.setCardBackgroundColor(
                    binding.root.context.getColor(android.R.color.white)
                )
            }
            binding.root.layoutParams = params
        }

        private fun formatTime(timestamp: Long): String {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}