package com.queueless.plus.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.lifecycle.lifecycleScope
import com.queueless.plus.databinding.ActivityQrScanBinding
import com.queueless.plus.models.QueueEntry
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.requireAdminAccess
import com.queueless.plus.utils.toast
import kotlinx.coroutines.launch

class QRScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScanBinding
    private lateinit var session: SessionManager
    private val db = FirebaseFirestore.getInstance()

    private var scannedEntryId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)
        if (!requireAdminAccess(session)) return

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnScan.setOnClickListener { startScan() }

        binding.btnSetPreparing.setOnClickListener {
            updateOrderStatus(QueueEntry.ORDER_PREPARING, "Preparing 🍳")
        }

        binding.btnSetReady.setOnClickListener {
            updateOrderStatus(QueueEntry.ORDER_READY, "Ready")
        }

        binding.btnMarkReady.setOnClickListener {
            updateOrderStatus(QueueEntry.ORDER_COMPLETED, "Completed & Received ✔")
        }
    }

    // QR SCANNER
    private val scanner = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            scannedEntryId = result.contents
            fetchUserDetails(scannedEntryId)
        } else {
            toast("Scan cancelled")
        }
    }

    private fun startScan() {
        val options = ScanOptions()
        options.setPrompt("Scan User QR Code")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        scanner.launch(options)
    }

    // FETCH RICH USER DATA
    private fun fetchUserDetails(entryId: String) {
        db.collection("queueEntries")
            .document(entryId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    toast("❌ Invalid QR — entry not found")
                    return@addOnSuccessListener
                }

                val name    = doc.getString("userName") ?: "Unknown"
                val order   = doc.getString("orderDetails") ?: "No order placed"
                val status  = doc.getString("orderStatus") ?: QueueEntry.ORDER_WAITING
                val payment = try {
                    // payment stored in order details line
                    val lines = order.lines()
                    lines.firstOrNull { it.startsWith("Payment:") }?.removePrefix("Payment:")?.trim()
                        ?: "Cash on Delivery"
                } catch (e: Exception) { "Cash on Delivery" }

                // Show order items (exclude Payment line)
                val itemsOnly = order.lines()
                    .filter { !it.startsWith("Payment:") }
                    .joinToString("\n")

                binding.tvUserName.text    = name
                binding.tvOrderItems.text  = itemsOnly.ifBlank { "No items" }
                binding.tvPayment.text     = payment
                binding.tvOrderStatus.text = status.replaceFirstChar { it.uppercase() }
                binding.cardResult.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                toast("Error: ${it.message}")
            }
    }

    // UPDATE ORDER STATUS
    private fun updateOrderStatus(status: String, label: String) {
        if (scannedEntryId.isEmpty()) {
            toast("Scan a QR code first")
            return
        }

        lifecycleScope.launch {
            try {
                FirestoreRepository.updateQueueEntryOrderStatus(scannedEntryId, status)

                // Push notification to user if completed
                if (status == QueueEntry.ORDER_COMPLETED) {
                    db.collection("queueEntries").document(scannedEntryId)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val userId = snapshot.getString("userId") ?: return@addOnSuccessListener
                            lifecycleScope.launch {
                                FirestoreRepository.pushNotification(
                                    userId = userId,
                                    title = "Order Completed",
                                    message = "Your order has been completed and received!"
                                )
                            }
                        }
                }

                binding.tvOrderStatus.text = status.replaceFirstChar { it.uppercase() }
                toast("Status updated to $label")
            } catch (e: Exception) {
                toast("Failed: ${e.message}")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}