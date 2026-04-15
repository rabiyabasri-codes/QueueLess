package com.queueless.plus.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.queueless.plus.databinding.ActivityCreateQueueBinding
import com.queueless.plus.models.Queue
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.hide
import com.queueless.plus.utils.show
import com.queueless.plus.utils.toast
import kotlinx.coroutines.launch

class CreateQueueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateQueueBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Create Queue"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnCreateQueue.setOnClickListener { createQueue() }
    }

    private fun createQueue() {
        val name        = binding.etQueueName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val location    = binding.etLocation.text.toString().trim()
        val serviceTime = binding.etServiceTime.text.toString().toIntOrNull()

        when {
            name.isEmpty()        -> { binding.etQueueName.error = "Queue name is required"; return }
            serviceTime == null   -> { binding.etServiceTime.error = "Enter a valid number"; return }
            serviceTime <= 0      -> { binding.etServiceTime.error = "Must be greater than 0"; return }
        }

        binding.progressBar.show()
        binding.btnCreateQueue.isEnabled = false

        lifecycleScope.launch {
            try {
                val queue = Queue(
                    queueName      = name,
                    description    = description,
                    location       = location,
                    avgServiceTime = serviceTime!!,
                    createdBy      = session.userId,
                    isActive       = true
                )
                FirestoreRepository.createQueue(queue)
                toast("Queue '$name' created successfully!")
                finish()
            } catch (e: Exception) {
                toast("Failed to create queue: ${e.message}")
            } finally {
                binding.progressBar.hide()
                binding.btnCreateQueue.isEnabled = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }
}
