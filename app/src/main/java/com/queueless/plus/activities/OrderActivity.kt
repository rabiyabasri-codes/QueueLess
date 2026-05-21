package com.queueless.plus.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.queueless.plus.R
import com.queueless.plus.databinding.ActivityOrderBinding
import com.queueless.plus.models.CartItem
import com.queueless.plus.models.MenuItem
import com.queueless.plus.models.Order
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.toast
import kotlinx.coroutines.launch

class OrderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IS_EDIT_MODE = "extra_is_edit_mode"
        const val EXTRA_ORDER_ID    = "extra_order_id"
        const val EXTRA_ENTRY_ID    = "ENTRY_ID"
        const val EXTRA_QUEUE_ID    = "QUEUE_ID"
    }

    private lateinit var binding: ActivityOrderBinding
    private lateinit var session: SessionManager

    private var entryId: String = ""
    private var queueId: String = ""
    private var isEditMode: Boolean = false
    private var existingOrderId: String = ""

    private val cart = mutableListOf<CartItem>()
    private var allMenuItems: List<MenuItem> = emptyList()
    private var filteredMenuItems: List<MenuItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        entryId         = intent.getStringExtra(EXTRA_ENTRY_ID) ?: ""
        queueId         = intent.getStringExtra(EXTRA_QUEUE_ID) ?: ""
        isEditMode      = intent.getBooleanExtra(EXTRA_IS_EDIT_MODE, false)
        existingOrderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""

        setupToolbar()
        setupSearch()
        setupPaymentSelection()
        loadMenu()
        updateCartView()
        updateTotal()

        // Pre-load cart for edit mode
        val existingItems = intent.getStringExtra("EXISTING_ITEMS") ?: ""
        if (isEditMode && existingItems.isNotEmpty()) {
            preloadCart(existingItems)
        }

        binding.btnPlaceOrder.text = if (isEditMode) "Update Order" else "Place Order"
        binding.btnPlaceOrder.setOnClickListener {
            if (isEditMode) updateOrder() else placeOrder()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) "Update Order" else "Order Food"
    }

    private fun setupSearch() {
        binding.etMenuSearch.doAfterTextChanged { text ->
            filterAndRenderMenu(text?.toString().orEmpty())
        }
    }

    private fun loadMenu() {
        FirestoreRepository.listenToMenu { items ->
            allMenuItems = items.sortedWith(
                compareByDescending<MenuItem> { session.isFavoriteItem(it.name) }
                    .thenBy { it.name.lowercase() }
            )
            filterAndRenderMenu(binding.etMenuSearch.text?.toString().orEmpty())
        }
    }

    private fun filterAndRenderMenu(query: String) {
        filteredMenuItems = if (query.isBlank()) allMenuItems
        else allMenuItems.filter { it.name.contains(query, ignoreCase = true) }
        renderMenuItems()
    }

    /** Renders menu items as simple card-like rows inside llMenuItems LinearLayout */
    private fun renderMenuItems() {
        val container = binding.llMenuItems
        container.removeAllViews()

        for (item in filteredMenuItems) {
            val row = layoutInflater.inflate(R.layout.item_menu_row, container, false)
            row.findViewById<TextView>(R.id.tvMenuItemName).text = item.name
            row.findViewById<TextView>(R.id.tvMenuItemPrice).text = "₹${item.price}"
            row.findViewById<MaterialButton>(R.id.btnAddItem).setOnClickListener {
                addToCart(item)
            }
            container.addView(row)
        }
    }

    /** Renders cart rows inside llCartItems LinearLayout */
    private fun updateCartView() {
        val container = binding.llCartItems
        container.removeAllViews()

        if (cart.isEmpty()) {
            binding.tvCartEmpty.visibility = View.VISIBLE
        } else {
            binding.tvCartEmpty.visibility = View.GONE
            for ((index, item) in cart.withIndex()) {
                val row = layoutInflater.inflate(R.layout.item_cart_row, container, false)
                row.findViewById<TextView>(R.id.tvCartItemName).text = item.name
                row.findViewById<TextView>(R.id.tvCartItemQty).text = item.quantity.toString()
                row.findViewById<TextView>(R.id.tvCartItemPrice).text =
                    if (item.price > 0) "₹${item.price * item.quantity}" else ""
                row.findViewById<MaterialButton>(R.id.btnCartMinus).setOnClickListener {
                    if (item.quantity > 1) {
                        cart[index].quantity--
                    } else {
                        cart.removeAt(index)
                    }
                    updateCartView()
                    updateTotal()
                }
                row.findViewById<MaterialButton>(R.id.btnCartPlus).setOnClickListener {
                    cart[index].quantity++
                    updateCartView()
                    updateTotal()
                }
                container.addView(row)
            }
        }
        updateTotal()
    }

    private fun addToCart(item: MenuItem) {
        val existing = cart.find { it.name == item.name }
        if (existing != null) existing.quantity++
        else cart.add(CartItem(item.name, item.price, 1))
        updateCartView()
        updateTotal()
        toast("${item.name} added ✓")
    }

    private fun updateTotal() {
        val total = cart.sumOf { it.price * it.quantity }
        binding.tvTotal.text = "Total: ₹$total"
    }

    private fun setupPaymentSelection() {
        binding.paymentMethodGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbUPI -> {
                    startActivity(
                        Intent(this, PaymentActivity::class.java)
                            .putExtra(PaymentActivity.EXTRA_PAYMENT_TYPE, PaymentActivity.TYPE_UPI)
                            .putExtra(PaymentActivity.EXTRA_TOTAL, cart.sumOf { it.price * it.quantity })
                    )
                }
                R.id.rbCard -> {
                    startActivity(
                        Intent(this, PaymentActivity::class.java)
                            .putExtra(PaymentActivity.EXTRA_PAYMENT_TYPE, PaymentActivity.TYPE_CARD)
                            .putExtra(PaymentActivity.EXTRA_TOTAL, cart.sumOf { it.price * it.quantity })
                    )
                }
            }
        }
    }

    private fun preloadCart(itemsText: String) {
        itemsText.lines().forEach { line ->
            val parts = line.split(" x")
            if (parts.size == 2) {
                val name = parts[0].trim()
                val qty  = parts[1].trim().toIntOrNull() ?: 1
                cart.add(CartItem(name, 0, qty))
            }
        }
        updateCartView()
        updateTotal()
    }

    private fun placeOrder() {
        if (cart.isEmpty()) { toast("Cart is empty"); return }
        if (entryId.isEmpty()) { toast("Try again in a moment"); return }

        binding.btnPlaceOrder.isEnabled = false
        val total = cart.sumOf { it.price * it.quantity }
        val orderText = cart.joinToString("\n") { "${it.name} x${it.quantity}" }
        val selectedPaymentMethod = getSelectedPaymentMethod()
        val detailedOrderText = "$orderText\nPayment: $selectedPaymentMethod"

        lifecycleScope.launch {
            try {
                val order = Order(
                    userId = session.userId,
                    queueId = queueId,
                    items = orderText,
                    total = total,
                    paymentMethod = selectedPaymentMethod,
                    timestamp = System.currentTimeMillis()
                )
                val orderId = FirestoreRepository.createOrder(order)
                FirestoreRepository.updateOrderDetails(entryId, detailedOrderText)
                FirestoreRepository.attachOrderToEntry(entryId, orderId)
                FirestoreRepository.addUserPoints(session.userId, 10)
                session.addRecentOrder(orderText, total)
                FirestoreRepository.pushNotification(
                    userId = session.userId,
                    title = "Order placed",
                    message = "Your order for ₹$total has been placed."
                )
                toast("Order placed ₹$total")
                val intent = Intent(this@OrderActivity, UserStatusActivity::class.java)
                intent.putExtra(UserStatusActivity.EXTRA_QUEUE_ID, queueId)
                intent.putExtra(UserStatusActivity.EXTRA_ENTRY_ID, entryId)
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                toast("Error: ${e.message}")
                binding.btnPlaceOrder.isEnabled = true
            }
        }
    }

    private fun updateOrder() {
        if (cart.isEmpty()) { toast("Cart is empty"); return }
        if (existingOrderId.isEmpty()) { toast("No existing order found"); return }

        binding.btnPlaceOrder.isEnabled = false
        val total = cart.sumOf { it.price * it.quantity }
        val orderText = cart.joinToString("\n") { "${it.name} x${it.quantity}" }
        val selectedPaymentMethod = getSelectedPaymentMethod()
        val detailedOrderText = "$orderText\nPayment: $selectedPaymentMethod"

        lifecycleScope.launch {
            try {
                val updatedOrder = Order(
                    orderId = existingOrderId,
                    userId = session.userId,
                    queueId = queueId,
                    items = orderText,
                    total = total,
                    paymentMethod = selectedPaymentMethod,
                    status = "Placed",
                    timestamp = System.currentTimeMillis()
                )
                FirestoreRepository.updateOrder(existingOrderId, updatedOrder)
                FirestoreRepository.updateOrderDetails(entryId, detailedOrderText)
                FirestoreRepository.pushNotification(
                    userId = session.userId,
                    title = "Order updated",
                    message = "Your order has been updated. Total: ₹$total"
                )
                toast("Order updated! ₹$total")
                finish()
            } catch (e: Exception) {
                toast("Error: ${e.message}")
                binding.btnPlaceOrder.isEnabled = true
            }
        }
    }

    private fun getSelectedPaymentMethod(): String {
        return try {
            val radioButton = binding.paymentMethodGroup.findViewById<android.widget.RadioButton>(
                binding.paymentMethodGroup.checkedRadioButtonId
            )
            radioButton?.text?.toString().orEmpty().ifBlank { "Cash on Delivery" }
        } catch (t: Throwable) {
            "Cash on Delivery"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}