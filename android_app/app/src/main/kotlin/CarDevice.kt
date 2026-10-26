package com.example.carremote.data

import kotlinx.serialization.Serializable

@Serializable
data class CarDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val sharedSecret: String,
    val colorSeed: Long,
    val isLocked: Boolean = true
)
