package app.paralino.core.domain.location

import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.map.MapHomeScreenFocusType
import app.paralino.core.models.raw.FunctionExecutionRaw
import app.paralino.core.models.raw.LocationFocusGroupPayload
import app.paralino.core.models.raw.LocationFocusSpacePayload
import app.paralino.core.models.raw.LocationRequestGroupPayload
import app.paralino.core.models.raw.LocationRequestSpacePayload
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.api.runAsync
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LocationRequestUseCase constructor(
    private val functionsApi: IFunctionsApi,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val personalSpaceLocal: UserPersonalSpaceLocalDataSource,
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
        focusType: MapHomeScreenFocusType,
        planConfig: PlanConfig
    ): Flow<String> {
        return flow {
            do {
                emit("User is looking at map")
                sendLocationFocusEvent(
                    groupId = groupId,
                    focusType = focusType,
                    planConfig = planConfig
                )
                val delayDuration = when (focusType) {
                    is MapHomeScreenFocusType.MEMBER -> {
                        when (planConfig.planTypeEnum.isPremium) {
                            true -> 5.seconds
                            false -> 20.seconds
                        }
                    }
                    else -> {
                        when (planConfig.planTypeEnum.isPremium) {
                            true -> 20.seconds
                            false -> 40.seconds
                        }
                    }
                }
                delay(delayDuration)
            } while (currentCoroutineContext().isActive)
        }.catch { emit("Error while boosting location") }
    }

    private suspend fun sendLocationFocusEvent(
        groupId: GroupOrSpaceId,
        focusType: MapHomeScreenFocusType,
        planConfig: PlanConfig
    ) {
        triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "location_performance_location_focus_${groupId}_${(focusType as? MapHomeScreenFocusType.MEMBER)?.memberUserId}",
                timeMillis = when (focusType) {
                    is MapHomeScreenFocusType.MEMBER -> {
                        when (planConfig.planTypeEnum.isPremium) {
                            true -> 20.seconds.inWholeMilliseconds
                            false -> 1.minutes.inWholeMilliseconds
                        }
                    }
                    else -> {
                        when (planConfig.planTypeEnum.isPremium) {
                            true -> 1.minutes.inWholeMilliseconds
                            false -> 3.minutes.inWholeMilliseconds
                        }
                    }
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

                                functionsApi.runAndWait(
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
                        if (groupId.isPersonalSpace) {
                            if (personalSpaceLocal.personalSpaceState.value.data?.areOtherDevicesActive == true) {
                                logg.tag("location_performance")
                                    .v("Map is active (${focusType::class.simpleName}), sending ping")
                                val requestPayload = LocationFocusSpacePayload(
                                    locationId = null
                                )

                                functionsApi.runAndWait(
                                    function = ServerFunction.LOCATION_FOCUS_SPACE,
                                    data = requestPayload
                                )
                            } else {
                                logg.tag("location_performance")
                                    .v("No active devices in personal space, skip ping")
                            }
                        } else {
                            logg.tag("location_performance")
                                .v("Map is active (${focusType::class.simpleName}), sending ping")

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
            suspend fun makeRequest(isAlternatePath: Boolean): FunctionExecutionRaw {
                return if (groupId.isPersonalSpace) {
                    val requestPayload = LocationRequestSpacePayload(
                        locationId = locationId
                    )

                    functionsApi.runAndWait(
                        function = if (isAlternatePath) {
                            ServerFunction.LOCATION_REQUEST_DEVICE_ALT
                        } else {
                            ServerFunction.LOCATION_REQUEST_DEVICE
                        },
                        data = requestPayload
                    )
                } else {
                    val requestPayload = LocationRequestGroupPayload(
                        groupId = groupId.id,
                        userId = memberUserId,
                        locationId = locationId
                    )

                    functionsApi.runAndWait(
                        function = if (isAlternatePath) {
                            ServerFunction.LOCATION_REQUEST_MEMBER_ALT
                        } else {
                            ServerFunction.LOCATION_REQUEST_MEMBER
                        },
                        data = requestPayload
                    )
                }
            }

            val responseOne = makeRequest(isAlternatePath = false)

            if (!responseOne.isSuccess) {
                val response = if (responseOne.statusCode in listOf(500, 502, 503, 504)) {
                    makeRequest(isAlternatePath = true)
                } else {
                    responseOne
                }

                if (!responseOne.isSuccess) {
                    throw response.remoteException
                }
            }
        }
    }
}