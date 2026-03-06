package app.paralino.android.notification

import app.paralino.core.logg
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Heavily inspired by ntfy.sh and Appwrite:
 * https://github.com/binwiederhier/ntfy-android
 * https://github.com/appwrite/sdk-for-android
 */
class PushServiceWebSocketConnection(
    val url: String,
    private val httpClient: OkHttpClient,
    private val onStateChange: (WebSocketConnectionState) -> Unit,
    private val onMessageCallback: () -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var errorCount = 0
    private var webSocket: WebSocket? = null
    private var webSocketConnectionState: WebSocketConnectionState? = null
    private var isConnectionClosed = false

    private val listenerId = AtomicLong(0)

    @Synchronized
    fun start() {
        if (isConnectionClosed || webSocketConnectionState == WebSocketConnectionState.Connecting || webSocketConnectionState == WebSocketConnectionState.Connected) {
            logg.i(TAG, "Not starting websocket, connection either closed or active")
            return
        }

        webSocket?.close(WS_CLOSE_CODE_NORMAL, "")
        webSocketConnectionState = WebSocketConnectionState.Connecting
        onStateChange(WebSocketConnectionState.Connecting)
        val nextListenerId = listenerId.incrementAndGet()

        val request = Request.Builder().url(url).build()
        logg.i(TAG, "Opening websocket to $url with listener $nextListenerId ...")
        webSocket = httpClient.newWebSocket(
            request = request,
            listener = ParalinoWebSocketListener(id = nextListenerId)
        )
    }

    @Synchronized
    fun close() {
        isConnectionClosed = true
        scope.cancel()

        logg.i(TAG, "Closing websocket connection")

        webSocketConnectionState = WebSocketConnectionState.Disconnected
        onStateChange(WebSocketConnectionState.Disconnected)
        webSocket?.close(WS_CLOSE_CODE_NORMAL, "")
        webSocket = null
    }

    @Synchronized
    private fun scheduleReconnect(duration: Duration) {
        if (isConnectionClosed || webSocketConnectionState == WebSocketConnectionState.Connecting || webSocketConnectionState == WebSocketConnectionState.Connected) {
            logg.i(TAG, "Not reconnecting to websocket, isClosed: $isConnectionClosed, state: $webSocketConnectionState")
            return
        }
        webSocketConnectionState = WebSocketConnectionState.Scheduled
        onStateChange(WebSocketConnectionState.Scheduled)
        logg.i(TAG, "Scheduling a websocket restart in $duration")

        scope.launch {
            delay(duration)
            start()
        }
    }

    private inner class ParalinoWebSocketListener(private val id: Long) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            synchronize {
                logg.i(TAG, "Websocket connection opened")
                webSocketConnectionState = WebSocketConnectionState.Connected
                onStateChange(WebSocketConnectionState.Connected)
                if (errorCount > 0) {
                    errorCount = 0
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            synchronize {
                val eventType = try {
                    BaseRemoteModule.formatter.parseToJsonElement(text)
                        .jsonObject["event"]?.jsonPrimitive?.content
                } catch (e: Exception) {
                    false
                }

                if (eventType == "message") {
                    logg.i(TAG, "WebSocket received new >message< event")
                    onMessageCallback()
                } else {
                    logg.i(TAG, "WebSocket received new >$eventType< event, ignoring")
                }
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            synchronize {
                logg.w(TAG, "Connection closed")
                webSocketConnectionState = WebSocketConnectionState.Disconnected
                onStateChange(WebSocketConnectionState.Disconnected)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            synchronize {
                logg.w(tag = TAG, message = "Connection failed, already closed: $isConnectionClosed - ${t::class} ${t.message}", t = t)
                if (isConnectionClosed) {
                    return@synchronize
                }

                webSocketConnectionState = WebSocketConnectionState.Disconnected
                onStateChange(WebSocketConnectionState.Disconnected)

                errorCount++
                val retrySeconds = RETRY_SECONDS.getOrNull(errorCount - 1) ?: RETRY_SECONDS.last()

                scheduleReconnect(retrySeconds.seconds)
            }
        }

        private fun synchronize(action: () -> Unit) {
            synchronized(this@PushServiceWebSocketConnection) {
                if (listenerId.get() == id) {
                    action()
                }
            }
        }
    }

    enum class WebSocketConnectionState {
        Scheduled, Connecting, Connected, Disconnected
    }

    companion object Companion {
        private const val TAG = "push_notification_socket"
        private const val WS_CLOSE_CODE_NORMAL = 1000
        private val RETRY_SECONDS = listOf(5, 10, 15, 20, 30, 45, 60, 120)
    }
}
