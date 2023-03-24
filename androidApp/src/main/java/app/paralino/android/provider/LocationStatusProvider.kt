package app.paralino.android.provider

import app.paralino.android.AppClass
import app.paralino.android.tracking.getLocationStatus
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.provider.LocationConditionStatus
import kotlinx.coroutines.flow.MutableStateFlow

class LocationStatusProvider(
    appClass: AppClass
) : ILocationStatusProvider {
    override val status: MutableStateFlow<LocationConditionStatus> = MutableStateFlow(
        LocationConditionStatus.getLocationStatus(appClass)
    )
}