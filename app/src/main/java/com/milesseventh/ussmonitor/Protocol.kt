package com.milesseventh.ussmonitor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import kotlinx.coroutines.runBlocking
import org.json.JSONObject



data class Platform(
    val btManager: BluetoothManager
)

@SuppressLint("MissingPermission")
fun lookup(connector: BT): List<String> {
    return runBlocking {
        connector
            .scanLeDevice()
            .map { device -> device.name }
    }
}

fun ping() {
    println("pong")
}

class Protocol(platform: Platform) {
    private val connector = BT(platform.btManager)

    val actions: Map<String, () -> JSONObject?> = mapOf(
        "lookup" to {
            val deviceList = lookup(connector)

            val json = JSONObject()
            json.put("devices", deviceList)
            json
        },
        "ping" to { ping(); null },
    )
}

