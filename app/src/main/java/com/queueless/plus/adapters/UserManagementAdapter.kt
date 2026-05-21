package com.queueless.plus.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.queueless.plus.databinding.ItemUserBinding
import com.queueless.plus.models.User

class UserManagementAdapter(
    private var users: MutableList<User>,
    private val currentUserId: String,
    private val onToggleRole: (User) -> Unit
) : RecyclerView.Adapter<UserManagementAdapter.UserViewHolder>() {

    inner class UserViewHolder(val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        val b = holder.binding

        // Avatar initial
        b.tvInitial.text = user.name.firstOrNull()?.uppercase() ?: "U"

        // Name & email
        b.tvUserName.text = user.name
        b.tvUserEmail.text = user.email

        val isAdmin = user.role == "admin"
        val isSelf  = user.userId == currentUserId

        // Role chip
        b.chipRole.text = if (isAdmin) "🛡️ Admin" else " User"
        b.chipRole.setChipBackgroundColorResource(
            if (isAdmin) com.queueless.plus.R.color.colorPrimary
            else         com.queueless.plus.R.color.colorBackground
        )
        b.chipRole.setTextColor(
            if (isAdmin) Color.WHITE else Color.GRAY
        )

        // Avatar background changes for admins
        b.tvInitial.setBackgroundColor(Color.TRANSPARENT)

        // Toggle button
        if (isSelf) {
            // Cannot change own role
            b.btnToggleRole.text = "You"
            b.btnToggleRole.isEnabled = false
            b.btnToggleRole.alpha = 0.4f
        } else {
            b.btnToggleRole.isEnabled = true
            b.btnToggleRole.alpha = 1.0f
            if (isAdmin) {
                b.btnToggleRole.text = "Remove Admin"
                b.btnToggleRole.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#D32F2F"))
            } else {
                b.btnToggleRole.text = "Make Admin"
                b.btnToggleRole.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#1565C0"))
            }
            b.btnToggleRole.setOnClickListener { onToggleRole(user) }
        }
    }

    override fun getItemCount() = users.size

    fun updateList(newList: List<User>) {
        users.clear()
        users.addAll(newList)
        notifyDataSetChanged()
    }
}
