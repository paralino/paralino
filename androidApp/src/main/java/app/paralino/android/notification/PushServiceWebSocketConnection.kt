package app.paralino.android.notification

import app.paralino.core.logg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
            logg.d(TAG, "Not starting websocket, connection either closed or active")
            return
        }

        webSocket?.close(WS_CLOSE_CODE_NORMAL, "")
        webSocketConnectionState = WebSocketConnectionState.Connecting
        onStateChange(WebSocketConnectionState.Connecting)
        val nextListenerId = listenerId.incrementAndGet()

        val request = Request.Builder().url(url).build()
        logg.d(TAG, "Opening websocket to $url with listener $nextListenerId ...")
        webSocket = httpClient.newWebSocket(
            request = request,
            listener = ParalinoWebSocketListener(id = nextListenerId)
        )
    }

    @Synchronized
    fun close() {
        isConnectionClosed = true
        scope.cancel()

        logg.d(TAG, "Closing connection")

        webSocketConnectionState = WebSocketConnectionState.Disconnected
        onStateChange(WebSocketConnectionState.Disconnected)
        webSocket?.close(WS_CLOSE_CODE_NORMAL, "")
        webSocket = null
    }

    @Synchronized
    private fun scheduleReconnect(duration: Duration) {
        if (isConnectionClosed || webSocketConnectionState == WebSocketConnectionState.Connecting || webSocketConnectionState == WebSocketConnectionState.Connected) {
            return
        }
        webSocketConnectionState = WebSocketConnectionState.Scheduled
        onStateChange(WebSocketConnectionState.Scheduled)
        logg.d(TAG, "Scheduling a restart in $duration")

        scope.launch {
            delay(duration)
            start()
        }
    }

    private inner class ParalinoWebSocketListener(private val id: Long) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            synchronize {
                logg.d(TAG, "Connection opened")
                webSocketConnectionState = WebSocketConnectionState.Connected
                onStateChange(WebSocketConnectionState.Connected)
                if (errorCount > 0) {
                    errorCount = 0
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            synchronize {
                logg.d(TAG, "WebSocket received new message")
                onMessageCallback()
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
                logg.w(tag = TAG, message = "Connection failed: ${t.message}", t = t)
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
