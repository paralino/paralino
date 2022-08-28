package app.paralino.android.utils

import app.paralino.core.utils.ILocationStatusProvider
import app.paralino.core.utils.LocationConditionStatus
import app.paralino.android.AppClass
import app.paralino.android.tracking.getLocationStatus
import kotlinx.coroutines.flow.MutableStateFlow

class LocationStatusProvider(
    appClass: AppClass
) : ILocationStatusProvider {
    override var status: MutableStateFlow<LocationConditionStatus> = MutableStateFlow(
        LocationConditionStatus.getLocationStatus(appClass)
    )
}