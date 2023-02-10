package app.paralino.android.di

import app.paralino.android.data.PreCheckControl
import app.paralino.android.notification.LocationNotificationHandler
import app.paralino.android.notification.PlaceNotificationHandler
import app.paralino.android.tracking.LocationInitHandler
import app.paralino.android.tracking.geofence.GeofenceManager
import app.paralino.core.config
import app.paralino.core.domain.crypto.GroupCryptoKeyRotation
import app.paralino.core.domain.crypto.GroupValidityVerification
import app.paralino.core.domain.crypto.MemberCryptoKeyRotation
import app.paralino.core.domain.crypto.RawCryptoPresentation
import app.paralino.core.domain.group.*
import app.paralino.core.domain.location.*
import app.paralino.core.domain.user.*
import app.paralino.core.notifications.IPlaceNotificationsHandler
import app.paralino.core.notifications.PushNotificationHandler
import app.paralino.core.presentation.MainViewModel
import app.paralino.core.presentation.dashboard.DashboardViewModel
import app.paralino.core.presentation.groups.CreateGroupViewModel
import app.paralino.core.presentation.groups.GroupDetailViewModel
import app.paralino.core.presentation.invite.input.MemberInviteViewModel
import app.paralino.core.presentation.invite.manage.PersonalCodeViewModel
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.login.LoginViewModel
import app.paralino.core.presentation.login.passwordreset.ResetPasswordViewModel
import app.paralino.core.presentation.login.recover.RecoverKeysViewModel
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.places.create.CreatePlaceViewModel
import app.paralino.core.presentation.places.detail.PlaceDetailViewModel
import app.paralino.core.presentation.places.list.PlacesViewModel
import app.paralino.core.presentation.rawdata.RawDataPresentationViewModel
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountViewModel
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.presentation.settings.emailchange.ChangeEmailViewModel
import app.paralino.core.presentation.settings.passwordchange.ChangePasswordViewModel
import app.paralino.core.presentation.share.SharingStartViewModel
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val viewModelModules = module {
    viewModel { DashboardViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { MapHomeViewModel(get(), get(), get(), get(), get(), config) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { (startScreenType: Any) -> LoginViewModel(get(), get(), get(), startScreenType as LoginScreenUiType) }
    viewModel { (groupID: String?) -> CreateGroupViewModel(groupID, get(), get()) }
    viewModel { (args: String?) ->
        SharingStartViewModel(args, get(), get(), get(), get(), get())
    }
    viewModel { (createCode: Boolean) -> PersonalCodeViewModel(get(), createCode) }
    viewModel { (groupId: String?, code: String?) ->
        MemberInviteViewModel(
            groupId = groupId,
            code = code,
            get(), get(), get()
        )
    }
    viewModel { (groupID: String) -> GroupDetailViewModel(groupID, get(), get(), get(), get(), get(), get(), get()) }
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
    single { AppAccountStateMonitor(get(), get(), get(), get(), get(), get(), get()) }
}

val locationModules = module {
    factory { LocationNotificationHandler(androidContext()) }
    factory { PlaceNotificationHandler(androidContext(), get(), get(), get()) } bind IPlaceNotificationsHandler::class
    factory { PushNotificationHandler(get(), get()) }
    single { LocationInitHandler(androidContext(), get(), get()) }
    single { LocationPerformanceManager(get(), get(), get()) }
    single<IGeofenceManager> { GeofenceManager(androidApplication()) }
}

val coreModule = module {
    factory { LoginUseCase(get(), get(), get(), get(), get()) }
    factory { CreateAccountUseCase(get(), get(), get(), get()) }
    factory { ResetPasswordUseCase(get(), get()) }
    factory { ChangePasswordUseCase(get(), get(), get()) }
    factory { LogoutUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), config) }
    factory { ChangeEmailUseCase(get(), get()) }
    factory { RecoverAccountUseCase(get(), get(), get(), get(), get()) }
    factory { ConvertAccountUseCase(get(), get()) }
    factory { LocationRefreshUseCase(get(), get(), get(), get()) }
    factory { RealtimeEventsWhileSharingUseCase(get(), get()) }
    factory { UserMemberIdUseCase(get(), get(), get(), get()) }
    factory { UpdateUserDataUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { RawCryptoPresentation(get(), get(), get(), get()) }
    factory { RegisterDeviceForSharingUseCase(get(), get(), get(), get(), get(), get(), get()) }
    factory { RealtimeEventsProcessor(get(), get(), get(), get(), get(), get()) }

    single { GroupCryptoKeyRotation(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { MemberCryptoKeyRotation(get(), get(), get(), get(), get(), get(), get()) }
    single { GroupMemberRemoveUseCase(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { GroupMemberAddUseCase(get(), get(), get(), get()) }
    single { GroupRefreshUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { GroupPreRefreshUseCase(get(), get(), get(), get(), get(), get()) }
    single { GroupRemoveUseCase(get(), get(), get(), get(), get(), get()) }
    single { GroupCreateUseCase(get(), get(), get(), get(), get(), get(), get(), get()) }

    single { GroupValidityVerification(get(), get(), get(), config) }

    single { LocationSharingManager(get(), get()) }
    single { PersonalCodesRepository(get(), get(), get()) }
    single { GroupRepository(get(), get(), get(), get(), get()) }
    single { UserRepository(get(), get(), get(), get(), get(), get()) }
    single { LocationRepository(get(), get(), get(), get(), get(), get()) }
    single { PlacesRepository(get(), get(), get(), get(), get()) }
}