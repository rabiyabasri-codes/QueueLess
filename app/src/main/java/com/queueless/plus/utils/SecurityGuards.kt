package com.queueless.plus.utils

import android.app.Activity

fun Activity.requireAdminAccess(session: SessionManager): Boolean {
    if (session.isAdmin) return true

    toast("Admin access required")
    finish()
    return false
}
