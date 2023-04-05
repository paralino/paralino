package app.paralino.android.provider

import app.paralino.android.AppClass
import app.paralino.android.tracking.getLocationStatus
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.provider.LocationConditionStatus
import kotlinx.coroutines.flow.MutableStateFlow

class LocationStatusProvider(
    private val appClass: AppClass
) : ILocationStatusProvider() {
    override fun getCurrentStatus(): LocationConditionStatus {
        return LocationConditionStatus.getLocationStatus(appClass)
    }
}