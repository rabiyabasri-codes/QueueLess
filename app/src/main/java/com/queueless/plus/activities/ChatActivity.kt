package com.queueless.plus.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.queueless.plus.adapters.ChatAdapter
import com.queueless.plus.databinding.ActivityChatBinding
import com.queueless.plus.models.ChatMessage
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.HuggingFaceClient
import com.queueless.plus.utils.SessionManager
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var session: SessionManager
    private lateinit var adapter: ChatAdapter
    private var chatListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        setupRecyclerView()
        setupSendButton()
        listenToMessages()
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(session.userId)
        binding.recyclerChat.adapter = adapter
        binding.recyclerChat.layoutManager = LinearLayoutManager(this)
    }

    private fun setupSendButton() {
        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                binding.etMessage.text.clear()
            }
        }
    }

    private fun sendMessage(message: String) {
        val userId = session.userId
        val chatMessage = ChatMessage(
            message = message,
            senderId = userId,
            senderName = session.userName,
            timestamp = System.currentTimeMillis()
        )
        FirestoreRepository.sendChatMessage(chatMessage) { success ->
            if (!success) {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            } else {
                generateAssistantResponse(message)
            }
        }
    }

    private fun generateAssistantResponse(prompt: String) {
        lifecycleScope.launch {
            try {
                val assistantText = HuggingFaceClient.getAssistantResponse(prompt)
                val assistantMessage = ChatMessage(
                    message = assistantText,
                    senderId = "assistant",
                    senderName = "QueueLessBot",
                    timestamp = System.currentTimeMillis()
                )
                FirestoreRepository.sendChatMessage(assistantMessage) { success ->
                    if (!success) {
                        Toast.makeText(
                            this@ChatActivity,
                            "Failed to save assistant response",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (exception: Exception) {
                Toast.makeText(
                    this@ChatActivity,
                    "AI assistant unavailable. Please try again later.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun listenToMessages() {
        chatListener = FirestoreRepository.listenToChatMessages { messages: List<ChatMessage> ->
            adapter.submitList(messages)
            if (messages.isNotEmpty()) {
                binding.recyclerChat.scrollToPosition(messages.size - 1)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatListener?.remove()
    }
}