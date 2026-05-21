package com.queueless.plus.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.queueless.plus.databinding.ActivityFavoritesRecentBinding
import com.queueless.plus.utils.SessionManager

class FavoritesRecentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesRecentBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesRecentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Favorites & Recent"

        val favorites = session.getFavoriteItems()
        val recents = session.getRecentOrders()

        binding.tvFavorites.text = if (favorites.isEmpty()) {
            "No favorites yet. Long press items in order screen."
        } else {
            favorites.joinToString("\n")
        }
        binding.tvRecentOrders.text = if (recents.isEmpty()) {
            "No recent orders yet"
        } else {
            recents.joinToString("\n\n")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
