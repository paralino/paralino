package app.paralino.core.domain.user

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.LimitedExecutionParentManager
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.domain.group.GroupRefreshUseCase
import app.paralino.core.domain.location.MemberDeviceStatusProcessor
import app.paralino.core.logg
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.places.PlaceActivityUploadEngine
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.IPushProviderManager
import app.paralino.core.repo.user.UserRepository
import kotlin.time.Duration.Companion.minutes

class RefreshEverythingUseCase(
    private val placesRepo: PlacesRepository,
    private val remote: GroupsRemoteSource,
    private val local: UserGroupLocalDataSource,
    private val userRepository: UserRepository,
    private val personalCodesRepo: PersonalCodesRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val groupRefreshUseCase: GroupRefreshUseCase,
    private val configRepo: AppConfigRepository,
    private val deviceStatusProcessor: MemberDeviceStatusProcessor,
    private val placeActivityUploadEngine: PlaceActivityUploadEngine,
    private val pushProviderManager: IPushProviderManager,
    private val parentManager: LimitedExecutionParentManager
) : LimitedExecutionRepository() {

    private val executionKey = "full_refresh"
    private val groupRefreshExecutionKey = "full_refresh_groups_refresh"


    override fun isProcessing(key: String): Boolean {
        return super.isProcessing(key) || parentManager.isProcessingAny()
    }

    suspend fun refreshEverything(
        forceGroupRefresh: Boolean = false,
        minimumDelayMillis: Long = 0
    ) {

        try {
            startProcessing(executionKey)
        } catch (e: Exception) {
            logg.tag("full_refresh").v("Processing, can't refresh data")
            return
        }

        logg.tag("full_refresh").v("Try full refresh with delay $minimumDelayMillis")
        try {
            triggerRemoteMaybe(
                remotePreference = RemotePreference.CACHE(
                    key = executionKey,
                    timeMillis = minimumDelayMillis
                ),
                remote = {
                    logg.tag("full_refresh").v("Start full refresh")
                    userRepository.refreshUserIfAvailable()
                    personalCodesRepo.refreshPersonalCodes()
                    userRepository.refreshBlocklist()
                    configRepo.refreshConfig()
                    scheduleRefreshAllGroupsWithPlaces(
                        time = if (forceGroupRefresh) 0 else 60_000
                    )
                    pushProviderManager.syncPushNotifications()
                    personalCodesRepo.tryToRestockPreKeys(5.minutes.inWholeMilliseconds)
                    deviceStatusProcessor.revokeActiveStatus()
                    deviceStatusProcessor.deleteOldLocations()
                }
            )
        } catch (e: Exception) {
            logg.tag("full_refresh").e("Error with full refresh", e)
        } finally {
            logg.tag("full_refresh").v("Full refresh done")
            stopProcessing(executionKey)
        }
    }

    suspend fun scheduleRefreshAllGroupsWithPlaces(
        time: Long = 15_000
    ) {
        try {
            startProcessing(groupRefreshExecutionKey)
        } catch (e: Exception) {
            logg.tag("full_refresh").v("Processing, can't refresh data")
            return
        }

        try {
            triggerRemoteMaybe(
                remotePreference = RemotePreference.CACHE(key = groupRefreshExecutionKey, timeMillis = time),
                remote = { refreshAllGroupsWithPlaces() }
            )
        } catch (e: Exception) {
            logg.tag("full_refresh").w("Error refreshing all groups - ${e.message}")
        } finally {
            stopProcessing(groupRefreshExecutionKey)
        }
    }

    private suspend fun refreshAllGroupsWithPlaces() {
        refreshAllGroups()
        placesRepo.refreshAllPlaces()
    }

    private suspend fun refreshAllGroups() {
        triggerRemoteOnly {
            logg.tag("full_refresh").d("refreshing all groups")
            remote.getAllGroups()
                .also { cryptoKEKRepo.refreshAllKeys() }
                .let { it.map { groupRefreshUseCase.handleGroupKeysUpdate(it) } }
        }
            .also {
                logg.tag("full_refresh").d("group and keys ready, updating groups")
                local.recreateGroups(it)
            }
            .also {
                placeActivityUploadEngine.syncPendingEvents()
            }
    }
}