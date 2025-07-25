package app.paralino.core.domain.location

import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.raw.LocationFocusGroupPayload
import app.paralino.core.models.raw.LocationFocusSpacePayload
import app.paralino.core.models.raw.LocationRequestGroupPayload
import app.paralino.core.models.raw.LocationRequestSpacePayload
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
        groupId: GroupOrSpaceId
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
        groupId: GroupOrSpaceId,
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

    private suspend fun sendLocationFocusEvent(groupId: GroupOrSpaceId, focusType: MapHomeScreenFocusType) {
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
            try {
                when (focusType) {
                    is MapHomeScreenFocusType.MEMBER -> {
                        if (focusType.isIdleActive) {
                            logg.tag("location_performance")
                                .v("Map is active (${focusType::class.simpleName}), sending ping")

                            if (groupId.isPersonalSpace) {
                                val requestPayload = LocationFocusSpacePayload(
                                    locationId = focusType.memberLocationId
                                )

                                functionsApi.runAndWait(
                                    function = ServerFunction.LOCATION_FOCUS_DEVICE,
                                    data = requestPayload
                                )
                            } else {
                                val requestPayload = LocationFocusGroupPayload(
                                    groupId = groupId.id,
                                    userId = focusType.memberUserId,
                                    locationId = focusType.memberLocationId
                                )

                                functionsApi.runAsync(
                                    function = ServerFunction.LOCATION_FOCUS_MEMBER,
                                    data = requestPayload
                                )
                            }
                        } else {
                            logg.tag("location_performance")
                                .v("User not sharing, skip ping")
                        }
                    }
                    else -> {
                        logg.tag("location_performance")
                            .v("Map is active (${focusType::class.simpleName}), sending ping")

                        if (groupId.isPersonalSpace) {
                            val requestPayload = LocationFocusSpacePayload(
                                locationId = null
                            )

                            functionsApi.runAndWait(
                                function = ServerFunction.LOCATION_FOCUS_SPACE,
                                data = requestPayload
                            )
                        } else {
                            val requestPayload = LocationFocusGroupPayload(
                                groupId = groupId.id,
                                userId = null,
                                locationId = null
                            )

                            functionsApi.runAsync(
                                function = ServerFunction.LOCATION_FOCUS_GROUP,
                                data = requestPayload
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                throw e
            }

            Unit
        }
    }

    suspend fun requestMemberDeviceLocation(
        groupId: GroupOrSpaceId,
        memberUserId: String,
        locationId: String
    ) {
        triggerRemoteOnly {
            val response = if (groupId.isPersonalSpace) {
                val requestPayload = LocationRequestSpacePayload(
                    locationId = locationId
                )

                functionsApi.runAndWait(
                    function = ServerFunction.LOCATION_REQUEST_DEVICE,
                    data = requestPayload
                )
            } else {
                val requestPayload = LocationRequestGroupPayload(
                    groupId = groupId.id,
                    userId = memberUserId,
                    locationId = locationId
                )

                functionsApi.runAndWait(
                    function = ServerFunction.LOCATION_REQUEST_MEMBER,
                    data = requestPayload
                )
            }

            if (!response.isSuccess) {
                throw BaseRemoteException.fromRemote(
                    code = response.statusCode,
                    error = response.errorResponse
                )
            }
        }
    }
}