package app.paralino.android.di

import app.paralino.android.AppClass
import app.paralino.android.crpyto.CryptoFrameworkProvider
import app.paralino.android.crpyto.CryptoHashProvider
import app.paralino.android.crpyto.CryptoKeyProvider
import app.paralino.android.data.config.Config
import app.paralino.android.data.repos.user.UserCryptoKeyLocalDataSource
import app.paralino.android.fcm.PushProviderManager
import app.paralino.android.remote.appwrite.*
import app.paralino.android.utils.ConnectionStatusProvider
import app.paralino.android.utils.DeviceApiProvider
import app.paralino.android.utils.LocationStatusProvider
import app.paralino.core.ICoroutineScopeProvider
import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.ICryptoHashProvider
import app.paralino.core.crypto.ICryptoKeyProvider
import app.paralino.core.crypto.ICryptoServiceProvider
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.group.CryptoKeyLocalDataSource
import app.paralino.core.crypto.group.CryptoKeyRemoteDataSource
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.crypto.user.ICryptoUserKeyLocalDataSource
import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.data.db.SqlDriverFactory
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.mappers.*
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.IRealtimeApi
import app.paralino.core.repo.api.IUserApi
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.*
import app.paralino.core.repo.personal_code.PersonalCodeLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodesRemoteSource
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.repo.places.PlacesRemoteSource
import app.paralino.core.repo.user.*
import app.paralino.core.utils.IConnectionStatusProvider
import app.paralino.core.utils.IDeviceApiProvider
import app.paralino.core.utils.ILocationStatusProvider
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModules = module {
    single { AppDatabaseHelper(SqlDriverFactory(androidContext()), get()) }
}

val cryptoModule = module {
    single { CryptoFrameworkProvider() }
    single { CryptoHashProvider() }
    single { CryptoKeyProvider() }
    single<ICryptoServiceProvider> { get<CryptoFrameworkProvider>() }
    single<ICryptoHashProvider> { get<CryptoHashProvider>() }
    single<ICryptoKeyProvider> { get<CryptoKeyProvider>() }
    single { CryptoCore(get(), get(), get()) }
    single { CryptoKEKRepository(get(), get(), get(), get(), get()) }
    single { CryptoUserKeyRepository(get(), get(), get()) }
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

val remoteDataSourceModule = module {
    single { UserRemoteSource(get(), get(), get(), get(), get()) }
    single { UserDevicesRemoteSource(get(), get(), get(), get()) }
    single { LocationRemoteSource(get(), get(), get(), get(), get(), get()) }
    single { GroupsRemoteSource(get(), get(), get(), get(), get(), get()) }
    single { PlacesRemoteSource(get(), get(), get(), get(), get(), get(), get()) }
    single { PersonalCodesRemoteSource(get(), get(), get(), get(), get(), get()) }
    single { CryptoKeyRemoteDataSource(get(), get()) }
    single { CryptoUserKeysRemoteSource(get(), get(), get()) }
    single {
        BackendConfig(
            serverUrl = app.paralino.android.BuildConfig.serverUrl,
            projectId = app.paralino.android.BuildConfig.serverKey,
            debug = app.paralino.android.BuildConfig.internalTesting
        )
    }
    single {
        AppWriteRemoteModule(
            androidApplication(),
            get(),
            get()
        )
    }
}

val coreDependencyModules = module {
    single<ICoroutineScopeProvider> { androidApplication() as AppClass }
    single<IConnectionStatusProvider> { ConnectionStatusProvider(androidApplication(), get()) }
    single<ILocationStatusProvider> { LocationStatusProvider(androidApplication() as AppClass) }
    single<IDeviceApiProvider> { DeviceApiProvider(androidApplication() as AppClass, config) }
    single<IPushProviderManager> { PushProviderManager(androidApplication() as AppClass) }

    single<IUserApi> { UserApi(get()) }
    single<IDocumentsApi> { DocumentsApi(get()) }
    single<IFunctionsApi> { FunctionsApi(get()) }
    single<IRealtimeApi> { RealtimeApi(get()) }

    single { LocationLocalDataSource(get(), get()) }
    single { SharingContractsLocalDataSource(get(), get()) }
    single { UserLocalDataSource(get(), get()) }
    single { CryptoKeyLocalDataSource(get(), get()) }
    single { UserGroupLocalDataSource(get(), get(), get(), get()) }
    single { PersonalCodeLocalDataSource(get(), get()) }
    single<ICryptoUserKeyLocalDataSource> { UserCryptoKeyLocalDataSource(get(), config as Config) }
    single { PlacesLocalDataSource(get(), get()) }
}