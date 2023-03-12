package app.paralino.core.domain.user

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.ILimitedExecution
import app.paralino.core.domain.group.GroupRefreshUseCase
import app.paralino.core.logg
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.*

class RefreshEverythingUseCase(
    private val placesRepo: PlacesRepository,
    private val remote: GroupsRemoteSource,
    private val local: UserGroupLocalDataSource,
    private val userRepository: UserRepository,
    private val personalCodesRepo: PersonalCodesRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val groupRefreshUseCase: GroupRefreshUseCase,
    private val configRepo: AppConfigRepository
) : BaseRepository(), ILimitedExecution<String> {

    override val executionState = MutableStateFlow(listOf<String>())
    private val executionKey = "full_refresh"

    fun isProcessing(): Boolean {
        return isProcessing(executionKey)
    }

    suspend fun refreshEverything(
       force: Boolean = false
    ) {

        if (isProcessing(executionKey)) {
            logg.tag("full_refresh").v("Processing, can't refresh data")
            throw IllegalStateException("data already in progress")
        }

        setAsProcessing(executionKey)

        logg.tag("full_refresh").v("Start full refresh")
        try {
            userRepository.refreshUserIfAvailable()
            personalCodesRepo.refreshPersonalCodes()
            userRepository.refreshBlocklist()
            configRepo.refreshConfig()
            scheduleRefreshAllGroupsWithPlaces(
                key = executionKey,
                time = if(force) 0 else 60_000
            )
        } catch (e: Exception) {
            logg.tag("full_refresh").e("Error with full refresh", e)
        }

        logg.tag("full_refresh").v("Full refresh done")
        stopProcessing(executionKey)
    }

    suspend fun scheduleRefreshAllGroupsWithPlaces(
        key: String = "groups_refresh",
        time: Long = 15_000
    ) {
        getDataOnce(
            local = flowOf(key),
            remotePreference = RemotePreference.CACHE(key = key, timeMillis = time)
        ) {
            refreshAllGroupsWithPlaces()
        }
            .catch { }
            .collect()
    }

    private suspend fun refreshAllGroupsWithPlaces() {
        refreshAllGroups()
        placesRepo.refreshAllPlaces()
    }

    private suspend fun refreshAllGroups() {
        triggerRemoteOnly {
            logg.tag("full_refresh").w("refreshing all groups")
            remote.getAllGroups()
                .also { cryptoKEKRepo.refreshAllKeys().first() }
                .let { it.map { groupRefreshUseCase.handleGroupKeysUpdate(it) } }
        }
            .also {
                logg.tag("full_refresh").d("group and keys ready, updating groups")
                local.recreateGroups(it)
            }
    }

}