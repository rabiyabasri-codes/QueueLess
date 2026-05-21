package com.queueless.plus.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

object NetworkUtils {

    /** Returns true if the device currently has an active internet connection. */
    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Registers a real-time network callback.
     * [onAvailable] fires when internet is gained, [onLost] when it's lost.
     * Remember to call [unregisterNetworkCallback] with the returned callback when done.
     */
    fun registerNetworkCallback(
        context: Context,
        onAvailable: () -> Unit,
        onLost: () -> Unit
    ): ConnectivityManager.NetworkCallback {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = onAvailable()
            override fun onLost(network: Network)      = onLost()
        }

        cm.registerNetworkCallback(request, callback)
        return callback
    }

    fun unregisterNetworkCallback(
        context: Context,
        callback: ConnectivityManager.NetworkCallback
    ) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(callback)
        } catch (e: Exception) { /* already unregistered */ }
    }
}
