package com.zbapp.locationtrack.utils

import com.zbapp.core.utils.ILocationStatusProvider
import com.zbapp.core.utils.LocationConditionStatus
import com.zbapp.locationtrack.AppClass
import com.zbapp.locationtrack.tracking.getLocationStatus
import kotlinx.coroutines.flow.MutableStateFlow

class LocationStatusProvider(
    appClass: AppClass
) : ILocationStatusProvider {
    override var status: MutableStateFlow<LocationConditionStatus> = MutableStateFlow(
        LocationConditionStatus.getLocationStatus(appClass)
    )
}