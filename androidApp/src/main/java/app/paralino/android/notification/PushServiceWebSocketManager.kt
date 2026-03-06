package app.paralino.android.notification

import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.ParalinoSocketConnectionType
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.user.IPushProviderManager
import app.paralino.core.utils.extensions.nowUTC
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class PushServiceWebSocketManager(
    private val pushProvider: IPushProviderManager,
    private val scopeProvider: ICoroutineScopeProvider,
    private val deviceApiProvider: IDeviceApiProvider,
    private val appPrefs: AppPrefs
): BaseRepository() {
    private var connection: PushServiceWebSocketConnection? = null
    private var periodicJob: Job? = null
    private var oneTimeCheckJob: Job? = null

    private val _connectionState = MutableStateFlow(PushServiceWebSocketConnection.WebSocketConnectionState.Disconnected)
    val connectionState: StateFlow<PushServiceWebSocketConnection.WebSocketConnectionState> = _connectionState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Instant?>(null)
    val lastSyncTime: StateFlow<Instant?> = _lastSyncTime.asStateFlow()
    private val periodicPollState = MutableStateFlow<PeriodicPollState?>(null)

    private val connectionLock = Mutex()

    private var socketHttpClient: OkHttpClient? = null

    suspend fun startConnection(url: String) {
        connectionLock.withLock {
            val connectionType = appPrefs.paralinoSocketConnectionType
            if (connectionType == ParalinoSocketConnectionType.REALTIME) {
                startRealtimeSocketConnection(url = url)
            } else {
                startPeriodicPollConnection(url = url, connectionType = connectionType)
            }
        }
    }

    fun closeConnection() {
        scopeProvider.userSessionIOScope.launch {
            connectionLock.withLock {
                closeConnectionNow()
            }
        }
    }

    private fun startRealtimeSocketConnection(url: String) {
        periodicJob?.cancel()

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

        val currentClient = socketHttpClient
        val client = if (currentClient != null) {
            currentClient
        } else {
            val newClient = OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(1, TimeUnit.MINUTES)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build()

            socketHttpClient = newClient
            newClient
        }

        connection = PushServiceWebSocketConnection(
            url = wsUrl,
            httpClient = client,
            onStateChange = { state ->
                _connectionState.value = state

                // when connected, check latest messages to fetch anything missing
                if (state == PushServiceWebSocketConnection.WebSocketConnectionState.Connected) {
                    checkMessagesOnce(baseUrl = baseUrl)
                }
            },
            onMessageCallback = {
                pushProvider.syncPushNotifications()
                _lastSyncTime.value = nowUTC()
            }
        )
        connection?.start()
    }

    private fun startPeriodicPollConnection(
        url: String,
        connectionType: ParalinoSocketConnectionType
    ) {
        closeConnectionNow()
        socketHttpClient = null

        _connectionState.value = PushServiceWebSocketConnection.WebSocketConnectionState.Connected
        if (periodicJob?.isActive != true) {
            periodicJob = scopeProvider.userSessionIOScope.launch {
                while (isActive) {
                    val newState = checkMessages(
                        connectionType = connectionType,
                        currentState = periodicPollState.value,
                        baseUrl = url,
                        lastUpdateTimeUnixSeconds = _lastSyncTime.value?.epochSeconds
                    )
                    logg.i("notification_socket_manager", "Delay next push message refresh for ${newState.nextDelay}")
                    periodicPollState.update { newState }
                    delay(newState.nextDelay)
                }
            }
        }
    }

    private suspend fun checkMessages(
        connectionType: ParalinoSocketConnectionType,
        currentState: PeriodicPollState?,
        baseUrl: String,
        lastUpdateTimeUnixSeconds: Long?
    ): PeriodicPollState {
        val time = lastUpdateTimeUnixSeconds ?: "latest"
        val url = "${baseUrl.substringBefore("?")}/json?poll=1&since=$time"
        logg.i("notification_socket_manager", "Fetch push messages since $time")

        val client = HttpClient() {
            if (AppBuildConfig.instance.isDevMode) {
                install(Logging) {
                    logger = object : io.ktor.client.plugins.logging.Logger {
                        override fun log(message: String) {
                            logg.d("notification_socket_manager", message)
                        }
                    }
                    level = LogLevel.INFO
                }
            }
        }
        val state = try {
            val response = client.request(urlString = url)
            val responseText = response.bodyAsText()

            if (response.status.isSuccess()) {
                _connectionState.value = PushServiceWebSocketConnection.WebSocketConnectionState.Connected
                if (responseText.isNotBlank() || currentState?.shouldMakeFullRefresh == true) {
                    logg.i("notification_socket_manager", "Found new push messages, sync notifications")
                    pushProvider.syncPushNotifications()
                    _lastSyncTime.value = nowUTC()

                    PeriodicPollState(
                        nextDelay = getDefaultDelayDuration(connectionType),
                        shouldMakeFullRefresh = false,
                        counter = 0
                    )
                } else {
                    logg.i("notification_socket_manager", "No new push messages")
                    _lastSyncTime.value = nowUTC()

                    val counter = currentState?.counter?.plus(1) ?: 0
                    PeriodicPollState(
                        nextDelay = getDefaultDelayDuration(connectionType),
                        shouldMakeFullRefresh = counter > 3,
                        counter = counter
                    )
                }
            } else {
                logg.i("notification_socket_manager", "Failed to fetch new messages: ${response.status}")
                _connectionState.value = PushServiceWebSocketConnection.WebSocketConnectionState.Scheduled
                PeriodicPollState(
                    nextDelay = getDefaultDelayDuration(connectionType) / 2,
                    shouldMakeFullRefresh = false,
                    counter = currentState?.counter ?: 0
                )
            }
        } catch (e: Exception) {
            logg.w("notification_socket_manager", "Failed to check messages", e)
            _connectionState.value = PushServiceWebSocketConnection.WebSocketConnectionState.Scheduled
            PeriodicPollState(
                nextDelay = getDefaultDelayDuration(connectionType) / 2,
                shouldMakeFullRefresh = true,
                counter = 0
            )
        }

        return state
    }

    private fun getDefaultDelayDuration(connectionType: ParalinoSocketConnectionType): Duration {
        val batteryLevel = deviceApiProvider.batteryLevel ?: 0
        val delayDuration = when (connectionType) {
            ParalinoSocketConnectionType.PERIODIC_LEVEL_1 -> {
                when {
                    batteryLevel > 60 -> 5.minutes
                    batteryLevel > 30 -> 10.minutes
                    else -> 15.minutes
                }
            }

            ParalinoSocketConnectionType.PERIODIC_LEVEL_2 -> {
                when {
                    batteryLevel > 60 -> 15.minutes
                    batteryLevel > 30 -> 25.minutes
                    else -> 35.minutes
                }
            }

            ParalinoSocketConnectionType.PERIODIC_LEVEL_3 -> {
                when {
                    batteryLevel > 60 -> 1.hours
                    batteryLevel > 30 -> 2.hours
                    else -> 3.hours
                }
            }

            else -> 30.minutes
        }

        return delayDuration
    }

    private fun checkMessagesOnce(baseUrl: String) {
        oneTimeCheckJob?.cancel()
        oneTimeCheckJob = scopeProvider.userSessionIOScope.launch {
            checkMessages(
                connectionType = ParalinoSocketConnectionType.PERIODIC_LEVEL_1,
                currentState = PeriodicPollState(
                    nextDelay = 5.minutes,
                    shouldMakeFullRefresh = false,
                    counter = 0
                ),
                baseUrl = baseUrl,
                lastUpdateTimeUnixSeconds = _lastSyncTime.value?.epochSeconds
            )
        }
    }

    private fun closeConnectionNow() {
        periodicJob?.cancel()
        periodicJob = null
        if (connection != null) {
            logg.i("notification_socket_manager", "Closing WebSocket connection")
            connection?.close()
            connection = null
            socketHttpClient = null
            _connectionState.value = PushServiceWebSocketConnection.WebSocketConnectionState.Disconnected
        }
    }

    private data class PeriodicPollState(
        val nextDelay: Duration,
        val counter: Int,
        val shouldMakeFullRefresh: Boolean
    )
}
