package app.paralino.mobile.di

import app.paralino.core.domain.user.*
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.mobile.data.PreCheckControl
import app.paralino.mobile.tracking.LocationInitHandler
import app.paralino.mobile.tracking.notification.LocationNotificationHandler
import app.paralino.mobile.ui.MainViewModel
import app.paralino.mobile.ui.dashboard.DashboardViewModel
import app.paralino.mobile.ui.groups.create.CreateGroupViewModel
import app.paralino.mobile.ui.groups.detail.GroupDetailViewModel
import app.paralino.mobile.ui.invite.input.MemberInviteViewModel
import app.paralino.mobile.ui.invite.manage.PersonalCodesViewModel
import app.paralino.mobile.ui.login.LoginViewModel
import app.paralino.mobile.ui.login.passwordreset.ResetPasswordViewModel
import app.paralino.mobile.ui.login.recover.RecoverKeysViewModel
import app.paralino.mobile.ui.map.MapViewModel
import app.paralino.mobile.ui.settings.SettingsViewModel
import app.paralino.mobile.ui.settings.editprofile.EditProfileViewModel
import app.paralino.mobile.ui.settings.emailchange.ChangeEmailViewModel
import app.paralino.mobile.ui.settings.passwordchange.ChangePasswordViewModel
import app.paralino.mobile.ui.share.SharingViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModules = module {
    viewModel { DashboardViewModel(androidApplication(), get(), get(), get()) }
    viewModel { MapViewModel(androidApplication(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { (startScreenType: Any) -> LoginViewModel(androidApplication(), get(), get(), get(), startScreenType as LoginScreenUiType) }
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
    factory { ResetPasswordUseCase(get(), get()) }
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