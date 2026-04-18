package com.queueless.plus.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.queueless.plus.adapters.AdminMenuAdapter
import com.queueless.plus.databinding.ActivityAdminMenuBinding
import com.queueless.plus.models.MenuItem
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.toast
import kotlinx.coroutines.launch

class AdminMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminMenuBinding
    private lateinit var adapter: AdminMenuAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecycler()
        listenMenu()

        binding.btnAdd.setOnClickListener {
            addItem()
        }
    }

    private fun setupRecycler() {
        adapter = AdminMenuAdapter(emptyList()) { item ->
            deleteItem(item)
        }

        binding.rvMenu.layoutManager = LinearLayoutManager(this)
        binding.rvMenu.adapter = adapter
    }

    private fun listenMenu() {
        FirestoreRepository.listenToMenu {
            adapter.updateData(it)
        }
    }

    private fun addItem() {

        val name = binding.etName.text.toString().trim()
        val price = binding.etPrice.text.toString().toIntOrNull()
        val image = binding.etImage.text.toString().trim()

        if (name.isEmpty() || price == null || image.isEmpty()) {
            toast("Fill all fields")
            return
        }

        lifecycleScope.launch {
            FirestoreRepository.addMenuItem(
                MenuItem(name = name, price = price, imageUrl = image)
            )
            toast("Item added")

            binding.etName.text.clear()
            binding.etPrice.text.clear()
            binding.etImage.text.clear()
        }
    }

    private fun deleteItem(item: MenuItem) {
        lifecycleScope.launch {
            FirestoreRepository.deleteMenuItem(item.id)
            toast("Item deleted")
        }
    }
}