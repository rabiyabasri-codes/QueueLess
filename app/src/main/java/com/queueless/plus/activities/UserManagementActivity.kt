package com.queueless.plus.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.queueless.plus.adapters.UserManagementAdapter
import com.queueless.plus.databinding.ActivityUserManagementBinding
import com.queueless.plus.models.User
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.requireAdminAccess
import com.queueless.plus.utils.toast
import kotlinx.coroutines.launch

class UserManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserManagementBinding
    private lateinit var session: SessionManager
    private lateinit var adapter: UserManagementAdapter
    private val userList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        if (!requireAdminAccess(session)) return

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = UserManagementAdapter(
            users = userList,
            currentUserId = session.userId,
            onToggleRole = { user -> confirmRoleChange(user) }
        )

        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter

        loadUsers()
    }

    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvUsers.visibility    = View.GONE
        binding.tvEmpty.visibility    = View.GONE

        lifecycleScope.launch {
            try {
                val users = FirestoreRepository.getAllUsers()
                binding.progressBar.visibility = View.GONE

                if (users.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    binding.rvUsers.visibility = View.VISIBLE
                    adapter.updateList(users)
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                toast("Failed to load users: ${e.message}")
            }
        }
    }

    private fun confirmRoleChange(user: User) {
        val isAdmin   = user.role == "admin"
        val newRole   = if (isAdmin) "user" else "admin"
        val action    = if (isAdmin) "Remove Admin from" else "Make Admin"
        val emoji     = if (isAdmin) "⬇️" else "⬆️"

        AlertDialog.Builder(this)
            .setTitle("$emoji $action ${user.name}?")
            .setMessage(
                if (isAdmin)
                    "${user.name} will lose admin access and become a regular user."
                else
                    "${user.name} will gain full admin access including queue management, order management and user management."
            )
            .setPositiveButton(if (isAdmin) "Remove Admin" else "Make Admin") { _, _ ->
                applyRoleChange(user, newRole)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyRoleChange(user: User, newRole: String) {
        lifecycleScope.launch {
            try {
                FirestoreRepository.updateUserRole(user.userId, newRole)
                toast(
                    if (newRole == "admin")
                        " ${user.name} is now an Admin!"
                    else
                        " ${user.name} is now a regular User"
                )
                // Reload list to reflect change
                loadUsers()
            } catch (e: Exception) {
                toast("Failed to update role: ${e.message}")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
