package app.paralino.core.provider

import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.utils.extensions.blocking
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeoutOrNull

abstract class IConnectionStatusProvider(
    private val backendConfig: BackendConfig
) {
    val status: MutableStateFlow<ConnectionStatus> = MutableStateFlow(ConnectionStatus.CONNECTED)
    private val checkData = InternetCheckData()

    suspend fun initAndGetStatus(): ConnectionStatus {
        val networkStatus = if(!isNetworkAvailable()) ConnectionStatus.OFFLINE
        else if(checkInternet(backendConfig.serverUrl)) ConnectionStatus.CONNECTED
        else ConnectionStatus.CONNECTED_NO_INTERNET

        logg.v("app_state", "Network status: $networkStatus")

        status.value = networkStatus
        return networkStatus
    }

    private suspend fun checkInternet(url: String): Boolean {
        checkData.checkCount = checkData.checkCount + 1

        if(!checkData.shouldCheck)
            return status.value == ConnectionStatus.CONNECTED

        return blocking {
            withTimeoutOrNull(3_000) {
                try {
                    checkData.lastCheckTime = nowUTC().toEpochMilliseconds()
                    checkData.checkCount = 0
                    isInternetAvailable(url)
                } catch (e: Exception) {
                    logg.w("app_state", "Error checking for connection")
                    false
                }
            }
        } == true
    }

    protected abstract suspend fun isInternetAvailable(url: String): Boolean
    protected abstract suspend fun isNetworkAvailable(): Boolean
}

enum class ConnectionStatus {
    CONNECTED, OFFLINE, CONNECTED_NO_INTERNET;

    val isOnline: Boolean get() = this == CONNECTED
    val isConnected: Boolean get() = isOnline || this == CONNECTED_NO_INTERNET
}

private data class InternetCheckData(
    var checkCount: Int = 0,
    var lastCheckTime: Long = 0
) {
    val shouldCheck: Boolean get() = nowUTC().toEpochMilliseconds() - lastCheckTime > 4_000 || checkCount >= 3
}