package com.zbapp.locationtrack.di

import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.location.LocationRepository
import com.zbapp.core.repo.user.LoginManager
import com.zbapp.core.repo.user.UserRepository
import com.zbapp.locationtrack.data.PreCheckControl
import com.zbapp.locationtrack.data.repos.LogoutControl
import com.zbapp.locationtrack.tracking.LocationInitHandler
import com.zbapp.locationtrack.tracking.notification.LocationNotificationHandler
import com.zbapp.locationtrack.ui.dashboard.DashboardViewModel
import com.zbapp.locationtrack.ui.groups.CreateGroupViewModel
import com.zbapp.locationtrack.ui.login.LoginCheckViewModel
import com.zbapp.locationtrack.ui.login.LoginViewModel
import com.zbapp.locationtrack.ui.map.MapViewModel
import com.zbapp.locationtrack.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModules = module {
    viewModel { DashboardViewModel(get(), get(), get(), get(), androidContext()) }
    viewModel { MapViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { LoginViewModel(androidContext(), get()) }
    viewModel { LoginCheckViewModel(androidContext(), get()) }
    viewModel { CreateGroupViewModel(get()) }
}

val utilModule = module {
    factory { LogoutControl(get(), get(), get()) }
    single { PreCheckControl(get()) }
}

val locationModules = module {
    factory { LocationNotificationHandler(androidContext()) }
    single { LocationInitHandler(androidContext()) }
}

val coreModule = module {
    single { LoginManager(get(), get()) }
    single { LocationSharingManager(get()) }
    single { GroupRepository(get(), get()) }
    single { UserRepository(get(), get()) }
    single { LocationRepository(get(), get(), get()) }
}