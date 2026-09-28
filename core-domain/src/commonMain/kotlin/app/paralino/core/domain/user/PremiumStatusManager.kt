package app.paralino.core.domain.user

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.base.ConfigData
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.settings.MapsSDKProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationPerformanceSettingsType
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

class PremiumStatusManager(
    private val userRepo: UserRepository,
    private val remoteConfigRepo: RemoteConfigRepository,
    private val locationSharingManager: LocationSharingManager,
    private val groupLocalDataSource: UserGroupLocalDataSource,
    private val personalSpaceRepo: UserPersonalSpaceLocalDataSource,
    private val appPrefs: AppPrefs,
    private val appConfigRepo: AppConfigRepository
) {

    private val _hasGroupOverLimits = MutableStateFlow(false)
    val hasGroupOverLimits: StateFlow<Boolean> = _hasGroupOverLimits.asStateFlow()

    fun allGroupPlans(config: ConfigData): List<PlanConfig> {
        return groupLocalDataSource.allGroupsState.value.data
            ?.map { it.group.planConfig(config) }
            ?: emptyList()
    }

    fun highestGroupPlan(config: ConfigData): PlanConfig {
        val highestPlan = allGroupPlans(config)
            .maxByOrNull { it.planOrder }
            ?: config.defaultFreePlanConfig()
        return highestPlan
    }

    suspend fun adjustDeviceSettingsForPlan() {
        val config = remoteConfigRepo.getConfigOnce()
        val currentPlanConfig = userRepo.userPlans.value.highestValidPlanConfig.takeIf { config.plans.isNotEmpty() }
        val hasBestPerformanceFromGroup = allGroupPlans(config).any { it.hasBestLocationPerformance }

        if (currentPlanConfig?.hasBestLocationPerformance == false && !hasBestPerformanceFromGroup) {
            appPrefs.locationSettingsHighPerformanceDurationTypeFallback = null

            if (LocationPerformanceSettingsType.HighPerformance.fromKey(appPrefs.locationSettingsHighPerformanceDurationType).isPremiumFeature) {
                appPrefs.locationSettingsHighPerformanceDurationType = LocationPerformanceSettingsType.HighPerformance.default.key
            }

            if (LocationPerformanceSettingsType.Balanced.fromKey(appPrefs.locationSettingsBalancedPerformanceType).isPremiumFeature) {
                appPrefs.locationSettingsBalancedPerformanceType = LocationPerformanceSettingsType.Balanced.default.key
            }

            if (LocationPerformanceSettingsType.BatterySaver.fromKey(appPrefs.locationSettingsBatterySaverPerformanceType).isPremiumFeature) {
                appPrefs.locationSettingsBatterySaverPerformanceType = LocationPerformanceSettingsType.BatterySaver.default.key
            }
        } else {
            appPrefs.locationSettingsHighPerformanceDurationTypeFallback = LocationPerformanceSettingsType.HighPerformance.defaultPremium.key
        }

        if (currentPlanConfig?.hasPremiumMapStyles == false) {
            appPrefs.setAndroidMapSDKProvider(
                appPrefs.getAndroidMapSDKProviderOrDefault(
                    hasGoogleMaps = appConfigRepo.hasGoogleMaps,
                    canUseMapLibre = appConfigRepo.canUseMapLibre,
                    isPremiumPlan = false,
                    mapTilerToken = null
                )
            )
        }

        val personalSpaceOverLimit = personalSpaceRepo.personalSpaceState.value.data?.isSpaceOverLimits() == true

        val now = nowUTC()
        val personalContract = locationSharingManager.currentSharingContractForGroup(
            id = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId()
        )
        val newPersonalContract = personalContract?.asValidContract(
            newEndTimeUTCOverride = if (personalSpaceOverLimit) {
                personalContract.endTimeUTCOverride ?: now.plus(5.days)
                    .toLocalDateTime(TimeZone.UTC).toString()
            } else {
                null
            }
        )

        if (newPersonalContract != null && newPersonalContract != personalContract) {
            locationSharingManager.startSharing(newPersonalContract)
        }

        val allGroups = groupLocalDataSource.allGroupsState.value.data
            ?: emptyList()

        val allUserOwnedGroups = allGroups.filter { it.group.isCurrentUserOwner }

        val groupLimit = userRepo.userPlans.value.groupsLimit
            .takeIf { config.plans.isNotEmpty() }
            ?: Int.MAX_VALUE

        // adjust contracts
        allGroups.map {
            val isGroupOverLimit = it.isGroupOverLimits(config)
            val contract = locationSharingManager.currentSharingContractForGroup(id = it.group.id.asGroupOrSpaceId())
            val newContract = contract?.asValidContract(
                newEndTimeUTCOverride = if (isGroupOverLimit) {
                    contract.endTimeUTCOverride
                        ?: now.plus(5.days).toLocalDateTime(TimeZone.UTC).toString()
                } else {
                    null
                }
            )

            if (newContract != null && newContract != contract) {
                locationSharingManager.startSharing(newContract)
            }
        }

        val areUserOwnedGroupsOverLimits = allUserOwnedGroups.any {
            it.isGroupOverLimits(config) || allUserOwnedGroups.size > groupLimit
        }

        _hasGroupOverLimits.update {
            areUserOwnedGroupsOverLimits || personalSpaceOverLimit
        }
    }

    fun resetLimitState() {
        _hasGroupOverLimits.update { false }
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
            .maxByOrNull { it.planOrder }
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

    suspend fun checkPrivacyZoneCountLimits(groupId: GroupOrSpaceId): PremiumStatusLimitState? {
        if (groupId.isPersonalSpace) return null

        val group = groupLocalDataSource.allGroupsState.value.data
            ?.firstOrNull { it.group.id == groupId.id } ?: return null

        val config = remoteConfigRepo.getConfigOnce()
        val payload = PremiumStatusLimitPayload(
            plan = group.group.planConfig(config),
            limit = group.group.privacyZonesLimit(config),
            count = group.privacyZonesCount,
            isOwner = true,
        )

        val maxPrivacyZoneLimit = config.plans
            .maxByOrNull { it.planOrder }
            ?.privacyZonesLimit
            ?.takeIf { it >= 0 }

        if (maxPrivacyZoneLimit != null && payload.count >= maxPrivacyZoneLimit) {
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
            .maxByOrNull { it.planOrder }
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

    suspend fun checkMemberCountLimits(groupId: String): PremiumStatusLimitState? {
        val group = groupLocalDataSource.allGroupsState.value.data
            ?.firstOrNull { it.group.id == groupId } ?: return null
        val limit = group.group.membersLimit(remoteConfigRepo.getConfigOnce())
        val payload = PremiumStatusLimitPayload(
            plan = group.group.planConfig(remoteConfigRepo.getConfigOnce()),
            limit = limit,
            count = group.memberCount,
            isOwner = group.group.isCurrentUserOwner
        )

        val maxMemberLimit = remoteConfigRepo.getConfigOnce()
            .plans
            .maxByOrNull { it.planOrder }
            ?.membersLimit

        if (maxMemberLimit != null && payload.isOwner && payload.count >= maxMemberLimit) {
            return PremiumStatusLimitState.ServiceLimitReached(payload)
        } else if (payload.limit in 0..payload.count) {
            return PremiumStatusLimitState.PlanLimitReached(payload)
        } else {
            return PremiumStatusLimitState.WithinLimits(payload)
        }
    }

    fun defaultMapProvider(): MapsSDKProvider {
        return appPrefs.getAndroidMapSDKProviderOrDefault(
            hasGoogleMaps = appConfigRepo.hasGoogleMaps,
            canUseMapLibre = appConfigRepo.canUseMapLibre,
            isPremiumPlan = userRepo.userPlans.value.highestValidPlanConfig.hasPremiumMapStyles,
            mapTilerToken = appPrefs.mapTilerToken
        )
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

    data class ServiceLimitReached(override val payload: PremiumStatusLimitPayload) :
        PremiumStatusLimitState

    data class PlanLimitReached(override val payload: PremiumStatusLimitPayload) :
        PremiumStatusLimitState

    data class WithinLimits(override val payload: PremiumStatusLimitPayload) :
        PremiumStatusLimitState
}