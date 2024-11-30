package app.paralino.core

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.crypto.group.CryptoKEKRefreshUseCase
import app.paralino.core.crypto.group.CryptoKeyLocalDataSource
import app.paralino.core.crypto.group.CryptoKeyRemoteDataSource
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.crypto.user.CryptoUserKeysRemoteSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecutionParentManager
import app.paralino.core.domain.crypto.GroupCryptoKeyRotation
import app.paralino.core.domain.crypto.GroupValidityVerification
import app.paralino.core.domain.crypto.MemberCryptoKeyRotation
import app.paralino.core.domain.crypto.MemberDeviceKeyRotation
import app.paralino.core.domain.crypto.RawCryptoPresentation
import app.paralino.core.domain.group.GroupCreateUseCase
import app.paralino.core.domain.group.GroupMemberAddUseCase
import app.paralino.core.domain.group.GroupMemberRemoveUseCase
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.domain.group.GroupRefreshUseCase
import app.paralino.core.domain.group.GroupRemoveUseCase
import app.paralino.core.domain.location.ActiveSharingEngine
import app.paralino.core.domain.location.ClearUserLocationUseCase
import app.paralino.core.domain.location.GeofenceRegistrationMonitor
import app.paralino.core.domain.location.LocationCacheRepository
import app.paralino.core.domain.location.LocationPayloadProvider
import app.paralino.core.domain.location.LocationRequestUseCase
import app.paralino.core.domain.location.LocationUploadEngine
import app.paralino.core.domain.location.MemberDeviceStatusProcessor
import app.paralino.core.domain.location.MemberDeviceStatusProvider
import app.paralino.core.domain.location.RealtimeEventsMaker
import app.paralino.core.domain.location.RealtimeEventsProcessor
import app.paralino.core.domain.location.RegisterDeviceForSharingUseCase
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.domain.notifications.PushNotificationDeleteUseCase
import app.paralino.core.domain.notifications.PushNotificationHandler
import app.paralino.core.domain.notifications.processors.GroupMemberAddedNotificationProcessor
import app.paralino.core.domain.notifications.processors.GroupMemberDeletedNotificationProcessor
import app.paralino.core.domain.notifications.processors.LocationRequestNotificationProcessor
import app.paralino.core.domain.notifications.processors.LocationSharingStartedNotificationsProcessor
import app.paralino.core.domain.notifications.processors.NotificationPreCheckProcessor
import app.paralino.core.domain.notifications.processors.PlaceActivityNotificationProcessor
import app.paralino.core.domain.notifications.processors.PlaceCreatedNotificationProcessor
import app.paralino.core.domain.notifications.processors.PlaceDeletedNotificationProcessor
import app.paralino.core.domain.notifications.processors.PlaceUpdatedNotificationProcessor
import app.paralino.core.domain.user.AppAccountStateMonitor
import app.paralino.core.domain.user.ChangeCredentialsUseCase
import app.paralino.core.domain.user.ConvertAccountUseCase
import app.paralino.core.domain.user.CreateAccountUseCase
import app.paralino.core.domain.user.ExpiredSessionHandler
import app.paralino.core.domain.user.ExpiredSessionRecovery
import app.paralino.core.domain.user.LoginUseCase
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.domain.user.RecoverAccountUseCase
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.domain.user.ResetPasswordUseCase
import app.paralino.core.domain.user.UpdateUserDataUseCase
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.mappers.GroupKeysMapper
import app.paralino.core.models.mappers.GroupMapperCore
import app.paralino.core.models.mappers.GroupPlaceMapperLocal
import app.paralino.core.models.mappers.GroupsMapperLocal
import app.paralino.core.models.mappers.MemberMapperCore
import app.paralino.core.models.mappers.MemberWithLocationsMapperLocal
import app.paralino.core.models.mappers.PersonalCodeMapperCore
import app.paralino.core.models.mappers.PersonalCodeMapperLocal
import app.paralino.core.models.mappers.PlaceMapperCore
import app.paralino.core.models.mappers.PlaceSettingsMapperCore
import app.paralino.core.models.mappers.SharingContractsMapper
import app.paralino.core.models.mappers.UserKeyMapperCore
import app.paralino.core.models.mappers.UserMapperCore
import app.paralino.core.models.mappers.UserMapperLocal
import app.paralino.core.provider.ICrashReportingLogr
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.InAppLogger
import app.paralino.core.provider.Logr
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.GroupRepository
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
import app.paralino.core.repo.user.UserConfigRepository
import app.paralino.core.repo.user.UserDevicesRemoteSource
import app.paralino.core.repo.user.UserLocalDataSource
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
        logg = logr

        val appPrefs = koinApp.koin.get<AppPrefs>()
        val deviceApiProvider = koinApp.koin.get<IDeviceApiProvider>()
        val appBuildConfig = koinApp.koin.get<AppBuildConfig>()
        val inAppLogger = koinApp.koin.get<InAppLogger>()

        AppBuildConfig.instance = appBuildConfig
        AppPrefs.instance = appPrefs
        connectionStatus = deviceApiProvider.connectionStatusProvider

        val crashReportingLogger = koinApp.koin.get<ICrashReportingLogr>()
        logr.init(
            logrs = listOfNotNull(
                deviceApiProvider.createSystemLogr(),
                inAppLogger.takeIf { appBuildConfig.isDevMode },
                crashReportingLogger
            )
        )

        if (appPrefs.deviceIdSeed.isBlank()) {
            val cryptoCore = koinApp.koin.get<CryptoCore>()
            appPrefs.deviceIdSeed = cryptoCore.randomId(length = 32, alphabet = AlphabetType.EXTENDED)
        }

        logr.i("paralino_core", "Paralino core initialized: ${deviceApiProvider.clientHeaderValue}")

        if (appBuildConfig.serverUrl.isBlank() || appBuildConfig.projectId.isBlank()) {
            logg.e("ERROR", "Server config invalid - missing endpoint or projectId")
        }
    }
}


val mappersModule = module {
    single { UserMapperLocal(get()) }
    single { UserMapperCore(get(), get()) }
    single { SharingContractsMapper }
    single { GroupsMapperLocal(get()) }
    single { GroupMapperCore(get(), get(), get(), get()) }
    single { MemberMapperCore(get(), get(), get()) }
    single { MemberWithLocationsMapperLocal(get()) }
    single { GroupKeysMapper() }
    single { UserKeyMapperCore(get()) }
    single { PersonalCodeMapperCore(get(), get()) }
    single { PersonalCodeMapperLocal(get()) }
    single { PlaceSettingsMapperCore() }
    single { PlaceMapperCore(get(), get(), get()) }
    single { GroupPlaceMapperLocal(get(), get()) }
}


val dataSourceModule = module {
    single(createdAtStart = true) { AppPrefs(get()) }
    single { LocationLocalDataSource(get(), get()) }
    single { SharingContractsLocalDataSource(get(), get()) }
    single { UserLocalDataSource(get(), get()) }
    single { CryptoKeyLocalDataSource(get(), get()) }
    single { UserGroupLocalDataSource(get(), get(), get(), get(), get(), get(), appPrefs) }
    single { PersonalCodeLocalDataSource(get(), get()) }
    single { PlacesLocalDataSource(get(), get()) }

    single { UserRemoteSource(get(), get(), get(), get(), get()) }
    single { UserDevicesRemoteSource(get(), get(), get(), get(), appPrefs) }
    single { LocationRemoteSource(get(), get(), get(), get(), get(), appPrefs) }
    single { GroupsRemoteSource(get(), get(), get(), get(), get(), get(), get(), appPrefs) }
    single { PlacesRemoteSource(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { PlacesGetRemoteSource(get(), get()) }
    single { PersonalCodesRemoteSource(get(), get(), get(), get(), get(), get()) }
    single { CryptoKeyRemoteDataSource(get(), get()) }
    single { CryptoUserKeysRemoteSource(get(), get(), get()) }
    single { PlaceActivityDataSource(get()) }

    single { AccessLogDataSource(get(), get()) }
}

val coreModule = module {
    single { CoreAppManager(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), appPrefs) }
    single { GeofenceRegistrationMonitor(get(), get(), get(), get(), get()) }
    single { ActiveSharingEngine(get(), get(), get(), get(), get(), get(), get(), get()) }

    factory { LoginUseCase(get(), get(), get(), get(), get(), get(), appPrefs) }
    factory { CreateAccountUseCase(get(), get(), get(), get(), get(), appPrefs) }
    factory { ResetPasswordUseCase(get(), get(), get()) }
    factory { ChangeCredentialsUseCase(get(), get(), get(), get(), appPrefs) }
    factory { RecoverAccountUseCase(get(), get(), get(), get(), get(), appPrefs) }
    factory { ConvertAccountUseCase(get(), get(), appPrefs) }
    factory { ExpiredSessionRecovery(get(), get(), get(), get(), get(), appPrefs) }
    factory { LocationRequestUseCase(get(), get()) }
    factory { ClearUserLocationUseCase(get(), get()) }
    factory { RealtimeEventsMaker(get(), get()) }
    factory { UserMemberIdUseCase(get(), get(), get(), get(), appPrefs) }
    factory { UpdateUserDataUseCase(get(), get(), get(), get(), get(), get(), get(), get(), appPrefs) }
    factory { RawCryptoPresentation(get(), get(), get(), get(), get(), appPrefs) }
    factory { PlaceActivityUploadEngine(get(), get(), get(), get()) }

    single { ExpiredSessionHandler(get(), get(), get()) }
    single { LogoutUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), appPrefs) }
    single { RegisterDeviceForSharingUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), appPrefs) }
    single { RealtimeEventsProcessor(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { GroupCryptoKeyRotation(get(), get(), get(), get(), get(), get(), get(), get(), appPrefs) }
    single { MemberCryptoKeyRotation(get(), get(), get(), get(), get(), get(), appPrefs) }
    single { MemberDeviceKeyRotation(get(), get(), get(), get(), get(), get(), appPrefs) }
    single { GroupMemberRemoveUseCase(get(), get(), get(), get(), get(), get(), get(), appPrefs) }
    single { GroupMemberAddUseCase(get(), get(), get(), get(), get(), get(), appPrefs) }
    single { GroupRefreshUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { GroupPreRefreshUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { GroupRemoveUseCase(get(), get(), get(), get(), get(), appPrefs) }
    single { GroupCreateUseCase(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { RefreshEverythingUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }

    single { PersonalCodesRepository(get(), get(), get(), get(), get(), appPrefs) }
    single { GroupRepository(get()) }
    single { UserRepository(get(), get(), get(), get(), get(), get(), get(), get(), appPrefs) }
    single { LocationUploadEngine(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { LocationCacheRepository(get()) }
    single { PlacesRepository(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), appPrefs) }
    single { RemoteConfigRepository(get(), get(), get(), get(), appPrefs) }
    single { UserConfigRepository(get(), get(), get(), get()) }

    single { GroupValidityVerification(get(), get(), get(), appPrefs) }
    single { LocationSharingManager(get(), get()) }
    single { UserSessionMonitor() }
    single { AppAccountStateMonitor(get(), get(), get(), get(), get(), get(), get(), get(), get(), appPrefs) }
    single { MemberDeviceStatusProvider(get(), get(), get(), get()) }
    single { LocationPayloadProvider(get(), get(), get(), get()) }
    single { MemberDeviceStatusProcessor(get(), get(), get(), get(), get(), appPrefs) }

    single { CryptoCore(get()) }
    single { CryptoKEKLocalRepository(get(), get(), get(), appPrefs) }
    single { CryptoUserKeysLocalSource(get(), get(), appPrefs) }
    single { CryptoKEKRefreshUseCase(get(), get(), get()) }

    factory { NotificationPreCheckProcessor(get(), get(), appPrefs) }
    factory { PushNotificationDeleteUseCase(get(), get()) }
    single { PushNotificationHandler(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), appPrefs) }

    factory { PlaceActivityNotificationProcessor(get(), get(), get(), get(), get()) }
    factory { PlaceCreatedNotificationProcessor(get(), get(), get()) }
    factory { PlaceDeletedNotificationProcessor(get()) }
    factory { PlaceUpdatedNotificationProcessor(get()) }
    factory { GroupMemberAddedNotificationProcessor(get(), get(), get(), appPrefs) }
    factory { GroupMemberDeletedNotificationProcessor(get()) }
    factory { LocationSharingStartedNotificationsProcessor(get(), get(), get(), get(), appPrefs) }
    factory { LocationRequestNotificationProcessor(get(), get(), get(), get(), get(), get()) }

    single { LocationPerformanceManager(get(), get(), appPrefs) }

    single { InAppLogger(get(), get(), get()) }
    single { LimitedExecutionParentManager() }
}