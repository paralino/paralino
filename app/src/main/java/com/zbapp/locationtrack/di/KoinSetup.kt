package com.zbapp.locationtrack.di

import com.zbapp.locationtrack.AppClass
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

object KoinSetup {

    fun initKoin(app: AppClass){
        startKoin {
            androidContext(app)
            modules(modules)
        }
    }

    private val modules = listOf(
        databaseModules,
        daoModules,
        localDataSourceModules,
        remoteDataSourceModule,
        viewModelModules,
        locationModules,
        coreModule
    )

}