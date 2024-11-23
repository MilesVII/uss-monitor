package com.milesseventh.ussmonitor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import kotlinx.coroutines.runBlocking
import org.json.JSONException
import org.json.JSONObject

data class Platform(
    val btManager: BluetoothManager
)

@SuppressLint("MissingPermission")
fun lookup(connector: BT, onFound: (name: String, address: String) -> Unit) = runBlocking {
    connector.scanLeDevice {
        device ->
        onFound(device.name, device.address)
    }
}

class Protocol(
    platform: Platform,
    send: (command: String, rid: String?, payload: JSONObject?) -> Unit
) {
    private val connector = BT(platform.btManager)

    val actions: Map<String, (rid: String, payload: JSONObject?) -> JSONObject?> = mapOf(
        "lookup" to {
            rid, _ ->
            val deviceList = lookup(connector) {
                name, address ->
                val response = JSONObject()
                val deviceJSON = JSONObject()
                deviceJSON.put("name", name)
                deviceJSON.put("address", address)

                response.put("device", deviceJSON)

                send("found", rid, response)
            }

            val json = JSONObject()
            json.put("devices", deviceList)
            json
        },
        "connect" to {
            _, payload ->

            val response = JSONObject()

            try {
                val address = payload?.getString("address") ?: throw JSONException("")

                val success = connector.connect(address)
                response.put("success", success)
                if (success)
                    response.put("error", "address not found or adapter unavailable")
            } catch (e: JSONException) {
                response.put("success", false)
                response.put("error", "no address provided")
            }

            response
        },
        "ping" to {
            _, _ ->
            val response = JSONObject()
            response.put("pong", true)
            response
        },
    )
}

