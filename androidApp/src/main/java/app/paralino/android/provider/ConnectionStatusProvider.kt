package app.paralino.android.provider

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import app.paralino.core.models.BackendConfig
import app.paralino.core.provider.IConnectionStatusProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

class ConnectionStatusProvider constructor(
    private val context: Context,
    backendConfig: BackendConfig,
): IConnectionStatusProvider(backendConfig) {

    override suspend fun isInternetAvailable(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            val uri = Uri.parse(url)
            !InetAddress.getByName(uri.host).equals("")
        }
    }

    override suspend fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Application.CONNECTIVITY_SERVICE) as ConnectivityManager
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
