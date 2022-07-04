package app.paralino.mobile.di

import app.paralino.core.ICoroutineScopeProvider
import app.paralino.core.config
import app.paralino.core.crypto.*
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.group.CryptoKeyRemoteDataSource
import app.paralino.core.crypto.group.ICryptoKeyLocalDataSource
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.crypto.user.ICryptoUserKeyLocalDataSource
import app.paralino.core.models.BackendConfig
import app.paralino.core.models.mappers.*
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.IUserApi
import app.paralino.core.repo.group.GroupsRemoteSource
import app.paralino.core.repo.group.IGroupLocalDataSource
import app.paralino.core.repo.personal_code.IPersonalCodelLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodesRemoteSource
import app.paralino.core.repo.location.ILocationLocalDataSource
import app.paralino.core.repo.location.ISharingContractsLocalDataSource
import app.paralino.core.repo.location.LocationRemoteSource
import app.paralino.core.repo.user.CryptoUserKeysRemoteSource
import app.paralino.core.repo.user.IUserLocalDataSource
import app.paralino.core.repo.user.UserRemoteSource
import app.paralino.core.utils.IConnectionStatusProvider
import app.paralino.core.utils.ILocationStatusProvider
import app.paralino.crpyto.CryptoFrameworkProvider
import app.paralino.crpyto.CryptoHashProvider
import app.paralino.crpyto.CryptoKeyProvider
import app.paralino.mobile.AppClass
import app.paralino.mobile.data.config.Config
import app.paralino.mobile.data.database.MainAppDatabase
import app.paralino.mobile.data.mappers.*
import app.paralino.mobile.data.repos.group.CryptoKeyLocalDataSource
import app.paralino.mobile.data.repos.group.UserGroupLocalDataSource
import app.paralino.mobile.data.repos.personal_code.PersonalCodeLocalDataSource
import app.paralino.mobile.data.repos.location.LocationLocalDataSource
import app.paralino.mobile.data.repos.location.SharingContractsLocalDataSourceImpl
import app.paralino.mobile.data.repos.user.UserCryptoKeyLocalDataSource
import app.paralino.mobile.data.repos.user.UserLocalDataSource
import app.paralino.mobile.utils.ConnectionStatusProvider
import app.paralino.mobile.utils.LocationStatusProvider
import app.paralino.remote.appwrite.AppWriteRemoteModule
import app.paralino.remote.appwrite.DocumentsApi
import app.paralino.remote.appwrite.FunctionsApi
import app.paralino.remote.appwrite.UserApi
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModules = module {
    single { MainAppDatabase.create(androidContext()) }
}

val daoModules = module {
    single { get<MainAppDatabase>().memberDao() }
    single { get<MainAppDatabase>().sharingContractDao() }
    single { get<MainAppDatabase>().userDao() }
    single { get<MainAppDatabase>().groupsDao() }
    single { get<MainAppDatabase>().personalCodeDao() }
    single { get<MainAppDatabase>().groupKeysDao() }
    single { get<MainAppDatabase>().userKeysDao() }
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
    single { MemberMapperCore(get(), get()) }
    single { MemberMapperLocal(get()) }
    single { GroupKeysMapper() }
    single { MemberKeysMapper() }
    single { UserKeyMapperLocal(get()) }
    single { UserKeyMapperCore(get()) }
    single { PersonalCodeMapperCore(get(), get()) }
    single { PersonalCodeMapperLocal(get()) }
}

val remoteDataSourceModule = module {
    single { UserRemoteSource(get(), get(), get(), get()) }
    single { LocationRemoteSource(get(), get(), get(), get(), get()) }
    single { GroupsRemoteSource(get(), get(), get(), get(), get(), get()) }
    single { PersonalCodesRemoteSource(get(), get(), get(), get()) }
    single { CryptoKeyRemoteDataSource(get(), get(), get(), get()) }
    single { CryptoUserKeysRemoteSource(get(), get()) }
    single {
        BackendConfig(
            serverUrl = app.paralino.mobile.BuildConfig.serverUrl,
            projectId = app.paralino.mobile.BuildConfig.serverKey,
            debug = app.paralino.mobile.BuildConfig.internalTesting
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

    single<IUserApi> { UserApi(get()) }
    single<IDocumentsApi> { DocumentsApi(get()) }
    single<IFunctionsApi> { FunctionsApi(get()) }

    single<ILocationLocalDataSource> { LocationLocalDataSource(get(), get(), get()) }
    single<ISharingContractsLocalDataSource> { SharingContractsLocalDataSourceImpl(get(), get()) }
    single<IUserLocalDataSource> { UserLocalDataSource(get(), get()) }
    single<ICryptoKeyLocalDataSource> { CryptoKeyLocalDataSource(get(), get(), get(), get(), get()) }
    single<IGroupLocalDataSource> { UserGroupLocalDataSource(get(), get(), get()) }
    single<IPersonalCodelLocalDataSource> { PersonalCodeLocalDataSource(get(), get()) }
    single<ICryptoUserKeyLocalDataSource> { UserCryptoKeyLocalDataSource(get(), get(), config as Config) }
}