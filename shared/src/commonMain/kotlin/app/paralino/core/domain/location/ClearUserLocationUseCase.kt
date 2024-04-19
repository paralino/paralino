package app.paralino.core.domain.location

import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.repo.location.LocationRemoteSource
import kotlinx.coroutines.flow.Flow

class ClearUserLocationUseCase(
    private val local: LocationLocalDataSource,
    private val remote: LocationRemoteSource,
) : BaseRepository() {
    fun clearLocation(locationId: String): Flow<Unit> {
        return getDataOnce(
            remote = { remote.clearLocation(locationId) },
            localUpdate = { local.clearMemberLocationData(locationId = locationId) }
        )
    }
}