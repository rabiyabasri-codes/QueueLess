package com.queueless.plus.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.queueless.plus.databinding.ItemMenuBinding
import com.queueless.plus.models.MenuItem

class MenuAdapter(
    private var items: List<MenuItem>,
    private val onAdd: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    inner class MenuViewHolder(val binding: ItemMenuBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMenuBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {

        val item = items[position]

        // 🍔 Name & Price
        holder.binding.tvName.text = item.name
        holder.binding.tvPrice.text = "₹${item.price}"

        // 🖼️ Image from drawable using Firebase string
        val context = holder.itemView.context

        val imageResId = context.resources.getIdentifier(
            item.imageUrl,   // e.g. "burger"
            "drawable",
            context.packageName
        )

        if (imageResId != 0) {
            holder.binding.ivFood.setImageResource(imageResId)
        } else {
            // fallback image
            holder.binding.ivFood.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // ➕ Add button
        holder.binding.btnAdd.setOnClickListener {
            onAdd(item)
        }
    }

    override fun getItemCount(): Int = items.size

    // 🔥 Update list dynamically from Firebase
    fun updateData(newList: List<MenuItem>) {
        items = newList
        notifyDataSetChanged()
    }
}