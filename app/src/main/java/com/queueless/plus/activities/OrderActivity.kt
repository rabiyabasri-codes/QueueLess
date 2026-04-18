package com.queueless.plus.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.queueless.plus.adapters.CartAdapter
import com.queueless.plus.adapters.MenuAdapter
import com.queueless.plus.databinding.ActivityOrderBinding
import com.queueless.plus.models.CartItem
import com.queueless.plus.models.MenuItem
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.toast

class OrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    private var entryId: String = ""
    private var queueId: String = ""

    private val cart = mutableListOf<CartItem>()
    private lateinit var cartAdapter: CartAdapter
    private lateinit var menuAdapter: MenuAdapter   // 🔥 NEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        entryId = intent.getStringExtra("ENTRY_ID") ?: ""
        queueId = intent.getStringExtra("QUEUE_ID") ?: ""

        if (entryId.isEmpty()) {
            toast("Entry not ready yet ⏳")
        }

        setupToolbar()
        setupMenu()        // 🔥 Firebase menu
        setupCart()
        setupSwipeToDelete()

        updateTotal()

        binding.btnPlaceOrder.setOnClickListener {
            placeOrder()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Order Food 🍔"
    }

    // 🔥 UPDATED: Firebase menu
    private fun setupMenu() {

        binding.rvMenu.layoutManager = LinearLayoutManager(this)

        menuAdapter = MenuAdapter(emptyList()) { item ->
            addToCart(item)
        }

        binding.rvMenu.adapter = menuAdapter

        // 🔥 LISTEN TO FIREBASE MENU
        FirestoreRepository.listenToMenu { list ->
            menuAdapter.updateData(list)
        }
    }

    private fun setupCart() {
        binding.rvCart.layoutManager = LinearLayoutManager(this)

        cartAdapter = CartAdapter(cart) {
            updateTotal()
        }

        binding.rvCart.adapter = cartAdapter
    }

    private fun addToCart(item: MenuItem) {

        val existing = cart.find { it.name == item.name }

        if (existing != null) {
            existing.quantity++
        } else {
            cart.add(CartItem(item.name, item.price, 1))
        }

        cartAdapter.notifyDataSetChanged()
        updateTotal()
    }

    private fun updateTotal() {
        val total = cart.sumOf { it.price * it.quantity }
        binding.tvTotal.text = "Total: ₹$total"
    }

    private fun setupSwipeToDelete() {

        val swipe = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                val position = viewHolder.adapterPosition

                if (position >= 0 && position < cart.size) {
                    cart.removeAt(position)
                    cartAdapter.notifyItemRemoved(position)
                    updateTotal()
                }
            }
        }

        ItemTouchHelper(swipe).attachToRecyclerView(binding.rvCart)
    }

    private fun placeOrder() {

        if (cart.isEmpty()) {
            toast("Cart is empty")
            return
        }

        if (entryId.isEmpty()) {
            toast("Try again in a moment ⏳")
            return
        }

        binding.btnPlaceOrder.isEnabled = false

        val total = cart.sumOf { it.price * it.quantity }

        val orderText = cart.joinToString("\n") {
            "${it.name} x${it.quantity}"
        }

        db.collection("queueEntries")
            .document(entryId)
            .update(
                mapOf(
                    "orderDetails" to orderText,
                    "orderStatus" to "waiting"
                )
            )
            .addOnSuccessListener {

                val orderMap = hashMapOf(
                    "userId" to session.userId,
                    "items" to orderText,
                    "total" to total,
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("orders").add(orderMap)

                toast("Order placed ₹$total")

                val intent = Intent(this, UserStatusActivity::class.java)
                intent.putExtra(UserStatusActivity.EXTRA_QUEUE_ID, queueId)
                startActivity(intent)

                finish()
            }
            .addOnFailureListener {
                toast("Error: ${it.message}")
                binding.btnPlaceOrder.isEnabled = true
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}