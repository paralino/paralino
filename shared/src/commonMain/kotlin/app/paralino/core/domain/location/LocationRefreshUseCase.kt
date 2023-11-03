package app.paralino.core.domain.location

import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.logg
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.raw.LocationRequestPayload
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.IRealtimeApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.utils.exceptions.BaseRemoteException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

class LocationRefreshUseCase constructor(
    private val realtimeApi: IRealtimeApi,
    private val realtimeEventsProcessor: RealtimeEventsProcessor,
    private val groupPreRefresh: GroupPreRefreshUseCase,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig
) {

    fun locationRefresh(groupId: String): Flow<Unit> {
        logg.tag("location_refresh").v("Start realtime subscription for group $groupId")
        val locationSubscriptionFlow = realtimeApi.subscribeWithFilter(
            channels = listOf(
                realtimeEventsProcessor.membersChannel,
                realtimeEventsProcessor.locationsChannel
            ),
            filters = listOf(
                "${realtimeEventsProcessor.membersChannel}.*",
                "${realtimeEventsProcessor.locationsChannel}.*"
            )
        )
            .onStart {
                logg.tag("location_refresh").v("Try refreshing group first")
                groupPreRefresh.refreshGroupWithMembers(groupId)
            }
            .onEach {
                realtimeEventsProcessor.processEvent(it)
            }
            .catch { logg.tag("realtime_events").e("Error parsing member or location on map", it) }

        val userMapFocusFlow = locationPerformanceManager.boostOnMapFlow(groupId)

        return combine(
            locationSubscriptionFlow,
            userMapFocusFlow
        ) { _, _ ->
            Unit
        }
    }

    fun refreshMemberDeviceLocation(
        groupId: String,
        memberUserId: String,
        locationId: String
    ): Flow<Unit> {
        return flow {

            val requestPayload = LocationRequestPayload(
                groupId = groupId,
                userId = memberUserId,
                locationId = locationId,
                force = false
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.LOCATION_REQUEST,
                data = requestPayload
            )

            if (!response.isSuccess) {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    code = response.statusCode,
                    message = "Unable to request location update",
                    cause = Exception(response.stderr)
                )
            }
        }
    }
}