package com.zbapp.locationtrack.di

import com.zbapp.core.domain.user.*
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.repo.location.LocationRepository
import com.zbapp.core.repo.personal_code.PersonalCodesRepository
import com.zbapp.core.repo.user.UserRepository
import com.zbapp.locationtrack.data.PreCheckControl
import com.zbapp.locationtrack.tracking.LocationInitHandler
import com.zbapp.locationtrack.tracking.notification.LocationNotificationHandler
import com.zbapp.locationtrack.ui.MainViewModel
import com.zbapp.locationtrack.ui.dashboard.DashboardViewModel
import com.zbapp.locationtrack.ui.groups.create.CreateGroupViewModel
import com.zbapp.locationtrack.ui.groups.detail.GroupDetailViewModel
import com.zbapp.locationtrack.ui.invite.input.MemberInviteViewModel
import com.zbapp.locationtrack.ui.invite.manage.PersonalCodesViewModel
import com.zbapp.locationtrack.ui.login.LoginViewModel
import com.zbapp.locationtrack.ui.login.passwordreset.ResetPasswordViewModel
import com.zbapp.locationtrack.ui.login.recover.RecoverKeysViewModel
import com.zbapp.locationtrack.ui.map.MapViewModel
import com.zbapp.locationtrack.ui.settings.SettingsViewModel
import com.zbapp.locationtrack.ui.settings.editprofile.EditProfileViewModel
import com.zbapp.locationtrack.ui.settings.emailchange.ChangeEmailViewModel
import com.zbapp.locationtrack.ui.settings.passwordchange.ChangePasswordViewModel
import com.zbapp.locationtrack.ui.share.SharingViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModules = module {
    viewModel { DashboardViewModel(androidApplication(), get(), get(), get()) }
    viewModel { MapViewModel(androidApplication(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { LoginViewModel(androidApplication(), get(), get(), get()) }
    viewModel { (groupID: String?) -> CreateGroupViewModel(groupID, get()) }
    viewModel { (args: String?) ->
        SharingViewModel(androidApplication(),
            args,
            get(),
            get(),
            get())
    }
    viewModel { PersonalCodesViewModel(androidApplication(), get()) }
    viewModel { (groupId: String?, code: String?) ->
        MemberInviteViewModel(androidApplication(),
            groupId = groupId,
            code = code,
            get(),
            get())
    }
    viewModel { (groupID: String) -> GroupDetailViewModel(groupID, get(), get(), get()) }
    viewModel { MainViewModel(get(), get()) }
    viewModel { ChangeEmailViewModel(get(), get()) }
    viewModel { ChangePasswordViewModel(get()) }
    viewModel { EditProfileViewModel(get()) }
    viewModel { RecoverKeysViewModel(get(), get()) }
    viewModel { (userId: String, secret: String) -> ResetPasswordViewModel(userId = userId, secret = secret, get()) }
}

val utilModule = module {
    single { PreCheckControl(get()) }
    single { UserSessionMonitor() }
    single { AppAccountStateMonitor(get(), get(), get(), get()) }
}

val locationModules = module {
    factory { LocationNotificationHandler(androidContext()) }
    single { LocationInitHandler(androidContext()) }
}

val coreModule = module {
    factory { LoginUseCase(get(), get(), get(), get(), get()) }
    factory { CreateAccountUseCase(get(), get(), get(), get()) }
    factory { ResetPasswordUseCase(get()) }
    factory { ChangePasswordUseCase(get(), get(), get()) }
    factory { LogoutUseCase(get(), get(), get(), get(), get(), get()) }
    factory { ChangeEmailUseCase(get(), get()) }
    factory { RecoverAccountUseCase(get(), get(), get(), get(), get()) }

    single { LocationSharingManager(get(), get()) }
    single { PersonalCodesRepository(get(), get(), get(), get()) }
    single { GroupRepository(get(), get(), get(), get(), get(), get()) }
    single { UserRepository(get(), get(), get(), get(), get(), get()) }
    single { LocationRepository(get(), get(), get(), get()) }
}