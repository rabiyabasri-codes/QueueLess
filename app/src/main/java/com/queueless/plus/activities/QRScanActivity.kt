package com.queueless.plus.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.queueless.plus.databinding.ActivityQrScanBinding
import com.queueless.plus.utils.toast

class QRScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScanBinding

    private val scanLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) {
            handleScanResult(result.contents)
        } else {
            toast("Scan cancelled")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Scan QR Code"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnScanQr.setOnClickListener { startScan() }
    }

    private fun startScan() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan a QueueLess+ QR code")
            setCameraId(0)
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setOrientationLocked(true)
        }
        scanLauncher.launch(options)
    }

    /**
     * QR code payload format: "queueless://join?queueId=<QUEUE_ID>"
     */
    private fun handleScanResult(content: String) {
        if (content.startsWith("queueless://join?queueId=")) {
            val queueId = content.removePrefix("queueless://join?queueId=")
            val intent = Intent(this, QueueDetailActivity::class.java).apply {
                putExtra(QueueDetailActivity.EXTRA_QUEUE_ID, queueId)
            }
            startActivity(intent)
            finish()
        } else {
            toast("Invalid QR code")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }
}
