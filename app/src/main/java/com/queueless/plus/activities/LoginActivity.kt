package com.queueless.plus.activities

import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.tasks.await
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.queueless.plus.databinding.ActivityLoginBinding
import com.queueless.plus.utils.AuthManager
import com.queueless.plus.utils.isValidEmail
import com.queueless.plus.utils.isValidPassword
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.hide
import com.queueless.plus.utils.show
import com.queueless.plus.utils.toast
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (!email.isValidEmail()) {
            binding.etEmail.error = "Enter a valid email"
            return
        }
        if (!password.isValidPassword()) {
            binding.etPassword.error = "Password must be at least 6 characters"
            return
        }

        binding.progressBar.show()
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                // 🔥 1. Firebase login
                val firebaseUser = com.google.firebase.auth.FirebaseAuth
                    .getInstance()
                    .signInWithEmailAndPassword(email, password)
                    .await()
                    .user ?: throw Exception("Login failed")

                val uid = firebaseUser.uid

                // 🔥 2. Fetch user directly from Firestore
                val doc = com.google.firebase.firestore.FirebaseFirestore
                    .getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .await()

                if (!doc.exists()) {
                    toast("User profile not found. Please register again.")
                    return@launch
                }

                val name = doc.getString("name") ?: ""
                val role = doc.getString("role") ?: "user"

                // 🔥 3. Save session
                session.userId = uid
                session.userName = name
                session.userRole = role

                // ✅ SUCCESS
                toast("Login successful 🎉")

                startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                finish()

            } catch (e: Exception) {
                toast("Login failed: ${e.localizedMessage}")
            } finally {
                binding.progressBar.hide()
                binding.btnLogin.isEnabled = true
            }
        }
    }
}
