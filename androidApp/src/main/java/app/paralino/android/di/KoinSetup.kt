package app.paralino.android.di

import app.paralino.android.AppClass
import app.paralino.android.BuildConfig
import app.paralino.android.app_update.AppUpdateManager
import app.paralino.android.crpyto.CryptoFrameworkLibsodium
import app.paralino.android.data.PreCheckControl
import app.paralino.android.fcm.PushProviderManager
import app.paralino.android.notification.GroupActivityNotificationPresenter
import app.paralino.android.notification.InfoMessagesNotificationPresenter
import app.paralino.android.notification.LocationNotificationHandler
import app.paralino.android.notification.PlaceNotificationPresenter
import app.paralino.android.provider.ConnectionStatusProvider
import app.paralino.android.provider.DeviceApiProvider
import app.paralino.android.provider.LocationStatusProvider
import app.paralino.android.provider.OneTimeLocationProvider
import app.paralino.android.provider.ValueFormatter
import app.paralino.android.remote.appwrite.AppWriteRemoteModule
import app.paralino.android.remote.appwrite.DocumentsApi
import app.paralino.android.remote.appwrite.FunctionsApi
import app.paralino.android.remote.appwrite.RealtimeApi
import app.paralino.android.remote.appwrite.UserApi
import app.paralino.android.tracking.LocationInitHandler
import app.paralino.android.tracking.geofence.GeofenceManager
import app.paralino.core.BuildKonfig
import app.paralino.core.appPrefs
import app.paralino.core.app_update.IAppUpdateManager
import app.paralino.core.crypto.ICryptoServiceProvider
import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.data.db.LogsDatabaseHelper
import app.paralino.core.data.db.SqlDriverFactory
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.notifications.IGroupActivityNotificationPresenter
import app.paralino.core.notifications.IInfoMessagesNotificationPresenter
import app.paralino.core.notifications.IPlaceActivityNotificationPresenter
import app.paralino.core.presentation.groups.CreateGroupViewModel
import app.paralino.core.presentation.groups.GroupDetailViewModel
import app.paralino.core.presentation.groups.GroupInitViewModel
import app.paralino.core.presentation.groups.GroupSettingsViewModel
import app.paralino.core.presentation.invite.input.MemberInviteViewModel
import app.paralino.core.presentation.invite.manage.PersonalCodeViewModel
import app.paralino.core.presentation.login.LoggedOutViewModel
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.login.LoginViewModel
import app.paralino.core.presentation.login.passwordreset.ResetPasswordViewModel
import app.paralino.core.presentation.login.recover.RecoverKeysViewModel
import app.paralino.core.presentation.main.MainViewModel
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.places.create.CreatePlaceViewModel
import app.paralino.core.presentation.places.detail.PlaceDetailViewModel
import app.paralino.core.presentation.places.list.PlacesViewModel
import app.paralino.core.presentation.rawdata.RawDataPresentationViewModel
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.presentation.settings.appstatus.AppStatusDebugViewModel
import app.paralino.core.presentation.settings.blocklist.BlocklistViewModel
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountViewModel
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.presentation.settings.emailchange.ChangeEmailViewModel
import app.paralino.core.presentation.settings.passwordchange.ChangePasswordViewModel
import app.paralino.core.presentation.share.SharingStartViewModel
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.provider.IOneTimeLocationProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.IRealtimeApi
import app.paralino.core.repo.api.IUserApi
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.user.IPushProviderManager
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

object KoinSetup {

    val modules get() = listOf(coreDependencyModule, appSpecificModule)

    private val coreDependencyModule = module {
        single { AppDatabaseHelper(SqlDriverFactory(androidContext()), get()) }
        single { LogsDatabaseHelper(SqlDriverFactory(androidContext()), get()) }

        single { CryptoFrameworkLibsodium() } bind ICryptoServiceProvider::class

        single<ICoroutineScopeProvider> { androidApplication() as AppClass }
        single<IConnectionStatusProvider>(createdAtStart = true) { ConnectionStatusProvider(androidContext(), get()) }
        single<ILocationStatusProvider>(createdAtStart = true) { LocationStatusProvider(androidContext()) }
        single<IDeviceApiProvider>(createdAtStart = true) { DeviceApiProvider(androidApplication() as AppClass, get(), get(), get()) }
        single<IPushProviderManager> { PushProviderManager(androidApplication() as AppClass) }
        single<IValueFormatter> { ValueFormatter() }
        single { OneTimeLocationProvider(get(), get(), get()) } bind IOneTimeLocationProvider::class

        single { GeofenceManager(androidApplication()) } bind IGeofenceManager::class
        factory { PlaceNotificationPresenter(androidContext()) } bind IPlaceActivityNotificationPresenter::class
        factory { GroupActivityNotificationPresenter(androidContext()) } bind IGroupActivityNotificationPresenter::class
        factory { InfoMessagesNotificationPresenter(androidContext()) } bind IInfoMessagesNotificationPresenter::class

        single<IUserApi> { UserApi(get()) }
        single<IDocumentsApi> { DocumentsApi(get()) }
        single<IFunctionsApi> { FunctionsApi(get(), get()) }
        single<IRealtimeApi> { RealtimeApi(get()) }

        single { AppUpdateManager(androidApplication(), get()) } bind IAppUpdateManager::class
    }

    private val appSpecificModule = module {
        single { PreCheckControl() }
        single { LocationInitHandler(androidContext(), get(), get()) }
        factory { LocationNotificationHandler(androidContext()) }

        single {
            AppBuildConfig(
                serverUrl = BuildKonfig.serverUrl,
                projectId = BuildKonfig.serverKey,
                publicServerUrl = BuildKonfig.publicServerUrl,
                isDevMode = BuildConfig.internalTesting,
                hasPreviewFeatures = BuildConfig.internalTesting,
                logToConsole = BuildConfig.forceLogToSystem,
                logErrorsToRemote = true
            )
        }
        single {
            AppWriteRemoteModule(
                app = androidApplication(),
                appBuildConfig = get(),
                sessionMonitor = get()
            )
        }

        viewModel { GroupInitViewModel(get(), get(), get(), get(), get()) }
        viewModel { MapHomeViewModel(get(), get(), get(), get(), get(), get(), get(), get(), appPrefs, get(), get()) }
        viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get(), get(), appPrefs) }
        viewModel { (startScreenType: LoginScreenUiType) -> LoginViewModel(get(), get(), get(), get(), startScreenType, appPrefs) }
        viewModel { (groupID: String?) -> CreateGroupViewModel(groupID, get(), get()) }
        viewModel { (args: String?) ->
            SharingStartViewModel(args, get(), get(), get(), get(), get(), get(), get(), get())
        }
        viewModel { LoggedOutViewModel(get()) }
        viewModel { PersonalCodeViewModel(get(), get(), get()) }
        viewModel { (groupId: String?, code: String?) ->
            MemberInviteViewModel(
                groupId = groupId,
                code = code,
                codeRepo = get(),
                groupRepo = get(),
                groupMemberAdd = get()
            )
        }
        viewModel { (groupID: String) -> GroupDetailViewModel(groupID, get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        viewModel { (groupID: String) -> GroupSettingsViewModel(groupID, get(), get(), get(), get(), get()) }
        viewModel { MainViewModel(get(), get(), get(), get(), get(), get()) }
        viewModel { ChangeEmailViewModel(get(), get()) }
        viewModel { ChangePasswordViewModel(get()) }
        viewModel { ConvertAccountViewModel(get()) }
        viewModel { EditProfileViewModel(get(), get()) }
        viewModel { RecoverKeysViewModel(get(), get()) }
        viewModel { (userId: String, secret: String) -> ResetPasswordViewModel(userId = userId, secret = secret, get()) }
        viewModel { (groupID: String) -> PlacesViewModel(groupID, get(), get(), get(), get()) }
        viewModel { (groupID: String, placeId: String) -> PlaceDetailViewModel(groupID, placeId, get(), get()) }
        viewModel { (groupID: String, placeId: String?, lat: Double?, lng: Double?) -> CreatePlaceViewModel(groupID, placeId, lat, lng, get()) }
        viewModel { (rawDataType: RawPresentationDataType) -> RawDataPresentationViewModel(get(), rawDataType) }
        viewModel { BlocklistViewModel(get()) }
        viewModel { AppStatusDebugViewModel(get(), get(), get(), get()) }
    }
}