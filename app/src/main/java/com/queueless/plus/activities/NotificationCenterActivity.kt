package com.queueless.plus.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.queueless.plus.adapters.NotificationAdapter
import com.queueless.plus.databinding.ActivityNotificationCenterBinding
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.SessionManager
import kotlinx.coroutines.launch

class NotificationCenterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationCenterBinding
    private lateinit var session: SessionManager
    private lateinit var adapter: NotificationAdapter
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationCenterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Notifications"

        adapter = NotificationAdapter(emptyList()) { notification ->
            if (!notification.read) {
                lifecycleScope.launch {
                    FirestoreRepository.markNotificationRead(notification.id)
                }
            }
        }
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        listener = FirestoreRepository.listenToNotifications(session.userId) { notifications ->
            adapter.update(notifications)
            binding.tvEmpty.visibility =
                if (notifications.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
