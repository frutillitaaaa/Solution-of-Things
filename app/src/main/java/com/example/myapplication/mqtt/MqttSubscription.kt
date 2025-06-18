package com.example.myapplication.mqtt

import java.util.Date

data class MqttSubscription(
    val topic: String,
    val qos: Int,
    var lastMessage: String? = null,
    var timestamp: Date? = null
) 