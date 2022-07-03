package app.paralino.mobile.utils

import app.paralino.core.utils.ILocationStatusProvider
import app.paralino.core.utils.LocationConditionStatus
import app.paralino.mobile.AppClass
import app.paralino.mobile.tracking.getLocationStatus
import kotlinx.coroutines.flow.MutableStateFlow

class LocationStatusProvider(
    appClass: AppClass
) : ILocationStatusProvider {
    override var status: MutableStateFlow<LocationConditionStatus> = MutableStateFlow(
        LocationConditionStatus.getLocationStatus(appClass)
    )
}