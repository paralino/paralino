package com.zbapp.locationtrack.di

import com.zbapp.location.repo.location.ILocationLocalDataSource
import com.zbapp.location.repo.location.ILocationRemoteDataSource
import com.zbapp.locationtrack.data.database.MainAppDatabase
import com.zbapp.locationtrack.data.repos.location.LocationLocalDataSource
import com.zbapp.locationtrack.net.LocationRemoteDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModules = module {
    single { MainAppDatabase.create(androidContext()) }
}

val daoModules = module {
    single { get<MainAppDatabase>().locationDao() }
}

val localDataSourceModules = module {
    single { LocationLocalDataSource(get()) }
    single<ILocationLocalDataSource> { get<LocationLocalDataSource>() }
}

val remoteDataSourceModule = module {
    single { LocationRemoteDataSource() }
    single<ILocationRemoteDataSource> { get<LocationRemoteDataSource>() }
}

val repositoryModules = module {

}