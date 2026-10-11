package app.paralino.core.domain.user

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.settings.MapsSDKProvider
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.user.UserRepository

class GetMapSDKProviderUseCase(
    private val appPrefs: AppPrefs,
    private val appConfigRepo: AppConfigRepository,
    private val userRepo: UserRepository
) {

    fun getProvider(
        hasPremiumMapStyles: Boolean = userRepo.userPlans.value.highestValidPlanConfig.hasPremiumMapStyles
    ): MapsSDKProvider {
        return appPrefs.getAndroidMapSDKProviderOrDefault(
            hasGoogleMaps = appConfigRepo.hasGoogleMaps,
            canUseMapLibre = appConfigRepo.canUseMapLibre,
            hasPremiumMapStyles = hasPremiumMapStyles,
            mapTilerStyleToken = appPrefs.mapTilerStylesToken ?: appPrefs.mapTilerTokenV1,
            mapTilerSatelliteToken = appPrefs.mapTilerSatelliteToken ?: appPrefs.mapTilerTokenV1
        )
    }
}
