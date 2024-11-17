package com.milesseventh.ussmonitor

import fi.iki.elonen.NanoWSD
import org.json.JSONObject
import java.io.IOException

const val WS_PORT = 7777

class Server (platform: Platform, val shout: (message: String) -> Unit) : NanoWSD(WS_PORT) {
    val protocol = Protocol(platform)

    override fun openWebSocket(handshake: IHTTPSession): WebSocket {
        return object : WebSocket(handshake) {
            override fun onOpen() {
                shout("Client connected")
            }

            override fun onClose(code: WebSocketFrame.CloseCode?, reason: String?, initiatedByRemote: Boolean) {
                shout("Client disconnected")
            }

            override fun onMessage(message: WebSocketFrame) {
                try {

                    val jsonObject = JSONObject(message.textPayload)
                    val actionName = jsonObject.optString("action")
                    val handle = jsonObject.optString("handle")

                    val action = protocol.actions[actionName]
                    val response = JSONObject()
                    response.put("handle", handle)
                    response.put("response", action?.invoke())

                    send(response.toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                    send(JSONObject().put("error", "Invalid JSON").toString())
                }
            }

            override fun onPong(message: WebSocketFrame) {
                try {
                    send("Pong: ${message.textPayload}")
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun onException(exception: IOException) {
                exception.printStackTrace()
            }
        }
    }
}
