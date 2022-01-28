package com.zbapp.locationtrack.di

import com.zbapp.core.ICoroutineScopeProvider
import com.zbapp.core.crypto.CryptoAuth
import com.zbapp.core.crypto.CryptoKeyRepository
import com.zbapp.core.crypto.ICryptoAuthProvider
import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.mappers.IGroupDataMapperCore
import com.zbapp.core.models.mappers.IMemberMapperCore
import com.zbapp.core.models.mappers.IUserDataMapperCore
import com.zbapp.core.models.mappers.IUserMapperCore
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
import com.zbapp.crpyto.CryptoAuthProvider
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
    single { CryptoAuthProvider() }
    single<ICryptoAuthProvider> { get<CryptoAuthProvider>() }
    single { CryptoAuth(get()) }
    single { CryptoKeyRepository(get(), get(), get(), get()) }
}

val mappersModule = module {
    single { UserDataMapper(get()) }
    single { UserMapper(get(), get()) }
    single { SharingContractsMapper }
    single { GroupsMapper(get(), get()) }
    single { MemberMapper(get(), get(), get()) }
    single { InviteCodeMapper(get()) }
    single { GroupKeysMapper() }
    single { GroupMemberKeysMapper() }
}

val remoteDataSourceModule = module {
    single { UserRemoteSource(get()) }
    single { LocationRemoteSource(get(), get()) }
    single { GroupsRemoteSource(get(), get(), get(), get(), get()) }
    single { InviteRemoteSource(get(), get(), get()) }
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

    single<IGroupDataMapperCore> { get<GroupsMapper>() }
    single<IUserMapperCore> { get<UserMapper>() }
    single<IUserDataMapperCore> { get<UserDataMapper>() }
    single<IMemberMapperCore> { get<MemberMapper>() }

    single<IUserApi> { UserApi(get()) }
    single<IDocumentsApi> { DocumentsApi(get()) }
    single<IFunctionsApi> { FunctionsApi(get()) }

    single<ILocationLocalDataSource> { LocationLocalDataSource(get(), get()) }
    single<ISharingContractsLocalDataSource> { SharingContractsLocalDataSourceImpl(get(), get()) }
    single<IUserLocalDataSource> { UserLocalDataSource(get(), get()) }
    single<IGroupLocalDataSource> {
        UserGroupLocalDataSource(get(), get(), get(), get(), get(), get(), get())
    }
    single<IInviteCodeLocalDataSource> { InviteLocalDataSource(get(), get()) }
}