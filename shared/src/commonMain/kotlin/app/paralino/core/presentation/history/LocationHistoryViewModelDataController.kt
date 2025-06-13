package app.paralino.core.presentation.history

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.GetLocationHistoryUseCase
import app.paralino.core.domain.location.RefreshLocationHistoryUseCase
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class LocationHistoryViewModelDataController(
    val getLocationHistory: GetLocationHistoryUseCase,
    val refreshLocationHistory: RefreshLocationHistoryUseCase,
    val userRepo: UserRepository,
    val userGroupRepo: UserGroupLocalDataSource,
    val valueFormatter: IValueFormatter,
    val appPrefs: AppPrefs,
    val remoteConfig: RemoteConfigRepository,
    val viewModel: LocationHistoryViewModel
) {
    val _uiState = MutableStateFlow<LocationHistoryScreenState>(
        LocationHistoryScreenState(
            groupId = "",
            memberUserId = "",
            groupName = null,
            groupImage = null,
            userName = null,
            userImage = null,
            isCurrentUser = false,
            isPersonalHistory = false,
            historyDaysLimit = null,
            isGroupOwner = false,
            dateRangeDays = 1,
            startDate = nowUTC().toLocalDateTime(TimeZone.UTC).date,
            endDate = nowUTC().toLocalDateTime(TimeZone.UTC).date,
            isDataMissing = false,
            isLoadingData = true,
            isLoadingLocations = true,
            locations = emptyList(),
            devices = emptyList(),
            selectedLocationIndex = null,
            mapFocusType = LocationHistoryMapFocusType.None,
            isSatelliteMode = false,
            alertDialog = null
        )
    )

    val _eventChannel = Channel<LocationHistoryScreenEvent?>()

    val dataFetchController = LocationHistoryDataFetchController(
        dataController = this
    )
}