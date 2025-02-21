package app.paralino.android.di

import app.paralino.android.AppClass
import app.paralino.android.BuildConfig
import app.paralino.android.app_rating.AppRatingPresenter
import app.paralino.android.app_update.AppUpdateManager
import app.paralino.android.crpyto.CryptoFrameworkLibsodium
import app.paralino.android.data.PreCheckControl
import app.paralino.android.fcm.PushProviderManager
import app.paralino.android.notification.GroupActivityNotificationPresenter
import app.paralino.android.notification.InfoMessagesNotificationPresenter
import app.paralino.android.notification.LocationNotificationHandler
import app.paralino.android.notification.PlaceNotificationPresenter
import app.paralino.android.provider.BillingProviderAndroid
import app.paralino.android.provider.ConnectionStatusProvider
import app.paralino.android.provider.CrashReportingLogr
import app.paralino.android.provider.DeviceApiProvider
import app.paralino.android.provider.LocationStatusProvider
import app.paralino.android.provider.NotificationStatusProvider
import app.paralino.android.provider.OneTimeLocationProvider
import app.paralino.android.provider.ValueFormatter
import app.paralino.android.remote.appwrite.AppWriteRemoteModule
import app.paralino.android.remote.appwrite.DocumentsApi
import app.paralino.android.remote.appwrite.FunctionsApi
import app.paralino.android.remote.appwrite.RealtimeApi
import app.paralino.android.remote.appwrite.UserApi
import app.paralino.android.tracking.LocationInitHandler
import app.paralino.android.tracking.LocationServiceLauncher
import app.paralino.android.tracking.geofence.GeofenceManager
import app.paralino.core.appPrefs
import app.paralino.core.app_rating.IAppRatingPresenter
import app.paralino.core.app_update.IAppUpdateManager
import app.paralino.core.crypto.ICryptoServiceProvider
import app.paralino.core.db.AppPrefsProvider
import app.paralino.core.db.LogsDatabaseHelper
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.SqlDriverFactory
import app.paralino.core.db.logs.AppLogsDatabase
import app.paralino.core.db.main.AppMainDatabase
import app.paralino.core.domain.notifications.IGroupActivityNotificationPresenter
import app.paralino.core.domain.notifications.IInfoMessagesNotificationPresenter
import app.paralino.core.domain.notifications.IPlaceActivityNotificationPresenter
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.AppHost
import app.paralino.core.presentation.groups.edit.CreateGroupViewModel
import app.paralino.core.presentation.groups.detail.GroupDetailViewModel
import app.paralino.core.presentation.groups.edit.GroupInitViewModel
import app.paralino.core.presentation.groups.settings.BatteryNotificationsViewModel
import app.paralino.core.presentation.groups.settings.GroupSettingsViewModel
import app.paralino.core.presentation.invite.input.MemberInviteViewModel
import app.paralino.core.presentation.invite.manage.PersonalCodeViewModel
import app.paralino.core.presentation.login.LoggedOutViewModel
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.login.LoginViewModel
import app.paralino.core.presentation.login.emailverify.VerifyEmailViewModel
import app.paralino.core.presentation.login.passwordreset.ResetPasswordViewModel
import app.paralino.core.presentation.login.recover.RecoverKeysViewModel
import app.paralino.core.presentation.main.MainViewModel
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.onboarding.WelcomeOnboardingViewModel
import app.paralino.core.presentation.places.create.CreatePlaceViewModel
import app.paralino.core.presentation.places.detail.PlaceDetailViewModel
import app.paralino.core.presentation.places.list.PlacesViewModel
import app.paralino.core.presentation.rawdata.RawDataPresentationViewModel
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.presentation.settings.appstatus.AppStatusDebugViewModel
import app.paralino.core.presentation.settings.billing.PaywallViewModel
import app.paralino.core.presentation.settings.billing.PlanInfoViewModel
import app.paralino.core.presentation.settings.billing.SubscriptionViewModel
import app.paralino.core.presentation.settings.blocklist.BlocklistViewModel
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountViewModel
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.presentation.settings.emailchange.ChangeEmailViewModel
import app.paralino.core.presentation.settings.general.GeneralInfoViewModel
import app.paralino.core.presentation.settings.location.LocationSettingsViewModel
import app.paralino.core.presentation.settings.passwordchange.ChangePasswordViewModel
import app.paralino.core.presentation.share.SharingStartViewModel
import app.paralino.core.provider.IBillingProvider
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.ICrashReportingLogr
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.provider.INotificationStatusProvider
import app.paralino.core.provider.IOneTimeLocationProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.IRealtimeApi
import app.paralino.core.repo.api.IUserApi
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.user.IPushProviderManager
import app.paralino.core.ui.settings.billing.BillingScreenSourceType
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

object KoinSetup {

    val modules get() = listOf(coreDependencyModule, appSpecificModule)

    private val coreDependencyModule = module {
        single { MainDatabaseHelper(AppMainDatabase.create(SqlDriverFactory(androidContext()))) }
        single { LogsDatabaseHelper(AppLogsDatabase.create(SqlDriverFactory(androidContext()))) }

        single { CryptoFrameworkLibsodium() } bind ICryptoServiceProvider::class

        single<ICoroutineScopeProvider> { androidApplication() as AppClass }

        single<AppPrefsProvider>(createdAtStart = true) { AppPrefsProvider(androidContext()) }
        single<IConnectionStatusProvider>(createdAtStart = true) { ConnectionStatusProvider(androidContext(), get()) }
        single<ILocationStatusProvider>(createdAtStart = true) { LocationStatusProvider(androidContext()) }
        single<INotificationStatusProvider>(createdAtStart = true) { NotificationStatusProvider(androidContext()) }
        single(createdAtStart = true) { DeviceApiProvider(androidApplication() as AppClass, get(), get(), get(), get()) } bind IDeviceApiProvider::class

        single<ICrashReportingLogr> { CrashReportingLogr(get(), appPrefs) }
        single<IPushProviderManager> { PushProviderManager(androidApplication() as AppClass) }
        single<IValueFormatter> { ValueFormatter() }
        single { OneTimeLocationProvider(get(), get(), get()) } bind IOneTimeLocationProvider::class

        single { GeofenceManager(androidApplication()) } bind IGeofenceManager::class
        factory { PlaceNotificationPresenter(androidContext(), get()) } bind IPlaceActivityNotificationPresenter::class
        factory { GroupActivityNotificationPresenter(androidContext(), get()) } bind IGroupActivityNotificationPresenter::class
        factory { InfoMessagesNotificationPresenter(androidContext(), get()) } bind IInfoMessagesNotificationPresenter::class

        single<IUserApi> { UserApi(get()) }
        single<IDocumentsApi> { DocumentsApi(get()) }
        single<IFunctionsApi> { FunctionsApi(get(), get()) }
        single<IRealtimeApi> { RealtimeApi(get()) }

        single<IBillingProvider> { BillingProviderAndroid(androidApplication() as AppClass, get(), get(), get()) }

        single { AppUpdateManager(androidApplication(), get()) } bind IAppUpdateManager::class
        single { AppRatingPresenter(androidApplication(), get()) } bind IAppRatingPresenter::class
    }

    private val appSpecificModule = module {
        single { PreCheckControl() }
        single { LocationServiceLauncher(androidApplication(), get(), get(), get(), get()) }
        single { LocationInitHandler(androidContext(), get(), get(), get()) }
        factory { LocationNotificationHandler(androidContext()) }

        single {
            AppBuildConfig(
                serverUrl = BuildConfig.serverUrl,
                projectId = BuildConfig.serverKey,
                publicServerUrl = BuildConfig.publicServerUrl,
                billingRCApiKey = BuildConfig.revenueCatApiKey,
                iOSTeamId = "",
                iOSAppGroup = "",
                appHost = AppHost.MAIN_APP,
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
                sessionMonitor = get(),
                networkQualityMonitor = get()
            )
        }

        viewModel { GroupInitViewModel(get(), get(), get(), get(), get()) }
        viewModel { MapHomeViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        viewModel { (startScreenType: LoginScreenUiType) -> LoginViewModel(get(), get(), get(), get(), startScreenType, get(), get()) }
        viewModel { (groupID: String?) -> CreateGroupViewModel(groupID, get(), get(), get(), get(), get()) }
        viewModel { (groupId: String?, isOnboarding: Boolean) ->
            SharingStartViewModel(initialGroupId = groupId, isOnboarding, get(), get(), get(), get(), get(), get(), get(), get(), get())
        }
        viewModel { LoggedOutViewModel(get(), get()) }
        viewModel { PersonalCodeViewModel(get(), get(), get()) }
        viewModel { (groupId: String?, code: String?) ->
            MemberInviteViewModel(
                groupId = groupId,
                code = code,
                codeRepo = get(),
                groupRepo = get(),
                groupMemberAdd = get(),
                userRepo = get()
            )
        }
        viewModel { (groupID: String) -> GroupDetailViewModel(groupID, get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        viewModel { (groupID: String) -> GroupSettingsViewModel(groupID, get(), get(), get(), get(), get()) }
        viewModel { MainViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        viewModel { VerifyEmailViewModel(get(), get()) }
        viewModel { ChangeEmailViewModel(get(), get(), get()) }
        viewModel { ChangePasswordViewModel(get(), get(), get(), get()) }
        viewModel { ConvertAccountViewModel(get(), get(), get()) }
        viewModel { EditProfileViewModel(get(), get()) }
        viewModel { RecoverKeysViewModel(get(), get(), get(), appPrefs) }
        viewModel { (userId: String, secret: String) -> ResetPasswordViewModel(userId = userId, secret = secret, get()) }
        viewModel { (groupID: String, isOnboarding: Boolean) -> PlacesViewModel(groupID, isOnboarding, get(), get(), get(), get(), get()) }
        viewModel { (groupID: String, placeId: String) -> PlaceDetailViewModel(groupID, placeId, get(), get(), get(), get(), get()) }
        viewModel { (groupID: String, placeId: String?, lat: Double?, lng: Double?) -> CreatePlaceViewModel(groupID, placeId, lat, lng, get(), get(), get(), get(), get(), get()) }
        viewModel { (rawDataType: RawPresentationDataType) -> RawDataPresentationViewModel(rawDataType, get(), get()) }
        viewModel { BlocklistViewModel(get()) }
        viewModel { AppStatusDebugViewModel(get(), get(), get(), get(), get()) }
        viewModel { SubscriptionViewModel(get(), get(), get()) }
        viewModel { (type: BillingScreenSourceType) -> PaywallViewModel(type, get(), get(), get(), get(), get(), get()) }
        viewModel { PlanInfoViewModel(get(), appPrefs) }
        viewModel { GeneralInfoViewModel(get(), get()) }
        viewModel { LocationSettingsViewModel(get(), get(), get()) }
        viewModel { WelcomeOnboardingViewModel(get(), get()) }
        viewModel { (groupID: String) -> BatteryNotificationsViewModel(groupID, get(), get(), get(), get()) }
    }
}