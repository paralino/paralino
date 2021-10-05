package com.zbapp.locationtrack.di

import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.IDataMapperCore
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.raw.SimpleDocumentRaw
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.repo.group.*
import com.zbapp.core.repo.location.ILocationLocalDataSource
import com.zbapp.core.repo.location.ISharingContractsLocalDataSource
import com.zbapp.core.repo.location.LocationRemoteSource
import com.zbapp.core.repo.user.IUserApi
import com.zbapp.core.repo.user.IUserLocalDataSource
import com.zbapp.core.repo.user.UserRemoteSource
import com.zbapp.crpyto.DummyCryptoRepository
import com.zbapp.locationtrack.BuildConfig
import com.zbapp.locationtrack.data.database.MainAppDatabase
import com.zbapp.locationtrack.data.mappers.GroupsMapper
import com.zbapp.locationtrack.data.mappers.LocationMapper
import com.zbapp.locationtrack.data.mappers.SharingContractsMapper
import com.zbapp.locationtrack.data.mappers.UserMapper
import com.zbapp.locationtrack.data.repos.group.UserGroupLocalDataSource
import com.zbapp.locationtrack.data.repos.location.LocationLocalDataSource
import com.zbapp.locationtrack.data.repos.location.SharingContractsLocalDataSourceImpl
import com.zbapp.locationtrack.data.repos.user.UserLocalDataSource
import com.zbapp.remote.appwrite.AppWriteRemoteModule
import com.zbapp.remote.appwrite.DocumentsApi
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
}

val remoteDataSourceModule = module {
    single { UserRemoteSource(get()) }
    single { LocationRemoteSource(get()) }
    single { GroupsRemoteSource(get()) }
    single {
        AppWriteRemoteModule(
            androidApplication(),
            BackendConfig(
                serverUrl = BuildConfig.serverUrl,
                projectId = BuildConfig.serverKey,
                debug = BuildConfig.internalTesting
            )
        )
    }
}

val coreDependencyModules = module {
//    single<ICryptoRepository> { CryptoRepository(androidApplication()) }
    single<ICryptoRepository> { DummyCryptoRepository() }

    single<IDataMapperCore<UserGroup, SimpleDocumentRaw>> { get<GroupsMapper>() }
//    single<IDataMapperCore<UserMe, UserMeRaw>> { get<UserMapper>() }
//    single<IDataMapperCore<BaseLocationModel, SimpleDocumentRaw>> { get<LocationMapper>() }

    single<IUserApi> { UserApi(get()) }
    single<IDocumentsApi> { DocumentsApi(get()) }

    single<ILocationLocalDataSource> { LocationLocalDataSource(get(), get()) }
    single<ISharingContractsLocalDataSource> { SharingContractsLocalDataSourceImpl(get(), get()) }
    single<IUserLocalDataSource> { UserLocalDataSource(get(), get()) }
    single<IGroupLocalDataSource> { UserGroupLocalDataSource(get(), get()) }
}