package app.paralino.core

import app.paralino.core.app_rating.AppRatingManager
import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecutionParentManager
import app.paralino.core.domain.crypto.RawCryptoPresentation
import app.paralino.core.domain.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.domain.crypto.group.CryptoKEKRefreshUseCase
import app.paralino.core.domain.crypto.group.CryptoKeyLocalDataSource
import app.paralino.core.domain.crypto.group.CryptoKeyRemoteDataSource
import app.paralino.core.domain.crypto.group.GetGroupHistoryKeyUseCase
import app.paralino.core.domain.crypto.group.GetGroupKeyUseCase
import app.paralino.core.domain.crypto.group.GetMemberPublicKeyUseCase
import app.paralino.core.domain.crypto.group.GetMemberSenderKeyUseCase
import app.paralino.core.domain.crypto.group.GetPersonalGroupKeyUseCase
import app.paralino.core.domain.crypto.group.GetPersonalHistoryKeyUseCase
import app.paralino.core.domain.crypto.group.GroupCryptoKeyRotation
import app.paralino.core.domain.crypto.group.GroupValidityVerification
import app.paralino.core.domain.crypto.user.CryptoAccountOperationV1
import app.paralino.core.domain.crypto.user.CryptoAccountOperationV2
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.domain.crypto.user.CryptoUserKeysRemoteSource
import app.paralino.core.domain.group.GroupBatterySettingsUpdateUseCase
import app.paralino.core.domain.group.GroupCreateUseCase
import app.paralino.core.domain.group.GroupMemberAddUseCase
import app.paralino.core.domain.group.GroupMemberRemoveUseCase
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.domain.group.GroupRefreshUseCase
import app.paralino.core.domain.group.GroupRemoveUseCase
import app.paralino.core.domain.group.PlaceAddressSearchUseCase
import app.paralino.core.domain.privacyzone.CreatePrivacyZoneUseCase
import app.paralino.core.domain.privacyzone.DeletePrivacyZoneUseCase
import app.paralino.core.domain.privacyzone.GetPrivacyZonesUseCase
import app.paralino.core.domain.privacyzone.UpdatePrivacyZoneUseCase
import app.paralino.core.domain.group.PlaceAlertsSettingsUpdateUseCase
import app.paralino.core.domain.location.ActiveSharingEngine
import app.paralino.core.domain.location.ClearUserLocationUseCase
import app.paralino.core.domain.location.GeofenceRegistrationMonitor
import app.paralino.core.domain.location.GetMemberWithLocationsUseCase
import app.paralino.core.domain.location.LocationCacheRepository
import app.paralino.core.domain.location.ApproximateLocationObfuscator
import app.paralino.core.domain.location.ApproximateSharedLocationLocalSource
import app.paralino.core.domain.location.PrivacyZoneObfuscator
import app.paralino.core.domain.location.PrivacyZoneSharedLocationLocalSource
import app.paralino.core.domain.notifications.processors.PrivacyZoneCreatedNotificationProcessor
import app.paralino.core.domain.notifications.processors.PrivacyZoneSyncNotificationProcessor
import app.paralino.core.domain.location.LocationPayloadProvider
import app.paralino.core.domain.location.LocationRequestUseCase
import app.paralino.core.domain.location.LocationUploadEngine
import app.paralino.core.domain.location.MemberDeviceStatusProcessor
import app.paralino.core.domain.location.MemberDeviceStatusProvider
import app.paralino.core.domain.location.RealtimeEventsMaker
import app.paralino.core.domain.location.RealtimeEventsProcessor
import app.paralino.core.domain.location.RegisterDeviceForSharingUseCase
import app.paralino.core.domain.location.RevokeDeviceActiveStatusUseCase
import app.paralino.core.domain.location.SaveLocationUseCase
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.domain.location.engine.HandleGetLocationPayloadRequest
import app.paralino.core.domain.location.engine.LocationSaveEventProcessingEngine
import app.paralino.core.domain.location.engine.LocationUploadEventProcessingEngine
import app.paralino.core.domain.location.history.DeleteLocationHistoryEntryUseCase
import app.paralino.core.domain.location.history.DeleteLocationHistoryPeriodUseCase
import app.paralino.core.domain.location.history.DeleteObsoleteLocationHistoryUseCase
import app.paralino.core.domain.location.history.DeletePendingLocationUseCase
import app.paralino.core.domain.location.history.GetLocationHistoryUseCase
import app.paralino.core.domain.location.history.RefreshLocationHistoryUseCase
import app.paralino.core.domain.location.history.SavePendingLocationUseCase
import app.paralino.core.domain.notifications.PushNotificationDeleteUseCase
import app.paralino.core.domain.notifications.PushNotificationHandler
import app.paralino.core.domain.notifications.processors.GroupMemberAddedNotificationProcessor
import app.paralino.core.domain.notifications.processors.GroupMemberDeletedNotificationProcessor
import app.paralino.core.domain.notifications.processors.LocationRequestNotificationProcessor
import app.paralino.core.domain.notifications.processors.LocationSharingStartedNotificationsProcessor
import app.paralino.core.domain.notifications.processors.LowBatteryNotificationProcessor
import app.paralino.core.domain.notifications.processors.NotificationPreCheckProcessor
import app.paralino.core.domain.notifications.processors.PlaceActivityNotificationProcessor
import app.paralino.core.domain.notifications.processors.PlaceCreatedNotificationProcessor
import app.paralino.core.domain.notifications.processors.PlaceDeletedNotificationProcessor
import app.paralino.core.domain.notifications.processors.PlaceUpdatedNotificationProcessor
import app.paralino.core.domain.user.AppAccountStateMonitor
import app.paralino.core.domain.user.BillingRefreshMonitor
import app.paralino.core.domain.user.ChangeCredentialsUseCase
import app.paralino.core.domain.user.ConvertAccountUseCase
import app.paralino.core.domain.user.CreateAccountUseCase
import app.paralino.core.domain.user.DeactivatePersonalDeviceUseCase
import app.paralino.core.domain.user.DeletePersonalDeviceUseCase
import app.paralino.core.domain.user.ExpiredSessionHandler
import app.paralino.core.domain.user.ExpiredSessionRecovery
import app.paralino.core.domain.user.GetDirectCheckoutWebPortalUseCase
import app.paralino.core.domain.user.LoginUseCase
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.domain.user.PasswordConfirmationUseCase
import app.paralino.core.domain.user.PersonalBatterySettingsUpdateUseCase
import app.paralino.core.domain.user.PersonalSpaceRefreshUseCase
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.domain.user.RecoverAccountUseCase
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.domain.user.ResetPasswordUseCase
import app.paralino.core.domain.user.SessionRenewalHandler
import app.paralino.core.domain.user.StartDirectCheckoutUseCase
import app.paralino.core.domain.user.UpdateUserDataUseCase
import app.paralino.core.domain.user.UserCryptoKeyRefreshUseCase
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.mappers.GroupKeysMapper
import app.paralino.core.models.mappers.GroupMapperCore
import app.paralino.core.models.mappers.GroupPlaceMapperLocal
import app.paralino.core.models.mappers.GroupWithPlacesMapperCore
import app.paralino.core.models.mappers.GroupsMapperLocal
import app.paralino.core.models.mappers.LocationHistoryMapper
import app.paralino.core.models.mappers.MemberMapperCore
import app.paralino.core.models.mappers.MemberWithLocationsMapperLocal
import app.paralino.core.models.mappers.PendingLocationHistoryMapper
import app.paralino.core.models.mappers.PersonalCodeMapperCore
import app.paralino.core.models.mappers.PersonalCodeMapperLocal
import app.paralino.core.models.mappers.PlaceMapperCore
import app.paralino.core.models.mappers.PlaceSettingsMapperCore
import app.paralino.core.models.mappers.PrivacyZoneMapperCore
import app.paralino.core.models.mappers.GroupPrivacyZoneMapperLocal
import app.paralino.core.models.mappers.SharingContractsMapper
import app.paralino.core.models.mappers.UserDeviceMapper
import app.paralino.core.models.mappers.UserKeyMapperCore
import app.paralino.core.models.mappers.UserMapperCore
import app.paralino.core.models.mappers.UserMapperLocal
import app.paralino.core.presentation.common.NetworkQualityMonitor
import app.paralino.core.provider.ICrashReportingLogr
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.InAppLogger
import app.paralino.core.repo.location.LocationSharingContractFilter
import app.paralino.core.provider.Logr
import app.paralino.core.provider.NotificationSocketServiceManager
import app.paralino.core.provider.RealtimeSubscriptionManager
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.location.LocationRemoteSource
import app.paralino.core.repo.location.SharingContractsLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodeLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodesRemoteSource
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.places.PlaceActivityDataSource
import app.paralino.core.repo.places.PlaceActivityUploadEngine
import app.paralino.core.repo.places.PlacesGetRemoteSource
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.repo.places.PlacesRemoteSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.privacyzones.PrivacyZonesGetRemoteSource
import app.paralino.core.repo.privacyzones.PrivacyZonesLocalDataSource
import app.paralino.core.repo.privacyzones.PrivacyZonesRemoteSource
import app.paralino.core.repo.user.PersonalDevicesLocalDataSource
import app.paralino.core.repo.user.UserConfigRepository
import app.paralino.core.repo.user.UserDevicesRemoteSource
import app.paralino.core.repo.user.UserLocalDataSource
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.repo.user.UserRemoteSource
import app.paralino.core.repo.user.UserRepository
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

object ParalinoCore {

    fun init(
        preLaunch: KoinApplication.() -> KoinApplication,
        appModules: List<Module>
    ): KoinApplication {
        val koinApp = startKoin {
            preLaunch()
            modules(
                buildList {
                    addAll(appModules)
                    add(coreModule)
                    add(dataSourceModule)
                    add(mappersModule)
                }
            )
        }

        initializeApp(koinApp)

        return koinApp
    }

    private fun initializeApp(koinApp: KoinApplication) {

        val logr = Logr

        val appPrefs = koinApp.koin.get<AppPrefs>()
        AppPrefs.instance = appPrefs

        val appBuildConfig = koinApp.koin.get<AppBuildConfig>()
        val appConfig = koinApp.koin.get<AppConfigRepository>()
        AppBuildConfig.instance = appBuildConfig

        val deviceApiProvider = koinApp.koin.get<IDeviceApiProvider>()
        val inAppLogger = koinApp.koin.get<InAppLogger>()

        val crashReportingLogger = koinApp.koin.get<ICrashReportingLogr>()
        logr.init(
            logrs = listOfNotNull(
                deviceApiProvider.createSystemLogr(),
                inAppLogger.takeIf { appConfig.isInAppDevMode },
                crashReportingLogger
            )
        )

        if (appPrefs.deviceIdSeed.isBlank()) {
            val cryptoCore = koinApp.koin.get<CryptoCore>()
            appPrefs.deviceIdSeed = cryptoCore.randomId(length = 32, alphabet = AlphabetType.EXTENDED)
        }

        if (appPrefs.errorReportingID.isBlank()) {
            val cryptoCore = koinApp.koin.get<CryptoCore>()
            appPrefs.errorReportingID = cryptoCore.randomId(length = 32, alphabet = AlphabetType.EXTENDED)
        }

        if (deviceApiProvider.deviceType == DeviceType.iOS) {
            val currentAppVersion = appPrefs.currentAppVersionNumber
            if (currentAppVersion != null && currentAppVersion < 60) {
                // invalidate location push tokens so they regenerate
                appPrefs.locationPushTokenSynced = ""
                appPrefs.locationPushTokenUnsynced = ""
            }
        }

        if (appPrefs.currentAppVersionNumber != deviceApiProvider.appVersionCode) {
            appPrefs.previousAppVersionNumber = appPrefs.currentAppVersionNumber
            appPrefs.currentAppVersionNumber = deviceApiProvider.appVersionCode
        }

        logr.i("paralino_core", "Paralino core initialized: ${deviceApiProvider.clientHeaderValue}")

        if (appBuildConfig.apiServerConfig.serverUrl.isBlank() || appBuildConfig.apiServerConfig.projectId.isBlank()) {
            logg.e("ERROR", "Server config invalid - missing endpoint or projectId")
        }
    }
}


val mappersModule = module {
    single { UserMapperLocal(get(), get()) }
    single { UserMapperCore(get(), get(), get()) }
    single { UserDeviceMapper(get(), get()) }
    single { SharingContractsMapper }
    single { GroupsMapperLocal(get()) }
    single { GroupMapperCore(get(), get(), get()) }
    single { GroupWithPlacesMapperCore(get(), get(), get()) }
    single { MemberMapperCore(get(), get(), get()) }
    single { MemberWithLocationsMapperLocal(get()) }
    single { GroupKeysMapper() }
    single { UserKeyMapperCore(get()) }
    single { PersonalCodeMapperCore(get(), get(), get()) }
    single { PersonalCodeMapperLocal(get()) }
    single { PlaceSettingsMapperCore() }
    single { PlaceMapperCore(get(), get(), get(), get()) }
    single { GroupPlaceMapperLocal(get(), get(), get()) }
    single { PrivacyZoneMapperCore(get(), get()) }
    single { GroupPrivacyZoneMapperLocal(get()) }
    single { PendingLocationHistoryMapper(get(), get()) }
    single { LocationHistoryMapper(get(), get(), get()) }
}


val dataSourceModule = module {
    single(createdAtStart = true) { AppPrefs(get()) }
    single { ApproximateSharedLocationLocalSource(get(), get(), get()) }
    single { ApproximateLocationObfuscator(get(), get(), get()) }
    single { PrivacyZoneSharedLocationLocalSource(get(), get(), get()) }
    single { PrivacyZoneObfuscator(get(), get(), get(), get()) }
    single { LocationLocalDataSource(get(), get(), get()) }
    single { SharingContractsLocalDataSource(get(), get()) }
    single { UserLocalDataSource(get(), get()) }
    single { UserPersonalSpaceLocalDataSource(get(), get(), get(), get(), get(), get(), get()) }
    single { PersonalDevicesLocalDataSource(get(), get()) }
    single { CryptoKeyLocalDataSource(get(), get(), get(), get()) }
    single { UserGroupLocalDataSource(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { PersonalCodeLocalDataSource(get(), get()) }
    single { PlacesLocalDataSource(get(), get()) }
    single { PrivacyZonesLocalDataSource(get(), get(), get()) }

    single { UserRemoteSource(get(), get(), get(), get(), get()) }
    single { UserDevicesRemoteSource(get(), get(), get(), get(), get(), get()) }
    single { LocationRemoteSource(get(), get(), get(), get(), get(), get(), get()) }
    single { GroupsRemoteSource(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { PlacesRemoteSource(get(), get(), get(), get(), get(), get(), get()) }
    single { PlacesGetRemoteSource(get(), get(), get()) }
    single { PrivacyZonesGetRemoteSource(get(), get()) }
    single { PrivacyZonesRemoteSource(get(), get(), get()) }
    single { PersonalCodesRemoteSource(get(), get(), get(), get(), get(), get()) }
    single { CryptoKeyRemoteDataSource(get(), get()) }
    single { CryptoUserKeysRemoteSource(get(), get(), get()) }
    single { PlaceActivityDataSource(get()) }

    single { AccessLogDataSource(get(), get()) }
}

val coreModule = module {
    single { CoreAppManager(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { GeofenceRegistrationMonitor(get(), get(), get(), get(), get()) }
    single { ActiveSharingEngine(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { RealtimeSubscriptionManager(get(), get()) }

    factory { LoginUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { CreateAccountUseCase(get(), get(), get(), get(), get(), get(), get()) }
    factory { ResetPasswordUseCase(get(), get()) }
    factory { ChangeCredentialsUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { RecoverAccountUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { PasswordConfirmationUseCase(get(), get(), get(), get(), get(), get(), get()) }
    factory { ConvertAccountUseCase(get(), get(), get()) }
    factory { ExpiredSessionRecovery(get(), get(), get(), get(), get(), get()) }
    factory { SessionRenewalHandler(get(), get(), get(), get()) }
    factory { LocationRequestUseCase(get(), get(), get()) }
    factory { SaveLocationUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { SavePendingLocationUseCase(get(), get(), get(), get(), get()) }
    factory { ClearUserLocationUseCase(get(), get(), get(), get(), get(), get()) }
    factory { RealtimeEventsMaker(get(), get()) }
    factory { UserMemberIdUseCase(get(), get(), get(), get()) }
    single { UpdateUserDataUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { RawCryptoPresentation(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { PlaceActivityUploadEngine(get(), get(), get(), get(), get(), get(), get()) }
    factory { GroupBatterySettingsUpdateUseCase(get(), get(), get()) }
    factory { PlaceAlertsSettingsUpdateUseCase(get(), get()) }
    factory { PersonalBatterySettingsUpdateUseCase(get(), get(), get()) }
    single { NotificationSocketServiceManager(get(), get(), get()) }

    single { ExpiredSessionHandler(get(), get(), get()) }
    single { LogoutUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { RegisterDeviceForSharingUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { RealtimeEventsProcessor(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { GroupCryptoKeyRotation(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { GroupMemberRemoveUseCase(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { GroupMemberAddUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { GroupRefreshUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { GroupPreRefreshUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { GroupRemoveUseCase(get(), get(), get(), get(), get(), get()) }
    single { GroupCreateUseCase(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { RefreshEverythingUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { GetMemberWithLocationsUseCase(get(), get(), get(), get()) }
    single { GetLocationHistoryUseCase(get(), get()) }
    single { RefreshLocationHistoryUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { DeleteLocationHistoryEntryUseCase(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { DeleteLocationHistoryPeriodUseCase(get(), get(), get(), get()) }
    single { DeleteObsoleteLocationHistoryUseCase(get(), get(), get(), get(), get()) }
    single { DeletePendingLocationUseCase(get(), get(), get()) }
    single { PersonalSpaceRefreshUseCase(get(), get(), get(), get(), get(), get()) }
    single { DeletePersonalDeviceUseCase(get(), get()) }
    single { DeactivatePersonalDeviceUseCase(get(), get(), get(), get()) }
    single { PlaceAddressSearchUseCase(get(), get()) }
    factory { CreatePrivacyZoneUseCase(get(), get()) }
    factory { UpdatePrivacyZoneUseCase(get(), get(), get()) }
    factory { DeletePrivacyZoneUseCase(get(), get(), get()) }
    factory { GetPrivacyZonesUseCase(get(), get(), get()) }

    single { PersonalCodesRepository(get(), get(), get(), get(), get(), get(), get()) }
    single { UserRepository(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { LocationUploadEngine(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { LocationSaveEventProcessingEngine(get(), get(), get(), get(), get(), get(), get()) }
    single { LocationUploadEventProcessingEngine(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { LocationSharingContractFilter() }
    single { HandleGetLocationPayloadRequest(get(), get(), get(), get()) }
    single { LocationCacheRepository() }
    single { PlacesRepository(get(), get(), get(), get(), get(), get()) }
    single { RemoteConfigRepository(get(), get(), get(), get()) }
    single { UserConfigRepository(get(), get(), get(), get()) }
    single { AppConfigRepository(get(), get()) }
    single { PremiumStatusManager(get(), get(), get(), get(), get(), get(), get()) }
    single { StartDirectCheckoutUseCase(get()) }
    single { GetDirectCheckoutWebPortalUseCase(get()) }

    single { GroupValidityVerification(get(), get(), get(), get()) }
    single { LocationSharingManager(get(), get()) }
    single { UserSessionMonitor() }
    single { BillingRefreshMonitor() }
    single { NetworkQualityMonitor() }
    single { AppAccountStateMonitor(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { MemberDeviceStatusProvider(get(), get(), get(), get()) }
    single { LocationPayloadProvider(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { MemberDeviceStatusProcessor(get(), get(), get(), get(), get(), get(), get()) }
    single { RevokeDeviceActiveStatusUseCase(get(), get(), get(), get(), get()) }
    single { UserCryptoKeyRefreshUseCase(get(), get(), get(), get()) }

    single { CryptoCore(get()) }
    single { CryptoAccountOperationV1(get()) }
    single { CryptoAccountOperationV2(get()) }
    single { GetGroupKeyUseCase(get(), get(), get(), get()) }
    single { GetPersonalGroupKeyUseCase(get(), get(), get()) }
    single { GetMemberPublicKeyUseCase(get(), get(), get(), get(), get()) }
    single { GetMemberSenderKeyUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { GetGroupHistoryKeyUseCase(get(), get(), get()) }
    single { GetPersonalHistoryKeyUseCase(get(), get()) }
    single { CryptoKEKLocalRepository(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { CryptoUserKeysLocalSource(get(), get(), get()) }
    single { CryptoKEKRefreshUseCase(get(), get(), get()) }

    factory { NotificationPreCheckProcessor(get(), get(), get()) }
    factory { PushNotificationDeleteUseCase(get(), get()) }
    single { PushNotificationHandler(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }

    factory { PlaceActivityNotificationProcessor(get(), get(), get(), get(), get(), get(), get()) }
    factory { PlaceCreatedNotificationProcessor(get(), get(), get(), get()) }
    factory { PlaceDeletedNotificationProcessor(get()) }
    factory { PlaceUpdatedNotificationProcessor(get()) }
    factory { PrivacyZoneCreatedNotificationProcessor(get(), get(), get(), get(), get()) }
    factory { PrivacyZoneSyncNotificationProcessor(get(), get(), get()) }
    factory { GroupMemberAddedNotificationProcessor(get(), get(), get(), get()) }
    factory { GroupMemberDeletedNotificationProcessor(get()) }
    factory { LocationSharingStartedNotificationsProcessor(get(), get(), get(), get(), get()) }
    factory { LocationRequestNotificationProcessor(get(), get(), get(), get(), get(), get()) }
    factory { LowBatteryNotificationProcessor(get(), get(), get(), get()) }

    single { LocationPerformanceManager(get(), get(), get()) }

    single { InAppLogger(get(), get(), get()) }
    single { LimitedExecutionParentManager() }

    single { AppRatingManager(get(), get(), get()) }
}