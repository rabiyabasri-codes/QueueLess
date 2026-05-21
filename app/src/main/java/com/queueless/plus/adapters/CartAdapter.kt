package com.queueless.plus.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.queueless.plus.databinding.ItemCartBinding
import com.queueless.plus.models.CartItem

class CartAdapter(
    private val items: MutableList<CartItem>,
    private val onUpdate: () -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {

        val item = items[position]

        holder.binding.tvName.text = item.name
        holder.binding.tvQty.text = item.quantity.toString()
        holder.binding.tvPrice.text = "₹${item.price * item.quantity}"

        holder.binding.btnPlus.setOnClickListener {
            val currentPos = holder.adapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                items[currentPos].quantity++
                notifyDataSetChanged()   // SAFE FIX
                onUpdate()
            }
        }

        holder.binding.btnMinus.setOnClickListener {
            val currentPos = holder.adapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                if (items[currentPos].quantity > 1) {
                    items[currentPos].quantity--
                    notifyDataSetChanged()   // SAFE FIX
                    onUpdate()
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}