package app.paralino.core.domain.user

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.base.PlanType
import app.paralino.core.models.data
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationPerformanceSettingsType
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.days

class PremiumStatusManager(
    private val userRepo: UserRepository,
    private val remoteConfigRepo: RemoteConfigRepository,
    private val locationSharingManager: LocationSharingManager,
    private val groupLocalDataSource: UserGroupLocalDataSource,
    private val appPrefs: AppPrefs,
) {

    private val _hasGroupOverLimits = MutableStateFlow(false)
    val hasGroupOverLimits = _hasGroupOverLimits

    suspend fun adjustDeviceSettingsForPlan() {
        val config = remoteConfigRepo.getConfigOnce()
        val currentPlanConfig = userRepo.userPlans.value.highestValidPlanConfig.takeIf { config.plans.isNotEmpty() }

        if (currentPlanConfig?.planTypeEnum == PlanType.FREE) {
            if (LocationPerformanceSettingsType.HighPerformance.fromKey(appPrefs.locationSettingsHighPerformanceDurationType).isPremiumFeature) {
                appPrefs.locationSettingsHighPerformanceDurationType = LocationPerformanceSettingsType.HighPerformance.default.key
            }

            if (LocationPerformanceSettingsType.Balanced.fromKey(appPrefs.locationSettingsBalancedPerformanceType).isPremiumFeature) {
                appPrefs.locationSettingsBalancedPerformanceType = LocationPerformanceSettingsType.Balanced.default.key
            }

            if (LocationPerformanceSettingsType.BatterySaver.fromKey(appPrefs.locationSettingsBatterySaverPerformanceType).isPremiumFeature) {
                appPrefs.locationSettingsBatterySaverPerformanceType = LocationPerformanceSettingsType.BatterySaver.default.key
            }
        }

        val groupsDataForOwner = groupLocalDataSource.allGroupsState.value.data?.filter { it.group.isCurrentUserOwner } ?: emptyList()
        val groupLimit = userRepo.userPlans.value.groupsLimit.takeIf { config.plans.isNotEmpty() } ?: Int.MAX_VALUE

        val overLimits = groupsDataForOwner.map {
            if (it.isGroupOverLimits(config) || groupsDataForOwner.size > groupLimit) {
                val contract = locationSharingManager.currentSharingContractForGroup(id = it.group.id)
                val newContract = contract?.asValidContract(
                    sharingLimitDuration = 5.days
                )
                if (newContract != null) {
                    locationSharingManager.startSharing(newContract)
                }
                true
            } else {
                false
            }
        }

        hasGroupOverLimits.update { overLimits.any { it } }
    }

    fun resetLimitState() {
        hasGroupOverLimits.update { false }
    }
}