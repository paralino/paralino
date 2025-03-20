package app.paralino.core.domain.location

import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.repo.location.LocationLocalDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LocationCacheRepository() {
    private val _lastLocation = MutableStateFlow<LocationDataModel?>(null)
    val lastLocationFlow: StateFlow<LocationDataModel?> = _lastLocation.asStateFlow()
    val lastLocation: LocationDataModel? get() = lastLocationFlow.value

    fun updateCache(data: LocationDataModel) {
        _lastLocation.update { data }
    }

    fun clearCache() {
        _lastLocation.update { null }
    }
}