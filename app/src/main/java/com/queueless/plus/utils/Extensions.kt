package com.queueless.plus.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment

// ─── View Extensions ─────────────────────────────────────────────────────────

fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

// ─── Toast Helpers ────────────────────────────────────────────────────────────

fun Context.toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Context.toastLong(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()

fun Fragment.toast(message: String) =
    requireContext().toast(message)

// ─── Time Formatting ─────────────────────────────────────────────────────────

/**
 * Converts a wait time in minutes into a human-readable string.
 * e.g. 75 → "1 hr 15 min"
 */
fun Int.formatWaitTime(): String {
    if (this <= 0) return "Ready now!"
    val hours   = this / 60
    val minutes = this % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours hr $minutes min"
        hours > 0                -> "$hours hr"
        else                     -> "$minutes min"
    }
}

// ─── Input Validation ────────────────────────────────────────────────────────

fun String.isValidEmail(): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun String.isValidPassword(): Boolean = length >= 6
