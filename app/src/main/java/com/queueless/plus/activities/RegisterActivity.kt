package com.queueless.plus.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.queueless.plus.databinding.ActivityRegisterBinding
import com.queueless.plus.models.User
import com.queueless.plus.utils.FirestoreRepository
import com.queueless.plus.utils.SessionManager
import com.queueless.plus.utils.hide
import com.queueless.plus.utils.show
import com.queueless.plus.utils.toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        val name     = binding.etName.text.toString().trim()
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm  = binding.etConfirmPassword.text.toString()

        // ✅ VALIDATION
        when {
            name.isEmpty() -> {
                binding.etName.error = "Name is required"
                return
            }

            email.isEmpty() -> {
                binding.etEmail.error = "Email is required"
                return
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.etEmail.error = "Enter valid email"
                return
            }

            password.length < 6 -> {
                binding.etPassword.error = "Minimum 6 characters required"
                return
            }

            password != confirm -> {
                binding.etConfirmPassword.error = "Passwords do not match"
                return
            }
        }

        binding.progressBar.show()
        binding.btnRegister.isEnabled = false

        lifecycleScope.launch {
            try {
                // 🔥 1. Create Firebase Auth user
                val firebaseUser = FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, password)
                    .await()
                    .user ?: throw Exception("User creation failed")

                // 🔥 2. Get FCM token
                val fcmToken = try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (e: Exception) {
                    ""
                }

                // 🔥 3. Create user object
                val user = User(
                    userId = firebaseUser.uid,
                    name = name,
                    email = email,
                    role = "user",
                    fcmToken = fcmToken
                )

                // 🔥 4. Save to Firestore
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.userId)
                    .set(user)
                    .await()

                // 🔥 5. Save session
                session.userId = user.userId
                session.userName = user.name
                session.userRole = user.role
                session.fcmToken = fcmToken

                // ✅ SUCCESS
                toast("Account created successfully 🎉")

                startActivity(Intent(this@RegisterActivity, DashboardActivity::class.java))
                finishAffinity()

            } catch (e: Exception) {
                // ❌ SHOW REAL ERROR
                toast(e.localizedMessage ?: "Registration failed. Try again.")
            } finally {
                binding.progressBar.hide()
                binding.btnRegister.isEnabled = true
            }
        }
    }
}