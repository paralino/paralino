package app.paralino.android.di

import app.paralino.android.AppClass
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
        coreDependencyModules,
        mappersModule,
        databaseModules,
        daoModules,
        remoteDataSourceModule,
        viewModelModules,
        utilModule,
        locationModules,
        coreModule,
        cryptoModule,
    )

}