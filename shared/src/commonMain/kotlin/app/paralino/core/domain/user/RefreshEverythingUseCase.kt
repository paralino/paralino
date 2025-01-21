package app.paralino.core.domain.user

import app.paralino.core.crypto.group.CryptoKEKRefreshUseCase
import app.paralino.core.domain.LimitedExecutionParentManager
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.domain.group.GroupRefreshUseCase
import app.paralino.core.domain.location.MemberDeviceStatusProcessor
import app.paralino.core.logg
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.places.PlaceActivityUploadEngine
import app.paralino.core.repo.user.IPushProviderManager
import app.paralino.core.repo.user.UserConfigRepository
import app.paralino.core.repo.user.UserRepository
import kotlin.time.Duration.Companion.minutes

class RefreshEverythingUseCase(
    private val remote: GroupsRemoteSource,
    private val local: UserGroupLocalDataSource,
    private val userRepository: UserRepository,
    private val personalCodesRepo: PersonalCodesRepository,
    private val cryptoKEKRefreshUseCase: CryptoKEKRefreshUseCase,
    private val groupRefreshUseCase: GroupRefreshUseCase,
    private val configRepo: RemoteConfigRepository,
    private val userConfigRepo: UserConfigRepository,
    private val deviceStatusProcessor: MemberDeviceStatusProcessor,
    private val placeActivityUploadEngine: PlaceActivityUploadEngine,
    private val pushProviderManager: IPushProviderManager,
    private val premiumStatusManager: PremiumStatusManager,
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
                    userRepository.refreshBlocklist(cacheTimeMillis = 2.minutes.inWholeMilliseconds)
                    configRepo.refreshConfig()
                    userConfigRepo.refreshAndGetConfig()
                    scheduleRefreshAllGroupsWithPlaces(
                        cacheTimeMillis = if (forceGroupRefresh) 0 else 60_000
                    )
                    pushProviderManager.syncPushNotifications()
                    personalCodesRepo.tryToRestockPreKeys(5.minutes.inWholeMilliseconds)
                    deviceStatusProcessor.revokeActiveStatus()
                    deviceStatusProcessor.deleteOldLocations()
                    premiumStatusManager.adjustDeviceSettingsForPlan()
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
        cacheTimeMillis: Long = 15_000
    ) {
        try {
            startProcessing(groupRefreshExecutionKey)
        } catch (e: Exception) {
            logg.tag("full_refresh").v("Processing, can't refresh data")
            return
        }

        try {
            triggerRemoteMaybe(
                remotePreference = RemotePreference.CACHE(key = groupRefreshExecutionKey, timeMillis = cacheTimeMillis),
                remote = { refreshAllGroupsWithPlaces() }
            )
            placeActivityUploadEngine.syncPendingEventsNow()
        } catch (e: Exception) {
            logg.tag("full_refresh").w("Error refreshing all groups - ${e.message}")
        } finally {
            stopProcessing(groupRefreshExecutionKey)
        }
    }

    private suspend fun refreshAllGroupsWithPlaces() {
        triggerRemoteOnly {
            logg.tag("full_refresh").d("refreshing all groups")
            remote.getAllGroups()
                .also { cryptoKEKRefreshUseCase.refreshAllKeys(it, force = false) }
                .let { it.map { groupRefreshUseCase.handleGroupKeysUpdate(it) } }
        }
            .also {
                logg.tag("full_refresh").d("group and keys ready, updating groups")
                local.recreateGroups(it)
            }
    }
}