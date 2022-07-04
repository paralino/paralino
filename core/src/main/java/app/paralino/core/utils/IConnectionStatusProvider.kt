package app.paralino.core.utils

interface IConnectionStatusProvider {
    var status: ConnectionStatus
    suspend fun initAndGetStatus(): ConnectionStatus
}

enum class ConnectionStatus {
    CONNECTED, OFFLINE, CONNECTED_NO_INTERNET;

    val isOnline: Boolean get() = this == CONNECTED
    val isConnected: Boolean get() = isOnline || this == CONNECTED_NO_INTERNET
}