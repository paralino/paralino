package app.paralino.core.provider

import app.paralino.core.logg
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.utils.extensions.blocking
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

abstract class IConnectionStatusProvider(
    protected val appBuildConfig: AppBuildConfig
) {
    private val _status: MutableStateFlow<NetworkConnectionInfo> = MutableStateFlow(NetworkConnectionInfo(ConnectionStatus.CONNECTED))
    val status = _status.asStateFlow()
    val currentStatus get() = status.value
    private val checkData = InternetCheckData()
    private val mutex = Mutex()

    suspend fun initAndGetStatus(forceCheck: Boolean = false): NetworkConnectionInfo {
        val networkStatus = when {
            !isNetworkAvailable() -> {
                logg.v("app_state", "Network status OFFLINE")
                NetworkConnectionInfo(ConnectionStatus.OFFLINE)
            }
            checkInternet(url = appBuildConfig.apiServerConfig.serverUrl, forceCheck = forceCheck) -> {
                NetworkConnectionInfo(ConnectionStatus.CONNECTED, getNetworkType())
            }
            else -> {
                logg.v("app_state", "Network status CONNECTED_NO_INTERNET")
                NetworkConnectionInfo(ConnectionStatus.CONNECTED_NO_INTERNET, getNetworkType())
            }
        }

        _status.value = networkStatus
        return networkStatus
    }

    private suspend fun checkInternet(url: String, forceCheck: Boolean): Boolean {
        mutex.withLock {
            checkData.checkCount = checkData.checkCount + 1
            if(forceCheck || checkData.shouldCheck || (checkData.couldCheck && !status.value.isOnline)) {
                return blocking {
                    return@blocking withTimeoutOrNull<Boolean>(3_000) {
                        checkData.lastCheckTime = nowUTC().toEpochMilliseconds()
                        checkData.checkCount = 0
                        return@withTimeoutOrNull try {
                            isInternetAvailable(url)
                        } catch (e: Exception) {
                            false
                        }
                    }
                } == true
            } else {
                return status.value.isOnline
            }
        }
    }

    protected abstract suspend fun isInternetAvailable(url: String): Boolean
    protected abstract suspend fun isNetworkAvailable(): Boolean
    protected abstract suspend fun getNetworkType(): NetworkType?
    abstract suspend fun getSignalStrength(): Int?
    open suspend fun getConnectedWifi(): ConnectedWifi? = null
}

data class ConnectedWifi(
    val bssid: String,
    val ssid: String?,
    // usually hotspot
    val isMetered: Boolean
)

private data class InternetCheckData(
    var checkCount: Int = 0,
    var lastCheckTime: Long = 0
) {
    val shouldCheck: Boolean get() = nowUTC().toEpochMilliseconds() - lastCheckTime > 15_000
    val couldCheck: Boolean get() = nowUTC().toEpochMilliseconds() - lastCheckTime > 4_000 || checkCount >= 3
}