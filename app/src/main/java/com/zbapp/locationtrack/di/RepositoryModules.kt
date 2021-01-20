package com.zbapp.locationtrack.di

import com.zbapp.locationtrack.data.database.MainAppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModules = module {
    single { MainAppDatabase.create(androidContext()) }
}

val daoModules = module {

}

val localDataSourceModules = module {

}

val remoteDataSourceModule = module {

}

val repositoryModules = module {

}