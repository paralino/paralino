package com.zbapp.locationtrack.di

import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.invite.InviteRepository
import com.zbapp.core.repo.location.LocationRepository
import com.zbapp.core.repo.user.LoginManager
import com.zbapp.core.repo.user.UserRepository
import com.zbapp.locationtrack.data.PreCheckControl
import com.zbapp.locationtrack.data.repos.LogoutControl
import com.zbapp.locationtrack.tracking.LocationInitHandler
import com.zbapp.locationtrack.tracking.notification.LocationNotificationHandler
import com.zbapp.locationtrack.ui.dashboard.DashboardViewModel
import com.zbapp.locationtrack.ui.groups.create.CreateGroupViewModel
import com.zbapp.locationtrack.ui.groups.SharingViewModel
import com.zbapp.locationtrack.ui.groups.detail.GroupDetailViewModel
import com.zbapp.locationtrack.ui.groups.detail.members.GroupMembersViewModel
import com.zbapp.locationtrack.ui.invite.AcceptInviteViewModel
import com.zbapp.locationtrack.ui.invite.AccessCodeInputFragmentArgs
import com.zbapp.locationtrack.ui.invite.ManageInviteViewModel
import com.zbapp.locationtrack.ui.login.LoginCheckViewModel
import com.zbapp.locationtrack.ui.login.LoginViewModel
import com.zbapp.locationtrack.ui.map.MapViewModel
import com.zbapp.locationtrack.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModules = module {
    viewModel { DashboardViewModel(androidApplication(), get(), get(), get(), get()) }
    viewModel { MapViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { LoginViewModel(androidApplication(), get()) }
    viewModel { LoginCheckViewModel(androidApplication(), get()) }
    viewModel { (groupID: String?) -> CreateGroupViewModel(groupID, get()) }
    viewModel { SharingViewModel(get()) }
    viewModel { ManageInviteViewModel(androidApplication(), get()) }
    viewModel { (args: AccessCodeInputFragmentArgs?) -> AcceptInviteViewModel(androidApplication(), args, get(), get()) }
    viewModel { (groupID: String) -> GroupDetailViewModel(groupID, get()) }
    viewModel { (groupID: String) -> GroupMembersViewModel(androidApplication(), groupID, get()) }
}

val utilModule = module {
    factory { LogoutControl(get(), get(), get(), get(), get()) }
    single { PreCheckControl(get()) }
}

val locationModules = module {
    factory { LocationNotificationHandler(androidContext()) }
    single { LocationInitHandler(androidContext()) }
}

val coreModule = module {
    single { LoginManager(get(), get(), get(), get()) }
    single { LocationSharingManager(get()) }
    single { InviteRepository(get(), get(), get()) }
    single { GroupRepository(get(), get(), get(), get()) }
    single { UserRepository(get(), get(), get()) }
    single { LocationRepository(get(), get(), get(), get(), get()) }
}