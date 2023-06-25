package app.paralino.core.provider

import app.paralino.core.CoreAppManager
import app.paralino.core.config
import app.paralino.core.crypto.ICryptoServiceProvider
import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.data.db.SqlDriverFactory
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.models.BackendConfig
import app.paralino.core.notifications.IGroupMemberAddedNotificationsHandler
import app.paralino.core.notifications.IPlaceNotificationsHandler
import app.paralino.core.presentation.MainViewModel
import app.paralino.core.presentation.groups.CreateGroupViewModel
import app.paralino.core.presentation.groups.GroupDetailViewModel
import app.paralino.core.presentation.groups.GroupInitViewModel
import app.paralino.core.presentation.invite.input.MemberInviteViewModel
import app.paralino.core.presentation.invite.manage.PersonalCodeViewModel
import app.paralino.core.presentation.login.LoggedOutViewModel
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
import app.paralino.core.presentation.settings.blocklist.BlocklistVideModel
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountViewModel
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.presentation.settings.emailchange.ChangeEmailViewModel
import app.paralino.core.presentation.settings.passwordchange.ChangePasswordViewModel
import app.paralino.core.presentation.share.SharingStartViewModel
import app.paralino.core.repo.api.*
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.user.IPushProviderManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.bind
import org.koin.dsl.module

object KoinIOS {

    fun createIOSKoinModules(
        deviceProvider: IDeviceApiProvider,
        connectionStatusProvider: IConnectionStatusProvider,
        locationStatusProvider: ILocationStatusProvider,
        pushSetupProvider: IPushProviderManager,
        placeNotificationHandler: IPlaceNotificationsHandler,
        groupActivityNotificationHandler: IGroupMemberAddedNotificationsHandler,
        geofenceManager: IGeofenceManager,
        backendConfig: BackendConfig,
        remoteModule: (sessionMonitor: UserSessionMonitor) -> IRemoteModule,
        documentsApi: (IRemoteModule, BackendConfig) -> IDocumentsApi,
        userApi: (IRemoteModule, BackendConfig) -> IUserApiIOS,
        functionsApi: (IRemoteModule, BackendConfig) -> IFunctionsApi,
        realtimeApi: (IRemoteModule, BackendConfig) -> IRealtimeApiIOS,
        cryptoServiceProvider: ICryptoServiceProviderIOS
    ): List<Module> {
        return listOf(
            module {
                single(createdAtStart = true) { deviceProvider }
                single { connectionStatusProvider }
                single { locationStatusProvider }
                single { pushSetupProvider }
                single { placeNotificationHandler }
                single { groupActivityNotificationHandler }
                single { geofenceManager }
                single { remoteModule(get()) }
                single { backendConfig }
                single { documentsApi(get(), get()) }
                single { UserApiWrapperIOS(userApi(get(), get())) } bind IUserApi::class
                single { functionsApi(get(), get()) }
                single { RealtimeApiWrapperIOS(realtimeApi(get(), get())) } bind IRealtimeApi::class
                single { CoroutineScopeProvider() } bind ICoroutineScopeProvider::class
                single { AppDatabaseHelper(SqlDriverFactory(), get()) }
                single { CryptoServiceWrapperIOS(cryptoServiceProvider) } bind ICryptoServiceProvider::class
            },
            createViewModelModule()
        )
    }
    
    private fun createViewModelModule(): Module {
        return module {
            factory { GroupInitViewModel(get(), get(), get(), get(), get()) }
            factory { MapHomeViewModel(get(), get(), get(), get(), get(), get(), config) }
            factory { SettingsViewModel(get(), get(), get(), get()) }
            factory { (startScreenType: LoginScreenUiType) -> LoginViewModel(get(), get(), get(), get(), startScreenType) }
            factory { (groupID: String?) -> CreateGroupViewModel(groupID, get(), get()) }
            factory { (args: String?) ->
                SharingStartViewModel(args, get(), get(), get(), get(), get(), get(), get())
            }
            factory { LoggedOutViewModel(get()) }
            factory { PersonalCodeViewModel(get(), get()) }
            factory { (groupId: String?, code: String?) ->
                MemberInviteViewModel(
                    groupId = groupId,
                    code = code,
                    codeRepo = get(),
                    groupRepo = get(),
                    groupMemberAdd = get()
                )
            }
            factory { (groupID: String) -> GroupDetailViewModel(groupID, get(), get(), get(), get(), get(), get(), get()) }
            factory { MainViewModel(get(), get(), get(), get()) }
            factory { ChangeEmailViewModel(get(), get()) }
            factory { ChangePasswordViewModel(get()) }
            factory { ConvertAccountViewModel(get()) }
            factory { EditProfileViewModel(get(), get()) }
            factory { RecoverKeysViewModel(get(), get()) }
            factory { (userId: String, secret: String) -> ResetPasswordViewModel(userId = userId, secret = secret, get()) }
            factory { (groupID: String) -> PlacesViewModel(groupID, get(), get(), get()) }
            factory { (groupID: String, placeId: String) -> PlaceDetailViewModel(groupID, placeId, get(), get()) }
            factory { (groupID: String, placeId: String?) -> CreatePlaceViewModel(groupID, placeId, get(), get()) }
            factory { (rawDataType: RawPresentationDataType) -> RawDataPresentationViewModel(get(), rawDataType) }
            factory { BlocklistVideModel(get()) }
        }
    }

    object Instances: KoinComponent {
        val coreAppManager: CoreAppManager get() = get()
    }

    object ViewModels: KoinComponent {
        val mainVM get() = get<MainViewModel>()
        fun loginVM(screenUiType: LoginScreenUiType) = get<LoginViewModel> { parametersOf(screenUiType) }
        val welcomeVM get() = get<LoggedOutViewModel>()
        val editProfileVM get() = get<EditProfileViewModel>()
        val groupInitVM get() = get<GroupInitViewModel>()
        val recoverKeysVM get() = get<RecoverKeysViewModel>()
        val resetPasswordVM get() = get<ResetPasswordViewModel>()
        val mapHomeVM get() = get<MapHomeViewModel>()
        val settingsVM get() = get<SettingsViewModel>()
        val changeEmailVM get() = get<ChangeEmailViewModel>()
        val changePasswordVM get() = get<ChangePasswordViewModel>()
        val convertAccountVM get() = get<ConvertAccountViewModel>()
        val personalCodeVM get() = get<PersonalCodeViewModel>()
        val blocklistVM get() = get<BlocklistVideModel>()
        fun rawDataVM(type: RawPresentationDataType) = get<RawDataPresentationViewModel> { parametersOf(type) }
    }

}
