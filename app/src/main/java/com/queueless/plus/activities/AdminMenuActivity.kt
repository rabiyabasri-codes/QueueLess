package com.queueless.plus.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.queueless.plus.adapters.AdminMenuAdapter
import com.queueless.plus.databinding.ActivityAdminMenuBinding
import com.queueless.plus.models.MenuItem
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.toast
import kotlinx.coroutines.launch

class AdminMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminMenuBinding
    private lateinit var session: SessionManager
    private lateinit var adapter: AdminMenuAdapter
    private var currentMenu: List<MenuItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        if (!session.isAdmin) {
            toast("Admin access required")
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Menu"

        setupRecycler()
        listenMenu()

        binding.btnAdd.setOnClickListener {
            addItem()
        }

        binding.btnLoadSamples.setOnClickListener {
            seedSampleMenu()
        }
    }

    private fun setupRecycler() {
        adapter = AdminMenuAdapter(emptyList()) { item ->
            deleteItem(item)
        }

        binding.rvMenu.layoutManager = LinearLayoutManager(this)
        binding.rvMenu.adapter = adapter
    }

    private var menuListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var hasAutoSeeded = false

    private fun listenMenu() {
        menuListener = FirestoreRepository.listenToMenu { items ->
            currentMenu = items
            adapter.updateData(items)

            // Show empty state hint
            binding.tvEmpty.visibility = if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

            // Auto-seed sample items the very first time the menu is empty
            if (items.isEmpty() && !hasAutoSeeded) {
                hasAutoSeeded = true
                seedSampleMenu()
            }
        }
    }

    // ADD ITEM
    private fun addItem() {

        val name = binding.etName.text?.toString()?.trim() ?: ""
        val price = binding.etPrice.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val image = binding.etImage.text?.toString()?.trim() ?: ""

        if (name.isEmpty() || price <= 0 || image.isEmpty()) {
            toast("Fill all fields correctly")
            return
        }

        lifecycleScope.launch {
            FirestoreRepository.addMenuItem(
                MenuItem(name = name, price = price, imageUrl = image)
            )
            toast("Item added")

            binding.etName.text?.clear()
            binding.etPrice.text?.clear()
            binding.etImage.text?.clear()
        }
    }

    // DELETE ITEM
    private fun deleteItem(item: MenuItem) {
        lifecycleScope.launch {
            FirestoreRepository.deleteMenuItem(item.id)
            toast("Item deleted")
        }
    }

    // ONLY 4 ITEMS
    private fun seedSampleMenu() {

        val existingNames = currentMenu.map { it.name.trim().lowercase() }.toSet()

        val samples = listOf(
            MenuItem(name = "Burger", price = 120, imageUrl = "burger"),
            MenuItem(name = "Fries", price = 80, imageUrl = "fries"),
            MenuItem(name = "Pizza", price = 200, imageUrl = "pizza"),
            MenuItem(name = "Coke", price = 40, imageUrl = "coke")
        )

        val toAdd = samples.filterNot {
            existingNames.contains(it.name.trim().lowercase())
        }

        if (toAdd.isEmpty()) {
            toast("Menu already added")
            return
        }

        lifecycleScope.launch {
            toAdd.forEach {
                FirestoreRepository.addMenuItem(it)
            }
            toast("${toAdd.size} items added")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        menuListener?.remove()
    }
}