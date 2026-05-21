package com.queueless.plus.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.queueless.plus.databinding.ItemAdminMenuBinding
import com.queueless.plus.models.MenuItem

class AdminMenuAdapter(
    private var items: List<MenuItem>,
    private val onDelete: (MenuItem) -> Unit
) : RecyclerView.Adapter<AdminMenuAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAdminMenuBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminMenuBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = items[position]

        holder.binding.tvName.text = item.name
        holder.binding.tvPrice.text = "₹${item.price}"

        holder.binding.btnDelete.setOnClickListener {
            onDelete(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newList: List<MenuItem>) {
        items = newList
        notifyDataSetChanged()
    }
}