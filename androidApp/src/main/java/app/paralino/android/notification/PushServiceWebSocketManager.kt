package app.paralino.android.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import app.paralino.core.logg
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.user.IPushProviderManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PushServiceWebSocketManager(
    private val pushProvider: IPushProviderManager,
    private val scopeProvider: ICoroutineScopeProvider
) {
    private var connection: PushServiceWebSocketConnection? = null
    
    private val _connectionState = MutableStateFlow(PushServiceWebSocketConnection.WebSocketConnectionState.Disconnected)
    val connectionState: StateFlow<PushServiceWebSocketConnection.WebSocketConnectionState> = _connectionState.asStateFlow()

    private val connectionLock = Mutex()

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(1, TimeUnit.MINUTES)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun startConnection(url: String) {
        connectionLock.withLock {
            val baseUrl = url.substringBefore("?")
            val wsUrl = if (baseUrl.endsWith("/ws")) {
                baseUrl
            } else if (baseUrl.endsWith("/")) {
                "${baseUrl}ws"
            } else {
                "$baseUrl/ws"
            }

            if (connection?.url == wsUrl) {
                logg.i("notification_socket_manager", "Websocket url not changed, abort restarting connection")
                return
            }

            closeConnectionNow()
            logg.i("notification_socket_manager", "Starting WebSocket connection to $wsUrl")

            connection = PushServiceWebSocketConnection(
                url = wsUrl,
                httpClient = httpClient,
                onStateChange = { state ->
                    _connectionState.value = state
                },
                onMessageCallback = {
                    pushProvider.syncPushNotifications()
                }
            )
            connection?.start()
        }
    }

    fun closeConnection() {
        scopeProvider.userSessionIOScope.launch {
            connectionLock.withLock {
                closeConnectionNow()
            }
        }
    }

    private fun closeConnectionNow() {
        if (connection != null) {
            logg.i("notification_socket_manager", "Closing WebSocket connection")
            connection?.close()
            connection = null
            _connectionState.value = PushServiceWebSocketConnection.WebSocketConnectionState.Disconnected
        }
    }
}
