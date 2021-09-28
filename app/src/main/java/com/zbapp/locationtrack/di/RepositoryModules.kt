package com.zbapp.locationtrack.di

import com.zbapp.core.models.BackendConfig
import com.zbapp.core.repo.group.GroupsRemoteSource
import com.zbapp.core.repo.group.IGroupLocalDataSource
import com.zbapp.core.repo.location.ILocationLocalDataSource
import com.zbapp.core.repo.location.ISharingContractsLocalDataSource
import com.zbapp.core.repo.location.LocationRemoteSource
import com.zbapp.core.repo.user.IUserApi
import com.zbapp.core.repo.user.IUserLocalDataSource
import com.zbapp.core.repo.user.UserRemoteSource
import com.zbapp.locationtrack.BuildConfig
import com.zbapp.locationtrack.data.database.MainAppDatabase
import com.zbapp.locationtrack.data.repos.group.UserGroupLocalDataSource
import com.zbapp.locationtrack.data.repos.location.LocationLocalDataSource
import com.zbapp.locationtrack.data.repos.location.SharingContractsLocalDataSourceImpl
import com.zbapp.locationtrack.data.repos.user.UserLocalDataSource
import com.zbapp.remote.api.UserApi
import com.zbapp.remote.appwrite.AppWriteRemoteModule
import com.zbapp.remote.user.RemoteUserCache
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
}

val localDataSourceModules = module {
    single<ILocationLocalDataSource> { LocationLocalDataSource(get()) }
    single<ISharingContractsLocalDataSource> { SharingContractsLocalDataSourceImpl(get()) }
    single<IUserLocalDataSource> { UserLocalDataSource(get()) }
    single<IGroupLocalDataSource> { UserGroupLocalDataSource(get()) }
}

val remoteDataSourceModule = module {
    single { RemoteUserCache() }
    single<IUserApi> { UserApi(get()) }
    single { UserRemoteSource(get()) }
    single { LocationRemoteSource(get()) }
    single { GroupsRemoteSource(get()) }
    single {
        AppWriteRemoteModule(
            androidApplication(),
            BackendConfig(
                serverUrl = BuildConfig.serverUrl,
                projectId = "",
                debug = BuildConfig.internalTesting
            )
        )
    }
}