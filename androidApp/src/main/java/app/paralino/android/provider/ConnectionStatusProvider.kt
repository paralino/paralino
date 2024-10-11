package app.paralino.android.provider

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.NetworkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

class ConnectionStatusProvider constructor(
    private val context: Context,
    appBuildConfig: AppBuildConfig,
) : IConnectionStatusProvider(appBuildConfig) {

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

    @SuppressLint("MissingPermission")
    override suspend fun getNetworkType(): NetworkType? {
        try {
            val cm = context.getSystemService(Application.CONNECTIVITY_SERVICE) as ConnectivityManager
            val actNw = cm.getNetworkCapabilities(cm.activeNetwork ?: return null) ?: return null
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    val hasDangerousPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                    val hasSystemPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_BASIC_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                    } else false

                    val cellType = if(hasDangerousPermission || hasSystemPermission) {
                        val telephonyManager =
                            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                        when (telephonyManager.dataNetworkType) {
                            TelephonyManager.NETWORK_TYPE_UNKNOWN -> null

                            TelephonyManager.NETWORK_TYPE_GPRS,
                            TelephonyManager.NETWORK_TYPE_EDGE,
                            TelephonyManager.NETWORK_TYPE_CDMA,
                            TelephonyManager.NETWORK_TYPE_1xRTT,
                            TelephonyManager.NETWORK_TYPE_IDEN,
                            TelephonyManager.NETWORK_TYPE_GSM -> NetworkType.G2

                            TelephonyManager.NETWORK_TYPE_UMTS,
                            TelephonyManager.NETWORK_TYPE_EVDO_0,
                            TelephonyManager.NETWORK_TYPE_EVDO_A,
                            TelephonyManager.NETWORK_TYPE_HSDPA,
                            TelephonyManager.NETWORK_TYPE_HSUPA,
                            TelephonyManager.NETWORK_TYPE_HSPA,
                            TelephonyManager.NETWORK_TYPE_EVDO_B,
                            TelephonyManager.NETWORK_TYPE_EHRPD,
                            TelephonyManager.NETWORK_TYPE_HSPAP,
                            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> NetworkType.G3

                            TelephonyManager.NETWORK_TYPE_LTE,
                            TelephonyManager.NETWORK_TYPE_IWLAN -> NetworkType.G4

                            TelephonyManager.NETWORK_TYPE_NR -> NetworkType.G5

                            else -> null
                        }
                    } else {
                        null
                    }
                    cellType ?: NetworkType.GENERIC_CELL
                }

                else -> null
            }
        } catch (e: Exception) {
            return null
        }
    }

    override suspend fun getSignalStrength(): Int? {
        try {
            val cm = context.getSystemService(Application.CONNECTIVITY_SERVICE) as ConnectivityManager
            val actNw = cm.getNetworkCapabilities(cm.activeNetwork ?: return null) ?: return null
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    val wm = context.getSystemService(Application.WIFI_SERVICE) as WifiManager
                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        WifiManager.calculateSignalLevel(wm.connectionInfo.rssi, 5)
                    else null
                }
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    telephonyManager.signalStrength?.level
                }

                else -> null
            }
        } catch (e: Exception) {
            return null
        }
    }
}
