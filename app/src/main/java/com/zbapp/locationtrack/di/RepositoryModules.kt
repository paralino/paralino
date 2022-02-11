package com.zbapp.locationtrack.di

import com.zbapp.core.ICoroutineScopeProvider
import com.zbapp.core.crypto.*
import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.mappers.GroupDataMapperCore
import com.zbapp.core.models.mappers.IUserDataMapperCore
import com.zbapp.core.models.mappers.IUserMapperCore
import com.zbapp.core.models.mappers.MemberMapperCore
import com.zbapp.core.repo.api.IDocumentsApi
import com.zbapp.core.repo.api.IFunctionsApi
import com.zbapp.core.repo.api.IUserApi
import com.zbapp.core.repo.group.GroupsRemoteSource
import com.zbapp.core.repo.group.IGroupLocalDataSource
import com.zbapp.core.repo.invite.IInviteCodeLocalDataSource
import com.zbapp.core.repo.invite.InviteRemoteSource
import com.zbapp.core.repo.location.ILocationLocalDataSource
import com.zbapp.core.repo.location.ISharingContractsLocalDataSource
import com.zbapp.core.repo.location.LocationRemoteSource
import com.zbapp.core.repo.user.IUserLocalDataSource
import com.zbapp.core.repo.user.UserRemoteSource
import com.zbapp.crpyto.CryptoFrameworkProvider
import com.zbapp.crpyto.CryptoHashProvider
import com.zbapp.crpyto.CryptoKeyProvider
import com.zbapp.locationtrack.AppClass
import com.zbapp.locationtrack.BuildConfig
import com.zbapp.locationtrack.data.database.MainAppDatabase
import com.zbapp.locationtrack.data.mappers.*
import com.zbapp.locationtrack.data.repos.group.CryptoKeyLocalDataSource
import com.zbapp.locationtrack.data.repos.group.UserGroupLocalDataSource
import com.zbapp.locationtrack.data.repos.invite.InviteLocalDataSource
import com.zbapp.locationtrack.data.repos.location.LocationLocalDataSource
import com.zbapp.locationtrack.data.repos.location.SharingContractsLocalDataSourceImpl
import com.zbapp.locationtrack.data.repos.user.UserLocalDataSource
import com.zbapp.remote.appwrite.AppWriteRemoteModule
import com.zbapp.remote.appwrite.DocumentsApi
import com.zbapp.remote.appwrite.FunctionsApi
import com.zbapp.remote.appwrite.UserApi
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
    single { get<MainAppDatabase>().inviteDao() }
    single { get<MainAppDatabase>().groupKeysDao() }
    single { get<MainAppDatabase>().groupMemberKeysDao() }
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
}

val mappersModule = module {
    single { UserDataMapper(get()) }
    single { UserMapper(get(), get()) }
    single { SharingContractsMapper }
    single { GroupsMapperLocal(get()) }
    single { GroupDataMapperCore(get(), get()) }
    single { MemberMapperCore(get(), get()) }
    single { MemberMapperLocal(get()) }
    single { InviteCodeMapper(get()) }
    single { GroupKeysMapper() }
    single { GroupMemberKeysMapper() }
}

val remoteDataSourceModule = module {
    single { UserRemoteSource(get()) }
    single { LocationRemoteSource(get(), get()) }
    single { GroupsRemoteSource(get(), get(), get(), get(), get(), get()) }
    single { InviteRemoteSource(get(), get(), get()) }
    single { CryptoKeyRemoteDataSource(get(), get(), get(), get()) }
    single {
        BackendConfig(
            serverUrl = BuildConfig.serverUrl,
            projectId = BuildConfig.serverKey,
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

    single<IUserMapperCore> { get<UserMapper>() }
    single<IUserDataMapperCore> { get<UserDataMapper>() }

    single<IUserApi> { UserApi(get()) }
    single<IDocumentsApi> { DocumentsApi(get()) }
    single<IFunctionsApi> { FunctionsApi(get()) }

    single<ILocationLocalDataSource> { LocationLocalDataSource(get(), get(), get()) }
    single<ISharingContractsLocalDataSource> { SharingContractsLocalDataSourceImpl(get(), get()) }
    single<IUserLocalDataSource> { UserLocalDataSource(get(), get()) }
    single<ICryptoKeyLocalDataSource> { CryptoKeyLocalDataSource(get(), get(), get(), get(), get(), get()) }
    single<IGroupLocalDataSource> { UserGroupLocalDataSource(get(), get(), get()) }
    single<IInviteCodeLocalDataSource> { InviteLocalDataSource(get(), get()) }
}