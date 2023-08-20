package app.paralino.android.provider

import android.content.Context
import app.paralino.android.tracking.getLocationStatus
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.provider.LocationConditionStatus

class LocationStatusProvider(
    private val context: Context
) : ILocationStatusProvider() {

    init {
        refreshLocationStatus()
    }

    override fun createCurrentStatus(): LocationConditionStatus {
        return LocationConditionStatus.getLocationStatus(context)
    }
}