package app.paralino.core.utils

import kotlinx.coroutines.flow.MutableStateFlow

interface IConnectionStatusProvider {
    val status: MutableStateFlow<ConnectionStatus>
    suspend fun initAndGetStatus(): ConnectionStatus
}

enum class ConnectionStatus {
    CONNECTED, OFFLINE, CONNECTED_NO_INTERNET;

    val isOnline: Boolean get() = this == CONNECTED
    val isConnected: Boolean get() = isOnline || this == CONNECTED_NO_INTERNET
}