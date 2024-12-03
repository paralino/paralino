package app.paralino.core.domain.location

import app.paralino.core.logg
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.raw.LocationFocusPayload
import app.paralino.core.models.raw.LocationRequestPayload
import app.paralino.core.presentation.map.MapHomeScreenFocusType
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.api.runAsync
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.utils.exceptions.BaseRemoteException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.seconds

class LocationRequestUseCase constructor(
    private val functionsApi: IFunctionsApi,
    private val locationPerformanceManager: LocationPerformanceManager
) : BaseRepository() {

    fun keepLocationBoost(
        groupId: String
    ): Flow<Unit> {
        return flow {
            do {
                emit(Unit)
                locationPerformanceManager.locationBoostRequested(
                    performanceType = LocationPerformanceType.HIGH,
                    groupId = groupId,
                    from = "map focus ping"
                )
                delay(20_000L)
            } while (currentCoroutineContext().isActive)
        }.catch { emit(Unit) }
    }

    fun keepLocationFocus(
        groupId: String,
        focusType: MapHomeScreenFocusType
    ): Flow<String> {
        return flow {
            do {
                emit("User is looking at map")
                sendLocationFocusEvent(groupId, focusType)
                val delayMillis = when (focusType) {
                    is MapHomeScreenFocusType.MEMBER -> 5_000L
                    else -> 20_000L
                }
                delay(delayMillis)
            } while (currentCoroutineContext().isActive)
        }.catch { emit("Error while boosting location") }
    }

    private suspend fun sendLocationFocusEvent(groupId: String, focusType: MapHomeScreenFocusType) {
        triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "location_performance_location_focus_${groupId}_${(focusType as? MapHomeScreenFocusType.MEMBER)?.memberUserId}",
                timeMillis = when (focusType) {
                    is MapHomeScreenFocusType.MEMBER -> 20.seconds.inWholeMilliseconds
                    else -> 60.seconds.inWholeMilliseconds
                },
                failOnError = false
            )
        ) {
            logg.tag("location_performance")
                .v("Map is active (${focusType::class.simpleName}), sending ping")
            try {
                val requestPayload = LocationFocusPayload(
                    groupId = groupId,
                    userId = (focusType as? MapHomeScreenFocusType.MEMBER)?.memberUserId,
                    locationId = (focusType as? MapHomeScreenFocusType.MEMBER)?.memberLocationId
                )

                functionsApi.runAsync(
                    function = ServerFunction.LOCATION_FOCUS,
                    data = requestPayload
                )
            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun requestMemberDeviceLocation(
        groupId: String,
        memberUserId: String,
        locationId: String
    ) {
        triggerRemoteOnly {
            val requestPayload = LocationRequestPayload(
                groupId = groupId,
                userId = memberUserId,
                locationId = locationId
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.LOCATION_REQUEST,
                data = requestPayload
            )

            if (!response.isSuccess) {
                throw BaseRemoteException.fromRemote(
                    code = response.statusCode,
                    error = response.errorResponse
                )
            }
        }
    }
}