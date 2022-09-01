package app.paralino.android.di

import app.paralino.core.domain.user.*
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.android.data.PreCheckControl
import app.paralino.android.tracking.LocationInitHandler
import app.paralino.android.tracking.notification.LocationNotificationHandler
import app.paralino.android.ui.MainViewModel
import app.paralino.android.ui.dashboard.DashboardViewModel
import app.paralino.android.ui.groups.create.CreateGroupViewModel
import app.paralino.android.ui.groups.detail.GroupDetailViewModel
import app.paralino.android.ui.invite.input.MemberInviteViewModel
import app.paralino.android.ui.invite.manage.PersonalCodesViewModel
import app.paralino.android.ui.login.LoginViewModel
import app.paralino.android.ui.login.passwordreset.ResetPasswordViewModel
import app.paralino.android.ui.login.recover.RecoverKeysViewModel
import app.paralino.android.ui.map.MapViewModel
import app.paralino.android.ui.settings.SettingsViewModel
import app.paralino.android.ui.settings.editprofile.EditProfileViewModel
import app.paralino.android.ui.settings.emailchange.ChangeEmailViewModel
import app.paralino.android.ui.settings.passwordchange.ChangePasswordViewModel
import app.paralino.android.ui.share.SharingViewModel
import app.paralino.core.repo.location.LocationPerformanceManager
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
    single { LocationInitHandler(androidContext(), get(), get()) }
    single { LocationPerformanceManager(get(), get(), get(), get()) }
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
    single { LocationRepository(get(), get(), get(), get(), get(), get()) }
}