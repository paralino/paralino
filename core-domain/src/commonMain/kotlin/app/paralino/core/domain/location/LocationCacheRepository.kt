package app.paralino.core.domain.location

import app.paralino.core.models.location.LocationUpdateDataFromDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LocationCacheRepository() {
    private val _lastLocation = MutableStateFlow<LocationUpdateDataFromDevice?>(null)
    val lastLocationFlow: StateFlow<LocationUpdateDataFromDevice?> = _lastLocation.asStateFlow()
    val lastLocation: LocationUpdateDataFromDevice? get() = lastLocationFlow.value

    fun updateCache(data: LocationUpdateDataFromDevice) {
        _lastLocation.update { data }
    }

    fun clearCache() {
        _lastLocation.update { null }
    }
}