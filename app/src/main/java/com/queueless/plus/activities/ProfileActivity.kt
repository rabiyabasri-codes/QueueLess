package com.queueless.plus.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.queueless.plus.databinding.ActivityProfileBinding
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.ThemeUtils
import com.queueless.plus.utils.toast
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        ThemeUtils.applyTheme(this, session)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Profile"

        binding.etName.setText(session.userName)
        binding.tvEmail.text = "User ID: ${session.userId}"
        binding.tvRole.text = "Role: ${session.userRole}"
        binding.switchDarkMode.isChecked = session.isDarkMode

        binding.switchDarkMode.setOnCheckedChangeListener { _, checked ->
            session.isDarkMode = checked
            recreate()
        }

        binding.btnSaveProfile.setOnClickListener { saveProfile() }
        binding.btnOpenNotificationCenter.setOnClickListener {
            startActivity(Intent(this, NotificationCenterActivity::class.java))
        }
        binding.btnOpenFavoritesRecent.setOnClickListener {
            startActivity(Intent(this, FavoritesRecentActivity::class.java))
        }
    }

    private fun saveProfile() {
        val updatedName = binding.etName.text?.toString()?.trim().orEmpty()
        if (updatedName.isEmpty()) {
            toast("Name cannot be empty")
            return
        }
        lifecycleScope.launch {
            try {
                FirestoreRepository.updateUserName(session.userId, updatedName)
                session.userName = updatedName
                toast("Profile updated")
            } catch (e: Exception) {
                toast("Failed to update profile: ${e.message}")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
