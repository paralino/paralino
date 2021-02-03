package com.zbapp.locationtrack.di

import com.zbapp.locationtrack.tracking.LocationInitHandler
import com.zbapp.locationtrack.tracking.LocationPermissionHandler
import com.zbapp.locationtrack.tracking.notification.LocationNotificationHandler
import com.zbapp.locationtrack.ui.dashboard.DashboardViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModules = module {
    viewModel { DashboardViewModel(get()) }
}

val locationModules = module {
    factory { LocationNotificationHandler(androidContext()) }
    single { LocationInitHandler(androidContext()) }
}