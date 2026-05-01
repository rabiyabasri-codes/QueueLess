package com.queueless.plus.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import android.view.View
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

        setSupportActionBar(null)

        binding.btnScan.setOnClickListener {
            startScan()
        }

        binding.btnMarkReady.setOnClickListener {
            markOrderReady()
        }
    }

    // 🔥 QR SCANNER
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
        options.setPrompt("Scan User QR")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)

        scanner.launch(options)
    }

    // 🔥 FETCH USER DATA
    private fun fetchUserDetails(entryId: String) {

        db.collection("queueEntries")
            .document(entryId)
            .get()
            .addOnSuccessListener { doc ->

                if (!doc.exists()) {
                    binding.tvResult.text = "❌ Invalid QR"
                    return@addOnSuccessListener
                }

                val name = doc.getString("userName") ?: "Unknown"
                val order = doc.getString("orderDetails") ?: "No order"
                val status = doc.getString("orderStatus") ?: "waiting"

                binding.tvResult.text =
                    "👤 $name\n🍔 $order\n📦 Status: $status"

                binding.btnMarkReady.visibility = android.view.View.VISIBLE
            }
            .addOnFailureListener {
                toast("Error: ${it.message}")
            }
    }

    // 🔥 MARK ORDER READY
    private fun markOrderReady() {

        if (scannedEntryId.isEmpty()) {
            toast("Scan first")
            return
        }

        db.collection("queueEntries")
            .document(scannedEntryId)
            .get()
            .addOnSuccessListener { doc ->
                val orderId = doc.getString("orderId")
                if (orderId != null) {
                    lifecycleScope.launch {
                        try {
                            FirestoreRepository.updateQueueEntryOrderStatus(scannedEntryId, QueueEntry.ORDER_READY)
                            toast("Order marked READY ✅")
                            binding.btnMarkReady.visibility = android.view.View.GONE
                        } catch (e: Exception) {
                            toast("Failed: ${e.message}")
                        }
                    }
                } else {
                    toast("No order attached")
                }
            }
            .addOnFailureListener {
                toast("Failed: ${it.message}")
            }
    }
}