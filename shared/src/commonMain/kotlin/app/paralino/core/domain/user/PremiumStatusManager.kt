package app.paralino.core.domain.user

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.PlanType
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationPerformanceSettingsType
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.days

class PremiumStatusManager(
    private val userRepo: UserRepository,
    private val remoteConfigRepo: RemoteConfigRepository,
    private val locationSharingManager: LocationSharingManager,
    private val groupLocalDataSource: UserGroupLocalDataSource,
    private val personalSpaceRepo: UserPersonalSpaceLocalDataSource,
    private val appPrefs: AppPrefs,
) {

    private val _hasGroupOverLimits = MutableStateFlow(false)
    val hasGroupOverLimits = _hasGroupOverLimits

    suspend fun adjustDeviceSettingsForPlan() {
        val config = remoteConfigRepo.getConfigOnce()
        val currentPlanConfig = userRepo.userPlans.value.highestValidPlanConfig.takeIf { config.plans.isNotEmpty() }
        val isUserPartOfPremiumGroup = groupLocalDataSource.allGroupsState.value.data?.any {
            it.group.isGroupPremium(remoteConfigRepo.getConfigOnce())
        } ?: false

        if (currentPlanConfig?.planTypeEnum == PlanType.FREE && !isUserPartOfPremiumGroup) {
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

        val personalSpaceOverLimit = personalSpaceRepo.personalSpaceState.value.data?.isSpaceOverLimits() == true

        if (personalSpaceOverLimit) {
            val contract = locationSharingManager.currentSharingContractForGroup(id = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId())
            val newContract = contract?.asValidContract(
                sharingLimitDuration = 5.days
            )
            if (newContract != null) {
                locationSharingManager.startSharing(newContract)
            }
        }

        val groupsDataForOwner = groupLocalDataSource.allGroupsState.value.data?.filter { it.group.isCurrentUserOwner } ?: emptyList()
        val groupLimit = userRepo.userPlans.value.groupsLimit.takeIf { config.plans.isNotEmpty() } ?: Int.MAX_VALUE
        val groupsOverLimits = groupsDataForOwner.map {
            if (it.isGroupOverLimits(config) || groupsDataForOwner.size > groupLimit) {
                val contract = locationSharingManager.currentSharingContractForGroup(id = it.group.id.asGroupOrSpaceId())
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
        }.any { it }

        hasGroupOverLimits.update {
            groupsOverLimits || personalSpaceOverLimit
        }
    }

    fun resetLimitState() {
        hasGroupOverLimits.update { false }
    }

    suspend fun checkPlaceCountLimits(groupId: GroupOrSpaceId): LimitCheckState {
        val (limit, placeCount, isGroupOwner) = if (groupId.isPersonalSpace) {
            val space = personalSpaceRepo.personalSpaceState.value.data ?: return LimitCheckState.Unknown
            Triple(space.plan.placesLimit, space.placesCount, true)
        } else {
            val group = groupLocalDataSource.allGroupsState.value.data
                ?.firstOrNull { it.group.id == groupId.id } ?: return LimitCheckState.Unknown
            val limit = group.group.placesLimit(remoteConfigRepo.getConfigOnce())
            Triple(limit, group.placesCount, group.group.isCurrentUserOwner)
        }

        val maxPlaceLimit = remoteConfigRepo.getConfigOnce()
            .plans
            .maxByOrNull { it.planTypeEnum.order }
            ?.placesLimit

        if (maxPlaceLimit != null && isGroupOwner && placeCount >= maxPlaceLimit) {
            return LimitCheckState.MaxLimitReached(limit = limit, isOwner = isGroupOwner)
        } else if (limit in 0..placeCount) {
            return LimitCheckState.OverLimits(limit = limit, isOwner = isGroupOwner)
        } else {
            return LimitCheckState.WithinLimits
        }
    }
}

sealed interface LimitCheckState {
    data class MaxLimitReached(val limit: Int, val isOwner: Boolean): LimitCheckState
    data class OverLimits(val limit: Int, val isOwner: Boolean): LimitCheckState
    data object WithinLimits: LimitCheckState
    data object Unknown: LimitCheckState
}