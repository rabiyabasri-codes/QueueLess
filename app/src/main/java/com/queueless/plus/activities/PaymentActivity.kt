package com.queueless.plus.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.queueless.plus.databinding.ActivityPaymentBinding
import com.queueless.plus.utils.toast

class PaymentActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PAYMENT_TYPE = "extra_payment_type"
        const val EXTRA_TOTAL        = "extra_total"
        const val TYPE_UPI           = "UPI"
        const val TYPE_CARD          = "CARD"
    }

    private lateinit var binding: ActivityPaymentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val paymentType = intent.getStringExtra(EXTRA_PAYMENT_TYPE) ?: TYPE_UPI
        val total       = intent.getIntExtra(EXTRA_TOTAL, 0)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (paymentType == TYPE_UPI) "UPI Payment" else "Card Payment"

        binding.tvPaymentTotal.text = "Total: ₹$total"

        when (paymentType) {
            TYPE_UPI -> {
                binding.tvPaymentIcon.text  = "📲"
                binding.tvPaymentTitle.text = "UPI Payment"
                binding.layoutUPI.visibility  = View.VISIBLE
                binding.layoutCard.visibility = View.GONE
                binding.btnPay.text = "Pay ₹$total via UPI"
            }
            TYPE_CARD -> {
                binding.tvPaymentIcon.text  = ""
                binding.tvPaymentTitle.text = "Card Payment"
                binding.layoutCard.visibility = View.VISIBLE
                binding.layoutUPI.visibility  = View.GONE
                binding.btnPay.text = "Pay ₹$total via Card"
            }
        }

        binding.btnPay.setOnClickListener {
            simulatePayment(paymentType, total)
        }
    }

    private fun simulatePayment(type: String, total: Int) {
        binding.btnPay.isEnabled = false
        binding.btnPay.text = "Processing..."

        // Simulate a small delay then confirm
        binding.btnPay.postDelayed({
            toast(" Payment of ₹$total successful! (Demo)")
            setResult(RESULT_OK)
            finish()
        }, 1500)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
