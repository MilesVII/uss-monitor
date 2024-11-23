package com.milesseventh.ussmonitor

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import org.json.JSONObject
import java.io.IOException

const val WS_PORT = 7777

class Server (val platform: Platform, val shout: (message: String) -> Unit) : NanoWSD(WS_PORT) {
    fun wrapPayload (command: String, rid: String?, payload: JSONObject?): String {
        val response = JSONObject()

        response.put("command", command)
        if (rid != null) response.put("rid", rid)
        if (payload != null) response.put("payload", payload)

        return response.toString()
    }

    var protocol: Protocol? = null

    override fun serveHttp(session: IHTTPSession?): Response {
        if (session?.method == Method.OPTIONS) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/plain", "").apply {
                addHeader("Access-Control-Allow-Origin", "*")
                addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
            }
        }

        val response = super.serve(session)
        response?.addHeader("Access-Control-Allow-Origin", "*")
        return response
    }

    override fun openWebSocket(handshake: IHTTPSession): WebSocket {
        return object : WebSocket(handshake) {
            override fun onOpen() {
                shout("Client connected")
                protocol = Protocol(platform){
                        command, rid, payload ->
                    send(wrapPayload(command, rid, payload))
                }
            }

            override fun onClose(code: WebSocketFrame.CloseCode?, reason: String?, initiatedByRemote: Boolean) {
                shout("Client disconnected")
            }

            override fun onMessage(message: WebSocketFrame) {
                try {
                    if (protocol == null) return

                    val jsonObject = JSONObject(message.textPayload)
                    val actionName = jsonObject.optString("action")
                    val rid = jsonObject.optString("rid")
                    val payload: JSONObject? = jsonObject.optJSONObject("payload")

                    val action = protocol!!.actions[actionName]
                    val response = JSONObject()
                    response.put("handle", rid)
                    response.put("response", action?.invoke(rid, payload))

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
