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
import app.paralino.android.tracking.geofence.GeofenceManager
import app.paralino.android.notification.LocationNotificationHandler
import app.paralino.android.notification.PlaceNotificationHandler
import app.paralino.android.notification.PushNotificationHandler
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
import app.paralino.android.ui.places.create.CreatePlaceViewModel
import app.paralino.android.ui.places.detail.PlaceDetailViewModel
import app.paralino.android.ui.places.list.PlacesViewModel
import app.paralino.android.ui.rawdata.RawDataPresentationViewModel
import app.paralino.android.ui.settings.SettingsViewModel
import app.paralino.android.ui.settings.convertaccount.ConvertAccountViewModel
import app.paralino.android.ui.settings.editprofile.EditProfileViewModel
import app.paralino.android.ui.settings.emailchange.ChangeEmailViewModel
import app.paralino.android.ui.settings.passwordchange.ChangePasswordViewModel
import app.paralino.android.ui.share.SharingViewModel
import app.paralino.core.domain.crypto.GroupCryptoKeyRotation
import app.paralino.core.domain.crypto.MemberCryptoKeyRotation
import app.paralino.core.domain.crypto.RawCryptoPresentation
import app.paralino.core.domain.group.*
import app.paralino.core.domain.location.LocationRefreshUseCase
import app.paralino.core.domain.location.RealtimeEventsWhileSharingUseCase
import app.paralino.core.domain.location.RegisterDeviceForSharingUseCase
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.places.PlacesRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModules = module {
    viewModel { DashboardViewModel(androidApplication(), get(), get(), get(), get(), get()) }
    viewModel { MapViewModel(androidApplication(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { (startScreenType: Any) -> LoginViewModel(androidApplication(), get(), get(), get(), startScreenType as LoginScreenUiType) }
    viewModel { (groupID: String?) -> CreateGroupViewModel(groupID, get()) }
    viewModel { (args: String?) ->
        SharingViewModel(androidApplication(), args, get(), get(), get(), get())
    }
    viewModel { (createCode: Boolean) -> PersonalCodesViewModel(androidApplication(), createCode, get()) }
    viewModel { (groupId: String?, code: String?) ->
        MemberInviteViewModel(
            androidApplication(),
            groupId = groupId,
            code = code,
            get(), get(), get()
        )
    }
    viewModel { (groupID: String) -> GroupDetailViewModel(groupID, get(), get(), get(), get(), get(), get()) }
    viewModel { MainViewModel(get(), get(), get()) }
    viewModel { ChangeEmailViewModel(get(), get()) }
    viewModel { ChangePasswordViewModel(get()) }
    viewModel { ConvertAccountViewModel(get()) }
    viewModel { EditProfileViewModel(get(), get()) }
    viewModel { RecoverKeysViewModel(get(), get()) }
    viewModel { (userId: String, secret: String) -> ResetPasswordViewModel(userId = userId, secret = secret, get()) }
    viewModel { (groupID: String) -> PlacesViewModel(groupID, get(), get(), get()) }
    viewModel { (groupID: String, placeId: String) -> PlaceDetailViewModel(groupID, placeId, get(), get()) }
    viewModel { (groupID: String, placeId: String?) -> CreatePlaceViewModel(groupID, placeId, get(), get()) }
    viewModel { (rawDataType: RawPresentationDataType) -> RawDataPresentationViewModel(get(), rawDataType) }
}

val utilModule = module {
    single { PreCheckControl(get()) }
    single { UserSessionMonitor() }
    single { AppAccountStateMonitor(get(), get(), get(), get(), get()) }
}

val locationModules = module {
    factory { LocationNotificationHandler(androidContext()) }
    factory { PlaceNotificationHandler(androidContext(), get(), get(), get()) }
    factory { PushNotificationHandler(get()) }
    single { LocationInitHandler(androidContext(), get(), get()) }
    single { LocationPerformanceManager(get(), get(), get()) }
    single<IGeofenceManager> { GeofenceManager(androidApplication()) }
}

val coreModule = module {
    factory { LoginUseCase(get(), get(), get(), get(), get()) }
    factory { CreateAccountUseCase(get(), get(), get(), get()) }
    factory { ResetPasswordUseCase(get(), get()) }
    factory { ChangePasswordUseCase(get(), get(), get()) }
    factory { LogoutUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { ChangeEmailUseCase(get(), get()) }
    factory { RecoverAccountUseCase(get(), get(), get(), get(), get()) }
    factory { ConvertAccountUseCase(get(), get()) }
    factory { LocationRefreshUseCase(get(), get(), get(), get()) }
    factory { RealtimeEventsWhileSharingUseCase(get(), get(), get(), get()) }
    factory { UserMemberIdUseCase(get(), get(), get()) }
    factory { UpdateUserDataUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { RawCryptoPresentation(get(), get(), get(), get()) }
    factory { RegisterDeviceForSharingUseCase(get(), get(), get(), get(), get(), get(), get()) }

    single { GroupCryptoKeyRotation(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { MemberCryptoKeyRotation(get(), get(), get(), get(), get(), get()) }
    single { GroupMemberRemoveUseCase(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { GroupMemberAddUseCase(get(), get(), get(), get()) }
    single { GroupRefreshUseCase(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { GroupPreRefreshUseCase(get(), get(), get(), get(), get(), get()) }
    single { GroupRemoveUseCase(get(), get(), get(), get(), get(), get()) }

    single { LocationSharingManager(get(), get()) }
    single { PersonalCodesRepository(get(), get(), get(), get()) }
    single { GroupRepository(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { UserRepository(get(), get(), get(), get(), get(), get()) }
    single { LocationRepository(get(), get(), get(), get(), get()) }
    single { PlacesRepository(get(), get(), get(), get(), get()) }
}