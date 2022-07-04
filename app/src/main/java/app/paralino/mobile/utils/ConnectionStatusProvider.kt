package app.paralino.mobile.utils

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import app.paralino.core.models.BackendConfig
import app.paralino.core.utils.ConnectionStatus
import app.paralino.core.utils.IConnectionStatusProvider
import app.paralino.core.utils.extensions.blocking
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress

class ConnectionStatusProvider constructor(
    private val app: Application,
    private val backendConfig: BackendConfig
): IConnectionStatusProvider {

    private val checkData = InternetCheckData()

    // assume we are connected for first time
    override var status: ConnectionStatus = ConnectionStatus.CONNECTED

    override suspend fun initAndGetStatus(): ConnectionStatus {
        status = if(!isNetworkAvailable()) ConnectionStatus.OFFLINE
        else if(isInternetAvailable(backendConfig.serverUrl)) ConnectionStatus.CONNECTED
        else ConnectionStatus.CONNECTED_NO_INTERNET
        return status
    }

    private suspend fun isInternetAvailable(url: String): Boolean {
        checkData.checkCount = checkData.checkCount + 1

        if(!checkData.shouldCheck)
            return status == ConnectionStatus.CONNECTED

        return blocking {
            withTimeoutOrNull(3_000) {
                try {
                    checkData.lastCheckTime = System.currentTimeMillis()
                    checkData.checkCount = 0
                    val uri = Uri.parse(url)
                    !InetAddress.getByName(uri.host).equals("")
                } catch (e: Exception) {
                    false
                }
            }
        } == true
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = app.getSystemService(Application.CONNECTIVITY_SERVICE) as ConnectivityManager
        val actNw = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    }
}

private data class InternetCheckData(
    var checkCount: Int = 0,
    var lastCheckTime: Long = 0
) {
    val shouldCheck: Boolean get() = System.currentTimeMillis() - lastCheckTime > 4_000 || checkCount >= 3
}
