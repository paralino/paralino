package com.zbapp.locationtrack.di

import com.zbapp.location.repo.LocationSharingManager
import com.zbapp.location.repo.circle.CircleRepository
import com.zbapp.location.repo.location.LocationRepository
import com.zbapp.locationtrack.tracking.LocationInitHandler
import com.zbapp.locationtrack.tracking.LocationPermissionHandler
import com.zbapp.locationtrack.tracking.notification.LocationNotificationHandler
import com.zbapp.locationtrack.ui.dashboard.DashboardViewModel
import com.zbapp.locationtrack.ui.map.MapViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModules = module {
    viewModel { DashboardViewModel(get(), get(), get(), androidContext()) }
    viewModel { MapViewModel(get()) }
}

val locationModules = module {
    factory { LocationNotificationHandler(androidContext()) }
    single { LocationInitHandler(androidContext()) }
    single { LocationSharingManager() }
    single { CircleRepository() }
    single { LocationRepository(get()) }
}