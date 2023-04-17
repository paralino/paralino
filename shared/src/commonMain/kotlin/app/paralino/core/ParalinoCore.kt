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
import app.paralino.core.domain.crypto.RawCryptoPresentation
import app.paralino.core.domain.group.*
import app.paralino.core.domain.location.*
import app.paralino.core.domain.user.*
import app.paralino.core.models.mappers.*
import app.paralino.core.notifications.PushNotificationHandler
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.*
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
    factory { LogoutUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), config) }
    factory { ChangeEmailUseCase(get(), get()) }
    factory { RecoverAccountUseCase(get(), get(), get(), get(), get()) }
    factory { ConvertAccountUseCase(get(), get()) }
    factory { LocationRefreshUseCase(get(), get(), get(), get()) }
    factory { RealtimeEventsWhileSharingUseCase(get(), get()) }
    factory { UserMemberIdUseCase(get(), get(), get(), get()) }
    factory { UpdateUserDataUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { RawCryptoPresentation(get(), get(), get(), get()) }
    factory { RegisterDeviceForSharingUseCase(get(), get(), get(), get(), get(), get(), get()) }
    factory { RealtimeEventsProcessor(get(), get(), get(), get(), get(), get()) }

    single { GroupCryptoKeyRotation(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { MemberCryptoKeyRotation(get(), get(), get(), get(), get(), get(), get()) }
    single { GroupMemberRemoveUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { GroupMemberAddUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { GroupRefreshUseCase(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { GroupPreRefreshUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { GroupRemoveUseCase(get(), get(), get(), get(), get(), get()) }
    single { GroupCreateUseCase(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { RefreshEverythingUseCase(get(), get(), get(), get(), get(), get(), get(), get()) }

    single { PersonalCodesRepository(get(), get(), get()) }
    single { GroupRepository(get()) }
    single { UserRepository(get(), get(), get(), get(), get(), get()) }
    single { LocationRepository(get(), get(), get(), get(), get(), get()) }
    single { PlacesRepository(get(), get(), get(), get(), get()) }
    single { AppConfigRepository(get(), get(), get(), config) }

    single { GroupValidityVerification(get(), get(), get(), config) }
    single { LocationSharingManager(get(), get()) }
    single { UserSessionMonitor() }
    single { AppAccountStateMonitor(get(), get(), get(), get(), get(), get(), get()) }

    single { CryptoCore(get()) }
    single { CryptoKEKRepository(get(), get(), get(), get(), get()) }
    single { CryptoUserKeyRepository(get(), get(), get()) }

    factory { PushNotificationHandler(get(), get(), get(), get()) }
    single { LocationPerformanceManager(get(), get(), get()) }
}