package com.zbapp.locationtrack.di

import com.zbapp.core.ICoroutineScopeProvider
import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.mappers.IGroupDataMapperCore
import com.zbapp.core.models.mappers.ILocationDataMapperCore
import com.zbapp.core.models.mappers.IUserDataMapperCore
import com.zbapp.core.repo.group.*
import com.zbapp.core.repo.invite.IInviteApi
import com.zbapp.core.repo.invite.IInviteCodeLocalDataSource
import com.zbapp.core.repo.location.ILocationLocalDataSource
import com.zbapp.core.repo.location.ISharingContractsLocalDataSource
import com.zbapp.core.repo.location.LocationRemoteSource
import com.zbapp.core.repo.user.IUserApi
import com.zbapp.core.repo.user.IUserLocalDataSource
import com.zbapp.core.repo.user.UserRemoteSource
import com.zbapp.crpyto.DummyCryptoRepository
import com.zbapp.locationtrack.AppClass
import com.zbapp.locationtrack.BuildConfig
import com.zbapp.locationtrack.data.database.MainAppDatabase
import com.zbapp.locationtrack.data.mappers.*
import com.zbapp.locationtrack.data.repos.group.UserGroupLocalDataSource
import com.zbapp.locationtrack.data.repos.invite.InviteLocalDataSource
import com.zbapp.locationtrack.data.repos.location.LocationLocalDataSource
import com.zbapp.locationtrack.data.repos.location.SharingContractsLocalDataSourceImpl
import com.zbapp.locationtrack.data.repos.user.UserLocalDataSource
import com.zbapp.remote.appwrite.AppWriteRemoteModule
import com.zbapp.remote.appwrite.DocumentsApi
import com.zbapp.remote.appwrite.InviteApi
import com.zbapp.remote.appwrite.UserApi
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModules = module {
    single { MainAppDatabase.create(androidContext()) }
}

val daoModules = module {
    single { get<MainAppDatabase>().locationDao() }
    single { get<MainAppDatabase>().sharingContractDao() }
    single { get<MainAppDatabase>().userDao() }
    single { get<MainAppDatabase>().groupsDao() }
    single { get<MainAppDatabase>().inviteDao() }
}

val cryptoModule = module {
    single<IUserCrypto> { get<ICryptoRepository>() }
    single<IGroupCrypto> { get<ICryptoRepository>() }
}

val mappersModule = module {
    single { UserMapper(get()) }
    single { SharingContractsMapper }
    single { GroupsMapper(get()) }
    single { LocationMapper(get()) }
    single { InviteCodeMapper(get()) }
}

val remoteDataSourceModule = module {
    single { UserRemoteSource(get()) }
    single { LocationRemoteSource(get(), get()) }
    single { GroupsRemoteSource(get(), get()) }
    single {
        BackendConfig(
            serverUrl = BuildConfig.serverUrl,
            projectId = BuildConfig.serverKey,
            collectionGroupsID = BuildConfig.keyCollectionGroups,
            collectionLocationsID = BuildConfig.keyCollectionLocations,
            collectionInvitesID = BuildConfig.keyCollectionInvites,
            functionInviteCreateID = BuildConfig.keyFunctionInviteCreate,
            functionInviteGetCodeID = BuildConfig.keyFunctionInviteGetCode,
            functionInviteGetUrlID = BuildConfig.keyFunctionInviteGetUrl,
            functionInviteAcceptID = BuildConfig.keyFunctionInviteAcceptCode,
            debug = BuildConfig.internalTesting
        )
    }
    single {
        AppWriteRemoteModule(
            androidApplication(),
            get()
        )
    }
}

val coreDependencyModules = module {
    single<ICoroutineScopeProvider> { androidApplication() as AppClass }
//    single<ICryptoRepository> { CryptoRepository(androidApplication()) }
    single<ICryptoRepository> { DummyCryptoRepository() }

    single<IGroupDataMapperCore> { get<GroupsMapper>() }
    single<IUserDataMapperCore> { get<UserMapper>() }
    single<ILocationDataMapperCore> { get<LocationMapper>() }

    single<IInviteApi> { InviteApi(get(), get()) }
    single<IUserApi> { UserApi(get()) }
    single<IDocumentsApi> { DocumentsApi(get()) }

    single<ILocationLocalDataSource> { LocationLocalDataSource(get(), get()) }
    single<ISharingContractsLocalDataSource> { SharingContractsLocalDataSourceImpl(get(), get()) }
    single<IUserLocalDataSource> { UserLocalDataSource(get(), get()) }
    single<IGroupLocalDataSource> { UserGroupLocalDataSource(get(), get()) }
    single<IInviteCodeLocalDataSource> { InviteLocalDataSource(get(), get()) }
}