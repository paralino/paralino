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
    val status: MutableStateFlow<NetworkConnectionInfo> = MutableStateFlow(NetworkConnectionInfo(ConnectionStatus.CONNECTED))
    private val checkData = InternetCheckData()

    suspend fun initAndGetStatus(): NetworkConnectionInfo {
        val networkStatus = if(!isNetworkAvailable()) NetworkConnectionInfo(ConnectionStatus.OFFLINE)
        else if(checkInternet(backendConfig.serverUrl)) NetworkConnectionInfo(ConnectionStatus.CONNECTED, getNetworkType())
        else NetworkConnectionInfo(ConnectionStatus.CONNECTED_NO_INTERNET, getNetworkType())

        logg.v("app_state", "Network status: $networkStatus")

        status.value = networkStatus
        return networkStatus
    }

    private suspend fun checkInternet(url: String): Boolean {
        checkData.checkCount = checkData.checkCount + 1

        if(!checkData.shouldCheck)
            return status.value.status == ConnectionStatus.CONNECTED

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
    protected abstract suspend fun getNetworkType(): NetworkType?
    abstract suspend fun getSignalStrength(): Int?
}

data class NetworkConnectionInfo(
    val status: ConnectionStatus,
    val networkType: NetworkType? = null
) {
    val isOnline: Boolean get() = status.isOnline
}

enum class ConnectionStatus {
    CONNECTED, OFFLINE, CONNECTED_NO_INTERNET;

    val isOnline: Boolean get() = this == CONNECTED
    val isConnected: Boolean get() = isOnline || this == CONNECTED_NO_INTERNET
}

enum class NetworkType {
    WIFI, G2, G3, G4, G5;
    val key: String = name
    val networkName: String get() = when (this){
        WIFI -> "WiFi"
        G2 -> "2G"
        G3 -> "3G"
        G4 -> "4G"
        G5 -> "5G"
    }
    companion object {
        fun fromKey(key: String?): NetworkType? {
            return NetworkType.values().firstOrNull { it.key == key }
        }
    }
}

enum class NetworkSignalStrength {
    POOR, GOOD, GREAT;
    val key: String = name
    companion object {
        fun fromKey(key: String?): NetworkSignalStrength? {
            return NetworkSignalStrength.values().firstOrNull { it.key == key }
        }
        fun fromLevel(level: Int?) : NetworkSignalStrength? {
            return when {
                level == null -> null
                level <= 1 -> POOR
                level >= 4 -> GREAT
                else -> GOOD
            }
        }
    }
}

private data class InternetCheckData(
    var checkCount: Int = 0,
    var lastCheckTime: Long = 0
) {
    val shouldCheck: Boolean get() = nowUTC().toEpochMilliseconds() - lastCheckTime > 4_000 || checkCount >= 3
}