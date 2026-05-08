package app.paralino.core.provider


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
    WIFI, G2, G3, G4, G5, GENERIC_CELL;
    val key: String = name
    val networkName: String get() = when (this){
        WIFI -> "Wi-Fi"
        G2 -> "2G"
        G3 -> "3G"
        G4 -> "4G"
        G5 -> "5G"
        GENERIC_CELL -> ""
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