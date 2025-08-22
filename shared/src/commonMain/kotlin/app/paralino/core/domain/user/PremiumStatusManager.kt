package app.paralino.core.domain.user

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.PlanConfig
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

    suspend fun checkPlaceCountLimits(groupId: GroupOrSpaceId): PremiumStatusLimitState? {
        val payload = if (groupId.isPersonalSpace) {
            val space = personalSpaceRepo.personalSpaceState.value.data ?: return null
            PremiumStatusLimitPayload(
                plan = space.plan,
                limit = space.plan.personalPlacesLimit.takeIf { it >= 0 },
                count = space.placesCount,
                isOwner = true
            )
        } else {
            val group = groupLocalDataSource.allGroupsState.value.data
                ?.firstOrNull { it.group.id == groupId.id } ?: return null
            PremiumStatusLimitPayload(
                plan = group.group.planConfig(remoteConfigRepo.getConfigOnce()),
                limit = group.group.placesLimit(remoteConfigRepo.getConfigOnce()),
                count = group.placesCount,
                isOwner = group.group.isCurrentUserOwner
            )

        }

        val maxPlaceLimit = remoteConfigRepo.getConfigOnce()
            .plans
            .maxByOrNull { it.planTypeEnum.order }
            ?.let {
                if (groupId.isPersonalSpace) it.personalPlacesLimit
                else it.placesLimit
            }?.takeIf { it >= 0 }

        if (maxPlaceLimit != null && payload.isOwner && payload.count >= maxPlaceLimit) {
            return PremiumStatusLimitState.ServiceLimitReached(payload)
        } else if (payload.limit != null && payload.limit in 0..payload.count) {
            return PremiumStatusLimitState.PlanLimitReached(payload)
        } else {
            return PremiumStatusLimitState.WithinLimits(payload)
        }
    }

    suspend fun checkDeviceCountLimits(groupId: GroupOrSpaceId): PremiumStatusLimitState? {
        val payload = if (groupId.isPersonalSpace) {
            val space = personalSpaceRepo.personalSpaceState.value.data ?: return null
            PremiumStatusLimitPayload(
                plan = space.plan,
                limit = space.plan.personalDevicesLimit,
                count = space.activeDeviceCount,
                isOwner = true
            )
        } else {
            val group = groupLocalDataSource.allGroupsState.value.data
                ?.firstOrNull { it.group.id == groupId.id } ?: return null
            val limit = group.group.devicesLimit(remoteConfigRepo.getConfigOnce())
            PremiumStatusLimitPayload(
                plan = group.group.planConfig(remoteConfigRepo.getConfigOnce()),
                limit = limit,
                count = group.devicesCount,
                isOwner = group.group.isCurrentUserOwner
            )
        }

        val maxDeviceLimit = remoteConfigRepo.getConfigOnce()
            .plans
            .maxByOrNull { it.planTypeEnum.order }
            ?.let {
                if (groupId.isPersonalSpace) {
                    it.personalDevicesLimit
                } else {
                    it.devicesLimit
                }
            }

        if (maxDeviceLimit != null && payload.isOwner && payload.count >= maxDeviceLimit) {
            return PremiumStatusLimitState.ServiceLimitReached(payload)
        } else if (payload.limit in 0..payload.count) {
            return PremiumStatusLimitState.PlanLimitReached(payload)
        } else {
            return PremiumStatusLimitState.WithinLimits(payload)
        }
    }
}

data class PremiumStatusLimitPayload(
    val plan: PlanConfig,
    val limit: Int?,
    val count: Int,
    val isOwner: Boolean
) {
    val isOverLimit = if (limit == null || limit < 0) false else count > limit
}

sealed interface PremiumStatusLimitState {
    val payload: PremiumStatusLimitPayload
    data class ServiceLimitReached(override val  payload: PremiumStatusLimitPayload): PremiumStatusLimitState
    data class PlanLimitReached(override val  payload: PremiumStatusLimitPayload): PremiumStatusLimitState
    data class WithinLimits(override val  payload: PremiumStatusLimitPayload): PremiumStatusLimitState
}