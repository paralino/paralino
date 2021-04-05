package com.zbapp.locationtrack.di

import com.zbapp.location.repo.location.ILocationLocalDataSource
import com.zbapp.location.repo.location.ILocationRemoteDataSource
import com.zbapp.location.repo.location.ISharingContractsLocalDataSource
import com.zbapp.location.repo.user.IUserLocalDataSource
import com.zbapp.location.repo.user.IUserRemoteDataSource
import com.zbapp.locationtrack.data.database.MainAppDatabase
import com.zbapp.locationtrack.data.repos.location.LocationLocalDataSource
import com.zbapp.locationtrack.data.repos.location.SharingContractsLocalDataSourceImpl
import com.zbapp.locationtrack.data.repos.user.UserLocalDataSource
import com.zbapp.locationtrack.net.LocationRemoteDataSource
import com.zbapp.locationtrack.net.UserRemoteDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModules = module {
    single { MainAppDatabase.create(androidContext()) }
}

val daoModules = module {
    single { get<MainAppDatabase>().locationDao() }
    single { get<MainAppDatabase>().sharingContractDao() }
    single { get<MainAppDatabase>().userDao() }
}

val localDataSourceModules = module {
    single<ILocationLocalDataSource> { LocationLocalDataSource(get()) }
    single<ISharingContractsLocalDataSource> { SharingContractsLocalDataSourceImpl(get()) }
    single<IUserLocalDataSource> { UserLocalDataSource(get()) }
}

val remoteDataSourceModule = module {
    single<ILocationRemoteDataSource> { LocationRemoteDataSource() }
    single<IUserRemoteDataSource> { UserRemoteDataSource() }
}