package com.queueless.plus.models

data class CartItem(
    val name: String = "",
    val price: Int = 0,
    var quantity: Int = 1
)