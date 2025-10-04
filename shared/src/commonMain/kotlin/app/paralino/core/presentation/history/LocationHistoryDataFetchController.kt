package app.paralino.core.presentation.history

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.history.GetLocationHistoryUseCase
import app.paralino.core.domain.location.history.RefreshLocationHistoryUseCase
import app.paralino.core.logg
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.presentation.common.ICoreMessageDispatcher
import app.paralino.core.provider.Logr
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.listOfDatesUntil
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes

class LocationHistoryDataFetchController(
    private val getLocationHistory: GetLocationHistoryUseCase,
    private val refreshLocationHistory: RefreshLocationHistoryUseCase,
    private val personalSpaceRepo: UserPersonalSpaceLocalDataSource,
    private val userGroupRepo: UserGroupLocalDataSource,
    private val appPrefs: AppPrefs,
    private val historyDateMapper: LocationHistoryDateMapper,
    private val personalStateMapper: LocationHistoryPersonalStateMapper,
    private val groupStateMapper: LocationHistoryGroupStateMapper,
    private val _uiState: MutableStateFlow<LocationHistoryScreenState>,
    private val messageDispatcher: ICoreMessageDispatcher,
    private val coroutineScope: CoroutineScope
) {
    fun loadHistory(
        groupId: String,
        memberUserId: String
    ): Flow<Unit> {
        return flow {
            val selectedDate = nowUTC().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val isPersonalHistory = groupId == UserGroup.groupPersonalDevicesId
            val isCurrentUser = memberUserId.isBlank() || memberUserId == appPrefs.userId

            val newState = _uiState.value.copy(
                groupOrSpaceId = groupId.asGroupOrSpaceId(),
                memberUserId = memberUserId.takeIf { it.isNotBlank() } ?: appPrefs.userId,
                isPersonalHistory = isPersonalHistory,
                isCurrentUser = isCurrentUser,
                isGroupOwner = false,
                selectedDate = selectedDate,
                locations = emptyList(),
                devices = emptyList(),
                isDeletingEntry = false,
                deletionRangeStartIndex = null,
                isLoadingLocations = true,
                isLoadingData = true,
                stats = null,
                daySelectorState = historyDateMapper.createDateSelector(
                    groupId = groupId,
                    selectedDate = selectedDate,
                    dateRangeDays = 1,
                    daysInLoading = listOf(selectedDate)
                ),
                selectedLocationId = null,
            )

            _uiState.value = newState

            emitAll(
                reload(
                    startDate = newState.startDate,
                    endDate = newState.endDate
                )
            )
        }
    }

    fun reload(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Unit> {
        val groupId = _uiState.value.groupOrSpaceId
        val memberUserId = _uiState.value.memberUserId

        val job = if (groupId.isPersonalSpace) {
            fetchPersonalLocationHistory(
                startDate = startDate,
                endDate = endDate
            )
        } else {
            fetchGroupLocationHistory(
                groupId = groupId.id,
                memberUserId = memberUserId,
                startDate = startDate,
                endDate = endDate
            )
        }

        coroutineScope.launch {
            try {
                refreshLocationHistory.refreshLocationHistory(
                    groupId = groupId,
                    memberUserId = memberUserId,
                    locationId = null,
                    startDate = startDate,
                    endDate = endDate,
                    requestCacheDuration = 10.minutes
                )
            } catch (e: Exception) {
                logg.e(message = "Error refreshing location history: ${e.message}", t = e)
                messageDispatcher.showSnackbar(text = e.errorString())
            }

            _uiState.update {
                it.copy(
                    daySelectorState = it.daySelectorState.copy(
                        daysInLoading = it.daySelectorState.daysInLoading - startDate.listOfDatesUntil(
                            endDate
                        )
                    )
                )
            }
        }

        return job
    }

    private fun fetchPersonalLocationHistory(startDate: LocalDate, endDate: LocalDate): Flow<Unit> {
        return getLocationHistory.getLocationHistory(
            groupId = UserGroup.groupPersonalDevicesId,
            memberUserId = appPrefs.userId,
            startDate = startDate,
            endDate = endDate
        ).map { history ->
            val space = personalSpaceRepo.personalSpaceState.value.data
            if (space != null) {
                val newState = personalStateMapper.mapPersonalHistory(
                    space = space,
                    history = history,
                    startDate = startDate,
                    endDate = endDate,
                    previousState = _uiState.value
                )
                _uiState.update {
                    newState
                }
            } else {
                // TODO handle data missing
                _uiState.update {
                    it.copy(
                        isLoadingLocations = false
                    )
                }
            }
        }
            .catch {
                Logr.d("Error loading history", it)
            }
            .flowOn(Dispatchers.IO)
    }

    private fun fetchGroupLocationHistory(
        groupId: String,
        memberUserId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<Unit> {
        return getLocationHistory.getLocationHistory(
            groupId = groupId,
            memberUserId = memberUserId,
            startDate = startDate,
            endDate = endDate
        ).map { history ->
            val group = userGroupRepo.getGroupWithMembersOnce(groupId = groupId)
            val currentMember = group?.memberByUserId(userId = memberUserId)
            if (group != null && currentMember != null) {
                val newState = groupStateMapper.mapGroupHistory(
                    group = group,
                    currentMember = currentMember,
                    history = history,
                    startDate = startDate,
                    endDate = endDate,
                    previousState = _uiState.value
                )

                _uiState.update {
                    newState
                }
            } else {
                // TODO handle data missing
                _uiState.update {
                    it.copy(
                        isLoadingLocations = false
                    )
                }
            }
        }
            .catch {
                Logr.d("Error loading history", it)
            }
            .flowOn(Dispatchers.IO)
    }
}