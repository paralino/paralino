package app.paralino.core

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.group.CryptoKeyLocalDataSource
import app.paralino.core.crypto.group.CryptoKeyRemoteDataSource
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.crypto.user.CryptoUserKeysRemoteSource
import app.paralino.core.crypto.user.UserCryptoKeyLocalDataSource
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
import app.paralino.core.domain.location.LocationPayloadProvider
import app.paralino.core.domain.location.LocationRequestUseCase
import app.paralino.core.domain.location.MemberDeviceStatusProcessor
import app.paralino.core.domain.location.MemberDeviceStatusProvider
import app.paralino.core.domain.location.RealtimeEventsMaker
import app.paralino.core.domain.location.RealtimeEventsProcessor
import app.paralino.core.domain.location.RegisterDeviceForSharingUseCase
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.domain.user.AppAccountStateMonitor
import app.paralino.core.domain.user.ChangeEmailUseCase
import app.paralino.core.domain.user.ChangePasswordUseCase
import app.paralino.core.domain.user.ConvertAccountUseCase
import app.paralino.core.domain.user.CreateAccountUseCase
import app.paralino.core.domain.user.LoginUseCase
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.domain.user.RecoverAccountUseCase
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.domain.user.ResetPasswordUseCase
import app.paralino.core.domain.user.UpdateUserDataUseCase
import app.paralino.core.domain.user.UserSessionMonitor
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
import app.paralino.core.notifications.GroupMemberAddedNotificationsHandler
import app.paralino.core.notifications.PlaceActivityNotificationHandler
import app.paralino.core.notifications.LocationRequestNotificationHandler
import app.paralino.core.notifications.PushNotificationHandler
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.location.LocationRemoteSource
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.repo.location.SharingContractsLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodeLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodesRemoteSource
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.repo.places.PlacesRemoteSource
import app.paralino.core.repo.places.PlacesRepository
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

        val deviceProvider = koinApp.koin.get<IDeviceApiProvider>()
        deviceProvider.initialize()

        return koinApp
    }
}


val mappersModule = module {
    single { UserMapperLocal(get()) }
    single { UserMapperCore(get(), get()) }
    single { SharingContractsMapper }
    single { GroupsMapperLocal(get()) }
    single { GroupMapperCore(get(), get(), get()) }
    single { MemberMapperCore(get(), get(), get()) }
    single { MemberWithLocationsMapperLocal(get()) }
    single { GroupKeysMapper() }
    single { UserKeyMapperCore(get()) }
    single { PersonalCodeMapperCore(get(), get()) }
    single { PersonalCodeMapperLocal(get()) }
    single { PlaceSettingsMapperCore(get(), get()) }
    single { PlaceMapperCore(get(), get(), get()) }
    single { GroupPlaceMapperLocal(get(), get()) }
}


val dataSourceModule = module {
    single { CoreAppManager(get(), get(), get(), get(), get(), get(), get()) }
    single { LocationLocalDataSource(get(), get()) }
    single { SharingContractsLocalDataSource(get(), get()) }
    single { UserLocalDataSource(get(), get()) }
    single { UserCryptoKeyLocalDataSource(get(), config, get()) }
    single { CryptoKeyLocalDataSource(get(), get()) }
    single { UserGroupLocalDataSource(get(), get(), get(), get()) }
    single { PersonalCodeLocalDataSource(get(), get()) }
    single { PlacesLocalDataSource(get(), get()) }

    single { UserRemoteSource(get(), get(), get(), get(), get()) }
    single { UserDevicesRemoteSource(get(), get(), get(), get()) }
    single { LocationRemoteSource(get(), get(), get(), get(), get(), get()) }
    single { GroupsRemoteSource(get(), get(), get(), get(), get(), get()) }
    single { PlacesRemoteSource(get(), get(), get(), get(), get(), get(), get()) }
    single { PersonalCodesRemoteSource(get(), get(), get(), get(), get(), get()) }
    single { CryptoKeyRemoteDataSource(get(), get()) }
    single { CryptoUserKeysRemoteSource(get(), get(), get()) }
}

val coreModule = module {
    factory { LoginUseCase(get(), get(), get(), get(), get()) }
    factory { CreateAccountUseCase(get(), get(), get(), get()) }
    factory { ResetPasswordUseCase(get(), get()) }
    factory { ChangePasswordUseCase(get(), get(), get()) }
    factory { LogoutUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), config) }
    factory { ChangeEmailUseCase(get(), get()) }
    factory { RecoverAccountUseCase(get(), get(), get(), get(), get()) }
    factory { ConvertAccountUseCase(get(), get()) }
    factory { LocationRequestUseCase(get(), get()) }
    factory { RealtimeEventsMaker(get(), get()) }
    factory { UserMemberIdUseCase(get(), get(), get(), get()) }
    factory { UpdateUserDataUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { RawCryptoPresentation(get(), get(), get(), get()) }

    single { RegisterDeviceForSharingUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { RealtimeEventsProcessor(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { GroupCryptoKeyRotation(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { MemberCryptoKeyRotation(get(), get(), get(), get(), get(), get()) }
    single { MemberDeviceKeyRotation(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { GroupMemberRemoveUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { GroupMemberAddUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { GroupRefreshUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { GroupPreRefreshUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { GroupRemoveUseCase(get(), get(), get(), get(), get(), get()) }
    single { GroupCreateUseCase(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { RefreshEverythingUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get()) }

    single { PersonalCodesRepository(get(), get(), get()) }
    single { GroupRepository(get()) }
    single { UserRepository(get(), get(), get(), get(), get(), get(), get()) }
    single { LocationRepository(get(), get(), get(), get(), get(), get()) }
    single { PlacesRepository(get(), get(), get(), get(), get()) }
    single { AppConfigRepository(get(), get(), get(), get(), config) }

    single { GroupValidityVerification(get(), get(), get(), config) }
    single { LocationSharingManager(get(), get()) }
    single { UserSessionMonitor() }
    single { AppAccountStateMonitor(get(), get(), get(), get(), get(), get(), get()) }
    single { MemberDeviceStatusProvider(get(), get(), get(), get()) }
    single { LocationPayloadProvider(get(), get(), get(), get()) }
    single { MemberDeviceStatusProcessor(get(), get(), get(), get()) }

    single { CryptoCore(get()) }
    single { CryptoKEKRepository(get(), get(), get(), get(), get()) }
    single { CryptoUserKeyRepository(get(), get()) }

    single { PushNotificationHandler(get(), get(), get(), get(), get()) }
    factory { LocationRequestNotificationHandler(get(), get(), get(), get()) }
    factory { PlaceActivityNotificationHandler(get(), get(), get(), get()) }
    factory { GroupMemberAddedNotificationsHandler(get(), get(), get()) }
    single { LocationPerformanceManager(get(), get(), config) }
}