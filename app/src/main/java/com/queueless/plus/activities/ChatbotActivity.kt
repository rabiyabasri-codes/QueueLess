package com.queueless.plus.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.queueless.plus.R
import com.queueless.plus.adapters.ChatbotAdapter
import com.queueless.plus.databinding.ActivityChatbotBinding
import com.queueless.plus.models.ChatMessage
import com.queueless.plus.utils.SessionManager
import kotlinx.coroutines.launch

class ChatbotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatbotBinding
    private lateinit var session: SessionManager
    private lateinit var adapter: ChatbotAdapter
    private val messagesList = ArrayList<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        setupToolbar()
        setupRecyclerView()
        setupSendButton()
        setupQuickQueries()

        // Submit initial greeting message if empty
        if (messagesList.isEmpty()) {
            addGreetingMessage()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatbotAdapter(session.userId)
        binding.recyclerChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.recyclerChat.adapter = adapter
    }

    private fun setupSendButton() {
        binding.btnSend.setOnClickListener {
            val messageText = binding.etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            }
        }
    }

    private fun setupQuickQueries() {
        binding.btnQuery1.setOnClickListener {
            sendMessage("How to join a queue?")
        }
        binding.btnQuery2.setOnClickListener {
            sendMessage("How does live wait time work?")
        }
        binding.btnQuery3.setOnClickListener {
            sendMessage("Can I order food and pay?")
        }
        binding.btnQuery4.setOnClickListener {
            sendMessage("How do I scan a QR code?")
        }
        binding.btnQuery5.setOnClickListener {
            sendMessage("What are the Admin features?")
        }
    }

    private fun addGreetingMessage() {
        val userName = session.userName.ifBlank { "User" }
        val greeting = ChatMessage(
            message = "Hi $userName! 👋 Welcome to the QueueLessPlus Support Desk.\n\n" +
                    "Tap any of the frequently asked questions below for direct answers, or type any query in the input box to see our quick startup guide!",
            senderId = "assistant",
            senderName = "SupportBot",
            timestamp = System.currentTimeMillis()
        )
        messagesList.add(greeting)
        adapter.submitList(ArrayList(messagesList))
    }

    private fun sendMessage(promptText: String) {
        // Clear input field immediately
        binding.etMessage.text.clear()

        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etMessage.windowToken, 0)

        // 1. Add User Message
        val userMsg = ChatMessage(
            message = promptText,
            senderId = session.userId,
            senderName = session.userName.ifBlank { "User" },
            timestamp = System.currentTimeMillis()
        )
        messagesList.add(userMsg)
        adapter.submitList(ArrayList(messagesList)) {
            binding.recyclerChat.smoothScrollToPosition(messagesList.size - 1)
        }

        // 2. Show Typing Indicator and disable Send button
        binding.cardTypingIndicator.visibility = View.VISIBLE
        binding.btnSend.isEnabled = false

        // 3. Respond locally after a brief simulated delay (500ms) for premium UI feel
        lifecycleScope.launch {
            kotlinx.coroutines.delay(500)
            
            val responseText = getDirectAnswer(promptText)
            
            val botMsg = ChatMessage(
                message = responseText,
                senderId = "assistant",
                senderName = "SupportBot",
                timestamp = System.currentTimeMillis()
            )
            
            // Hide Typing Indicator and enable Send button
            binding.cardTypingIndicator.visibility = View.GONE
            binding.btnSend.isEnabled = true
            
            messagesList.add(botMsg)
            adapter.submitList(ArrayList(messagesList)) {
                binding.recyclerChat.smoothScrollToPosition(messagesList.size - 1)
            }
        }
    }

    private fun getDirectAnswer(query: String): String {
        val q = query.trim().lowercase()
        return when {
            q.contains("join") || q.contains("queue") && !q.contains("time") && !q.contains("scan") && !q.contains("admin") -> {
                "To join a queue:\n1. Click **Search Queues** on your dashboard.\n2. Tap a merchant (e.g., MM Foods).\n3. Click **Join Queue**.\n4. You will receive a live position token and estimated waiting time!"
            }
            q.contains("time") || q.contains("wait") || q.contains("live") -> {
                "QueueLessPlus calculates live wait times based on historical serving speeds (e.g., ~5 min per person). As the merchant serves customers, your position is updated in real-time, and you are notified when it is your turn!"
            }
            q.contains("order") || q.contains("food") || q.contains("pay") || q.contains("menu") -> {
                "Yes! Tap any merchant from the dashboard to browse their digital menu. Add items to your cart, place a food or retail order, and track its progress directly through the **Order History** tab."
            }
            q.contains("scan") || q.contains("qr") || q.contains("code") -> {
                "Tap the **QR Scanner** floating icon on the dashboard. Grant camera permissions, then point your camera at any merchant's QueueLess QR code. You'll instantly see their active queues and menus!"
            }
            q.contains("admin") || q.contains("create") || q.contains("merchant") -> {
                "Admins can:\n1. Create and customize queues.\n2. Add, remove, or edit menu items (with custom prices and photos).\n3. View live queue metrics and dashboard analytics.\n4. Accept and serve orders in real-time."
            }
            else -> {
                "**QueueLessPlus Guide** 🚀\n\nQueueLessPlus digitizes wait times to eliminate physical lines!\n\n* **Customers**: Search nearby merchants, browse digital menus, order food, and scan on-site QR codes to instantly join active queues. Get live wait updates and notifications.\n* **Admins**: Create queues, configure menus (items, prices), manage waitlists in real-time, and process incoming orders with full analytics.\n\nUse the quick query buttons above to learn more about specific app features!"
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chatbot, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_clear_chat) {
            messagesList.clear()
            addGreetingMessage()
            Toast.makeText(this, "Chat history cleared", Toast.LENGTH_SHORT).show()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}
