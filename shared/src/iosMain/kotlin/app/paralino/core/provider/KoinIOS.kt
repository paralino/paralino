package app.paralino.core.provider

import app.paralino.core.CoreAppManager
import app.paralino.core.app_rating.IAppRatingPresenter
import app.paralino.core.app_update.IAppUpdateManager
import app.paralino.core.crypto.ICryptoServiceProvider
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.AppPrefsProvider
import app.paralino.core.db.LogsDatabaseHelper
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.SqlDriverFactory
import app.paralino.core.db.logs.AppLogsDatabase
import app.paralino.core.db.main.AppMainDatabase
import app.paralino.core.domain.crypto.group.IMemberCryptoKeyRotation
import app.paralino.core.domain.crypto.group.IMemberDeviceKeyRotation
import app.paralino.core.domain.crypto.group.MemberCryptoKeyRotation
import app.paralino.core.domain.crypto.group.MemberDeviceKeyRotation
import app.paralino.core.domain.crypto.group.NoOpMemberCryptoKeyRotation
import app.paralino.core.domain.crypto.group.NoOpMemberDeviceKeyRotation
import app.paralino.core.domain.crypto.user.IPersonalDeviceCryptoKeyRotation
import app.paralino.core.domain.crypto.user.NoOpPersonalDeviceKeyRotation
import app.paralino.core.domain.crypto.user.PersonalDeviceCryptoKeyRotation
import app.paralino.core.domain.location.BatteryAlertUploadEngine
import app.paralino.core.domain.location.IBatteryAlertUploadEngine
import app.paralino.core.domain.location.ILocationRequestAutoUpgradeUseCase
import app.paralino.core.domain.location.IOSLSEBatteryAlertUploadEngine
import app.paralino.core.domain.location.IReRegisterDeviceUseCase
import app.paralino.core.domain.location.LocationCacheRepository
import app.paralino.core.domain.location.LocationRequestAutoUpgradeUseCase
import app.paralino.core.domain.location.NoOpLocationRequestAutoUpgradeUseCase
import app.paralino.core.domain.location.NoOpReRegisterDeviceUseCase
import app.paralino.core.domain.location.ReRegisterDeviceUseCase
import app.paralino.core.domain.location.history.ILocationHistoryUploadEngine
import app.paralino.core.domain.location.history.IOSLocationHistoryUploadEngine
import app.paralino.core.domain.location.history.LocationHistoryUploadEngine
import app.paralino.core.domain.notifications.IGroupActivityNotificationPresenter
import app.paralino.core.domain.notifications.IInfoMessagesNotificationPresenter
import app.paralino.core.domain.notifications.IPlaceActivityNotificationPresenter
import app.paralino.core.domain.notifications.PushNotificationHandler
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.domain.user.UpdateUserDataUseCase
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.AppHost
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.settings.RawPresentationDataType
import app.paralino.core.notifications.IOSNSEPlaceCreatedNotificationProcessor
import app.paralino.core.notifications.IOSNSEPrivacyZoneCreatedNotificationProcessor
import app.paralino.core.notifications.IOSNSEPushNotificationHandler
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.presentation.common.NetworkQualityMonitor
import app.paralino.core.presentation.groups.add_member.MemberInviteViewModel
import app.paralino.core.presentation.groups.detail.GroupDetailViewModel
import app.paralino.core.presentation.groups.edit.CreateGroupViewModel
import app.paralino.core.presentation.groups.settings.BatteryNotificationsViewModel
import app.paralino.core.presentation.groups.settings.GroupSettingsViewModel
import app.paralino.core.presentation.history.LocationHistoryDeleteViewModel
import app.paralino.core.presentation.login.LoggedOutViewModel
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.login.LoginViewModel
import app.paralino.core.presentation.login.emailverify.VerifyEmailViewModel
import app.paralino.core.presentation.login.password_confirmation.PasswordConfirmationViewModel
import app.paralino.core.presentation.login.passwordreset.ResetPasswordViewModel
import app.paralino.core.presentation.login.recover.RecoverKeysViewModel
import app.paralino.core.presentation.main.MainViewModel
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.onboarding.AccountOnboardingViewModel
import app.paralino.core.presentation.onboarding.GroupJoinWelcomeViewModel
import app.paralino.core.presentation.onboarding.WelcomeOnboardingViewModel
import app.paralino.core.presentation.paralinoid.PersonalCodeViewModel
import app.paralino.core.presentation.personal_space.PersonalSpaceViewModel
import app.paralino.core.presentation.personal_space.settings.PersonalSpaceSettingsViewModel
import app.paralino.core.presentation.places.create.CreatePlaceScreenStep
import app.paralino.core.presentation.places.create.CreatePlaceViewModel
import app.paralino.core.presentation.places.detail.PlaceDetailViewModel
import app.paralino.core.presentation.places.detail.PlaceSensitivityViewModel
import app.paralino.core.presentation.places.list.PlacesViewModel
import app.paralino.core.presentation.privacyzones.create.CreatePrivacyZoneScreenStep
import app.paralino.core.presentation.privacyzones.create.CreatePrivacyZoneViewModel
import app.paralino.core.presentation.privacyzones.detail.PrivacyZoneDetailViewModel
import app.paralino.core.presentation.privacyzones.list.PrivacyZonesViewModel
import app.paralino.core.presentation.rawdata.RawDataPresentationViewModel
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.presentation.settings.appstatus.AppStatusDebugViewModel
import app.paralino.core.presentation.settings.billing.PaywallViewModel
import app.paralino.core.presentation.settings.billing.PlanInfoViewModel
import app.paralino.core.presentation.settings.billing.SubscriptionViewModel
import app.paralino.core.presentation.settings.billing.licence.LicenceCodeViewModel
import app.paralino.core.presentation.settings.blocklist.BlocklistViewModel
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountViewModel
import app.paralino.core.presentation.settings.editprofile.EditProfileViewModel
import app.paralino.core.presentation.settings.emailchange.ChangeEmailViewModel
import app.paralino.core.presentation.settings.general.GeneralInfoViewModel
import app.paralino.core.presentation.settings.generic.AppSettingsViewModel
import app.paralino.core.presentation.settings.generic.DefaultNavigationAppViewModel
import app.paralino.core.presentation.settings.generic.MeasurementSystemViewModel
import app.paralino.core.presentation.settings.generic.NotificationsGroupingViewModel
import app.paralino.core.presentation.settings.generic.PlaceSearchSettingsViewModel
import app.paralino.core.presentation.settings.generic.maps.MapInteractionsViewModel
import app.paralino.core.presentation.settings.location.LocationSettingsViewModel
import app.paralino.core.presentation.settings.passwordchange.ChangePasswordViewModel
import app.paralino.core.presentation.share.SharingStartPresentationType
import app.paralino.core.presentation.share.SharingStartViewModel
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.api.DocumentApiWrapper
import app.paralino.core.repo.api.FunctionsApiWrapper
import app.paralino.core.repo.api.IDocumentApiIOS
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.IFunctionsApiIOS
import app.paralino.core.repo.api.IRealtimeApi
import app.paralino.core.repo.api.IRealtimeApiIOS
import app.paralino.core.repo.api.IUserApi
import app.paralino.core.repo.api.IUserApiIOS
import app.paralino.core.repo.api.RealtimeApiWrapperIOS
import app.paralino.core.repo.api.UserApiWrapperIOS
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.location.LocationRepositoryWrapper
import app.paralino.core.repo.location.LocationSharingContractFilter
import app.paralino.core.repo.places.IOSLSEPlacesActivityProcessor
import app.paralino.core.repo.places.IPlacesActivityProcessor
import app.paralino.core.repo.places.PlaceActivityUploadEngine
import app.paralino.core.repo.places.PlacesActivityProcessor
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.IPushProviderManager
import app.paralino.core.repo.user.UserConfigRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.bind
import org.koin.dsl.module

@Suppress("unused")
object KoinIOS {

    fun createIOSKoinModules(
        deviceProvider: IDeviceApiProvider,
        crashReportingLogr: () -> ICrashReportingLogr,
        connectionStatusProvider: IConnectionStatusProvider,
        locationStatusProvider: ILocationStatusProvider,
        notificationStatusProvider: INotificationStatusProvider,
        pushSetupProvider: () -> IPushProviderManager,
        placeNotificationPresenter: (InAppLogger) -> IPlaceActivityNotificationPresenter,
        groupActivityNotificationPresenter: (InAppLogger) -> IGroupActivityNotificationPresenter,
        infoNotificationPresenter: (InAppLogger) -> IInfoMessagesNotificationPresenter,
        geofenceManager: () -> IGeofenceManager,
        appBuildConfig: AppBuildConfig,
        oneTimeLocationProvider: (
            locationStatusProvider: ILocationStatusProvider,
            locationCacheRepo: LocationCacheRepository
        ) -> IOneTimeLocationProvider,
        remoteModule: (
            sessionMonitor: UserSessionMonitor,
            networkQualityMonitor: NetworkQualityMonitor,
            appConfig: AppConfigRepository
        ) -> IRemoteModule,
        documentsApi: (IRemoteModule, AppBuildConfig) -> IDocumentApiIOS,
        userApi: (IRemoteModule, AppBuildConfig) -> IUserApiIOS,
        functionsApi: (IRemoteModule, AppBuildConfig) -> IFunctionsApiIOS,
        realtimeApi: (IRemoteModule, AppBuildConfig) -> IRealtimeApiIOS,
        cryptoServiceProvider: () -> ICryptoServiceProviderIOS,
        valueFormatter: (AppPrefs) -> IValueFormatter,
        appUpdateManager: () -> IAppUpdateManager,
        billingProvider: () -> IBillingProvider,
        appRatingPresenter: () -> IAppRatingPresenter
    ): List<Module> {
        return buildList {
            add(
                module {
                    single(createdAtStart = true) { AppPrefsProvider(appBuildConfig) }
                    single(createdAtStart = true) { deviceProvider }
                    single { connectionStatusProvider }
                    single { locationStatusProvider }
                    single { notificationStatusProvider }
                    single { pushSetupProvider() }
                    single { crashReportingLogr() }
                    factory { placeNotificationPresenter(get()) } bind IPlaceActivityNotificationPresenter::class
                    factory { groupActivityNotificationPresenter(get()) } bind IGroupActivityNotificationPresenter::class
                    factory { infoNotificationPresenter(get()) } bind IInfoMessagesNotificationPresenter::class
                    single { billingProvider() } bind IBillingProvider::class
                    single { geofenceManager() }
                    single { remoteModule(get(), get(), get()) }
                    single { appBuildConfig }
                    single { DocumentApiWrapper(documentsApi(get(), get())) } bind IDocumentsApi::class
                    single { UserApiWrapperIOS(userApi(get(), get())) } bind IUserApi::class
                    single { FunctionsApiWrapper(functionsApi(get(), get())) } bind IFunctionsApi::class
                    single { RealtimeApiWrapperIOS(realtimeApi(get(), get()), get()) } bind IRealtimeApi::class
                    single { CoroutineScopeProvider() } bind ICoroutineScopeProvider::class
                    single { MainDatabaseHelper(AppMainDatabase.create(SqlDriverFactory(get()))) }
                    single { LogsDatabaseHelper(AppLogsDatabase.create(SqlDriverFactory(get()))) }
                    single { CryptoServiceWrapperIOS(cryptoServiceProvider()) } bind ICryptoServiceProvider::class
                    single { valueFormatter(get()) } bind IValueFormatter::class
                    single { appUpdateManager() } bind IAppUpdateManager::class
                    single { appRatingPresenter() } bind IAppRatingPresenter::class
                    single { oneTimeLocationProvider(get(), get()) }
                    single { LocationRepositoryWrapper(get(), get(), get(), get()) }
                    single { IOSNSEPlaceCreatedNotificationProcessor(get(), get(), get(), get()) }
                    single { IOSNSEPrivacyZoneCreatedNotificationProcessor(get(), get(), get(), get()) }
                    single { IOSNSEPushNotificationHandler(get(), get(), get(), get()) }
                    single { PlatformUrlHandler() } bind IPlatformUrlHandler::class

                    when (appBuildConfig.appHost) {
                        AppHost.MAIN_APP -> {
                            single { MemberCryptoKeyRotation(get(), get(), get(), get(), get(), get(), get()) } bind IMemberCryptoKeyRotation::class
                            single { MemberDeviceKeyRotation(get(), get(), get(), get(), get(), get(), get(), get()) } bind IMemberDeviceKeyRotation::class
                            single { PersonalDeviceCryptoKeyRotation(get(), get(), get(), get(), get(), get(), get(), get()) } bind IPersonalDeviceCryptoKeyRotation::class
                            single { ReRegisterDeviceUseCase(get(), get()) } bind IReRegisterDeviceUseCase::class
                            single { LocationRequestAutoUpgradeUseCase(get(), get(), get(), get(), get()) } bind ILocationRequestAutoUpgradeUseCase::class
                            single { PlacesActivityProcessor(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) } bind IPlacesActivityProcessor::class
                            single { LocationHistoryUploadEngine(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) } bind ILocationHistoryUploadEngine::class
                            single { BatteryAlertUploadEngine(get(), get(), get(), get(), get()) } bind IBatteryAlertUploadEngine::class
                        }
                        else -> {
                            single { NoOpMemberCryptoKeyRotation() } bind IMemberCryptoKeyRotation::class
                            single { NoOpMemberDeviceKeyRotation() } bind IMemberDeviceKeyRotation::class
                            single { NoOpPersonalDeviceKeyRotation() } bind IPersonalDeviceCryptoKeyRotation::class
                            single { NoOpReRegisterDeviceUseCase() } bind IReRegisterDeviceUseCase::class
                            single { NoOpLocationRequestAutoUpgradeUseCase() } bind ILocationRequestAutoUpgradeUseCase::class
                            single { IOSLSEPlacesActivityProcessor() } bind IPlacesActivityProcessor::class
                            single { IOSLocationHistoryUploadEngine() } bind ILocationHistoryUploadEngine::class
                            single { IOSLSEBatteryAlertUploadEngine() } bind IBatteryAlertUploadEngine::class
                        }
                    }
                }
            )

            if (appBuildConfig.appHost == AppHost.MAIN_APP) {
                add(createViewModelModule())
            }
        }
    }

    private fun createViewModelModule(): Module {
        return module {
            factory { AccountOnboardingViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
            factory {
                MapHomeViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
            }
            factory { SettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
            factory { (startScreenType: LoginScreenUiType) ->
                LoginViewModel(get(), get(), get(), get(), startScreenType, get(), get())
            }
            factory { (groupID: String?) -> CreateGroupViewModel(groupID, get(), get(), get(), get(), get()) }
            factory { (groupID: String?, presentationType: SharingStartPresentationType) ->
                SharingStartViewModel(groupID?.asGroupOrSpaceId(), presentationType, get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
            }
            factory { LoggedOutViewModel(get(), get()) }
            factory { PersonalCodeViewModel(get(), get(), get(), get()) }
            factory { (groupId: String?, code: String?, isOnboarding: Boolean) ->
                MemberInviteViewModel(
                    groupId = groupId,
                    code = code,
                    isOnboarding = isOnboarding,
                    codeRepo = get(),
                    groupRepo = get(),
                    groupMemberAdd = get(),
                    userRepo = get(),
                    premiumStatusManager = get()
                )
            }
            factory { (groupID: String) ->
                GroupDetailViewModel(groupID, get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
            }
            factory { (groupID: String) ->
                GroupSettingsViewModel(groupID, get(), get(), get(), get(), get(), get(), get(), get())
            }
            factory { MainViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
            factory { ChangeEmailViewModel(get(), get(), get()) }
            factory { ChangePasswordViewModel(get(), get(), get(), get()) }
            factory { ConvertAccountViewModel(get(), get(), get()) }
            factory { EditProfileViewModel(get(), get()) }
            factory { RecoverKeysViewModel(get(), get(), get(), get()) }
            factory { PasswordConfirmationViewModel(get(), get(), get()) }
            factory { VerifyEmailViewModel(get(), get(), get(), get()) }
            factory { (userId: String, secret: String) ->
                ResetPasswordViewModel(
                    userId = userId,
                    secret = secret,
                    resetPassword = get()
                )
            }
            factory { (groupID: String, isOnboarding: Boolean) -> PlacesViewModel(groupID, isOnboarding, get(), get(), get(), get(), get(), get(), get(), get()) }
            factory { (groupID: String, placeId: String) ->
                PlaceDetailViewModel(
                    groupId = groupID,
                    placeId = placeId,
                    placesRepo = get(),
                    updatePlaceAlerts = get(),
                    groupRepo = get(),
                    personalSpaceRepo = get(),
                    cryptoCore = get(),
                    appPrefs = get(),
                    premiumStatusManager = get()
                )
            }
            factory { (groupID: String, isOnboarding: Boolean) ->
                PrivacyZonesViewModel(
                    groupId = groupID,
                    isOnboarding = isOnboarding,
                    remoteConfig = get(),
                    getPrivacyZonesUseCase = get(),
                    groupRepo = get(),
                    premiumStatusManager = get(),
                    valueFormatter = get()
                )
            }
            factory { (groupID: String, zoneId: String) ->
                PrivacyZoneDetailViewModel(
                    groupId = groupID,
                    zoneId = zoneId,
                    getPrivacyZonesUseCase = get(),
                    deletePrivacyZoneUseCase = get(),
                    groupRepo = get(),
                    premiumStatusManager = get(),
                    appPrefs = get(),
                    valueFormatter = get()
                )
            }
            factory { (groupID: String, placeId: String?, editStep: CreatePlaceScreenStep?, lat: Double?, lng: Double?) ->
                CreatePlaceViewModel(
                    groupId = groupID,
                    placeId = placeId,
                    editStep = editStep,
                    lat = lat,
                    lng = lng,
                    placesRepo = get(),
                    placesLocalDataSource = get(),
                    premiumStatusManager = get(),
                    refreshEverythingUseCase = get(),
                    valueFormatter = get(),
                    placeAddressSearch = get(),
                    cachedLocation = get()
                )
            }
            factory { (groupID: String, zoneId: String?, editStep: CreatePrivacyZoneScreenStep?, lat: Double?, lng: Double?) ->
                CreatePrivacyZoneViewModel(
                    groupId = groupID,
                    zoneId = zoneId,
                    editStep = editStep,
                    lat = lat,
                    lng = lng,
                    createPrivacyZoneUseCase = get(),
                    updatePrivacyZoneUseCase = get(),
                    privacyZonesLocalDataSource = get(),
                    premiumStatusManager = get(),
                    refreshEverythingUseCase = get(),
                    valueFormatter = get(),
                    placeAddressSearch = get(),
                    cachedLocation = get(),
                    getMemberWithLocations = get(),
                    userRepo = get()
                )
            }
            factory { (rawDataType: RawPresentationDataType) ->
                RawDataPresentationViewModel(
                    rawPresentationType = rawDataType,
                    rawCryptoPresentation = get(),
                    deviceApiProvider = get()
                )
            }
            factory { BlocklistViewModel(get()) }
            factory { AppStatusDebugViewModel(get(), get(), get(), get(), get()) }
            factory { SubscriptionViewModel(get(), get(), get(), get(), get(), get()) }
            factory { (type: BillingScreenSourceType) -> PaywallViewModel(type, get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
            factory { PlanInfoViewModel(get(), get()) }
            factory { GeneralInfoViewModel(get(), get()) }
            factory { LocationSettingsViewModel(get(), get(), get(), get(), get(), get(), get()) }
            factory { WelcomeOnboardingViewModel(get(), get()) }
            factory { (groupID: String) -> GroupJoinWelcomeViewModel(groupID, get(), get(), get(), get(), get(), get()) }
            factory { (groupID: String) -> BatteryNotificationsViewModel(groupID, get(), get(), get(), get(), get()) }
            factory { MeasurementSystemViewModel(get(), get()) }
            factory { NotificationsGroupingViewModel(get()) }
            factory { MapInteractionsViewModel(get()) }
            factory { DefaultNavigationAppViewModel(get()) }
            factory { PlaceSearchSettingsViewModel(get(), get()) }
            factory { (groupId: String) -> LocationHistoryDeleteViewModel(groupId = groupId.asGroupOrSpaceId(), get(), get(), get(), get(), get(), get()) }
            factory { PersonalSpaceViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
            factory { PersonalSpaceSettingsViewModel(get()) }
            factory { (groupId: String, placeId: String) -> PlaceSensitivityViewModel(groupId = groupId, placeId = placeId, get(), get(), get(), get()) }
            factory { AppSettingsViewModel(get(), get()) }
            factory { LicenceCodeViewModel(get(), get()) }
        }
    }

    object Instances : KoinComponent {
        val coreAppManager: CoreAppManager get() = get()
        val locationRepo: LocationRepositoryWrapper get() = get()
        val placesRepo: PlacesRepository get() = get()
        val placesActivityProcessor: PlacesActivityProcessor get() = get()
        val locationSharingManager: LocationSharingManager get() = get()
        val locationPerformanceManager: LocationPerformanceManager get() = get()
        val pushNotificationsHandler: PushNotificationHandler get() = get()
        val userDataUpdate: UpdateUserDataUseCase get() = get()
        val refreshEverything: RefreshEverythingUseCase get() = get()
        val pushProvider: IPushProviderManager get() = get()
        val locationStatusProvider: ILocationStatusProvider get() = get()
        val deviceApiProvider: IDeviceApiProvider get() = get()
        val inAppLogger: InAppLogger get() = get()
        val appBuildConfig: AppBuildConfig get() = get()
        val appConfig: AppConfigRepository get() = get()
        val remoteConfigRepo: RemoteConfigRepository get() = get()
        val userConfigRepo: UserConfigRepository get() = get()
        val nseNotificationHandler: IOSNSEPushNotificationHandler get() = get()
        val placeActivityUploadEngine: PlaceActivityUploadEngine get() = get()
        val sharingContractFilter: LocationSharingContractFilter get() = get()
    }

    object ViewModels : KoinComponent {
        val mainVM get() = get<MainViewModel>()
        fun loginVM(screenUiType: LoginScreenUiType) =
            get<LoginViewModel> { parametersOf(screenUiType) }

        val welcomeVM get() = get<LoggedOutViewModel>()
        val editProfileVM get() = get<EditProfileViewModel>()
        val accountOnboardingVM get() = get<AccountOnboardingViewModel>()
        val recoverKeysVM get() = get<RecoverKeysViewModel>()
        val passwordConfirmationVM get() = get<PasswordConfirmationViewModel>()
        fun resetPasswordVM(userId: String, secret: String) = get<ResetPasswordViewModel> {
            parametersOf(userId, secret)
        }
        val mapHomeVM get() = get<MapHomeViewModel>()
        val settingsVM get() = get<SettingsViewModel>()
        val changeEmailVM get() = get<ChangeEmailViewModel>()
        val changePasswordVM get() = get<ChangePasswordViewModel>()
        val convertAccountVM get() = get<ConvertAccountViewModel>()
        val personalCodeVM get() = get<PersonalCodeViewModel>()
        val blocklistVM get() = get<BlocklistViewModel>()
        fun rawDataVM(type: RawPresentationDataType) =
            get<RawDataPresentationViewModel> { parametersOf(type) }

        fun groupCreateVM(groupId: String?) = get<CreateGroupViewModel> { parametersOf(groupId) }
        fun groupDetailVM(groupId: String) = get<GroupDetailViewModel> { parametersOf(groupId) }
        fun groupSettingsVM(groupId: String) = get<GroupSettingsViewModel> { parametersOf(groupId) }
        fun groupBatterySettingsVM(groupId: String) = get<BatteryNotificationsViewModel> { parametersOf(groupId) }
        fun memberInviteVM(groupId: String?, code: String?, isOnboarding: Boolean) =
            get<MemberInviteViewModel> { parametersOf(groupId, code, isOnboarding) }

        fun sharingStartVM(
            groupId: String?,
            presentationType: SharingStartPresentationType = SharingStartPresentationType.FULL
        ) = get<SharingStartViewModel> { parametersOf(groupId, presentationType) }

        fun sharingStartVM(groupId: String?, isOnboarding: Boolean) = sharingStartVM(
            groupId = groupId,
            presentationType = if (isOnboarding) {
                SharingStartPresentationType.GROUP_CREATE_ONBOARDING
            } else {
                SharingStartPresentationType.FULL
            }
        )
        fun placesVM(groupId: String, isOnboarding: Boolean) = get<PlacesViewModel> { parametersOf(groupId, isOnboarding) }
        fun placeDetailVM(groupId: String, placeId: String) =
            get<PlaceDetailViewModel> { parametersOf(groupId, placeId) }
        fun privacyZonesVM(groupId: String, isOnboarding: Boolean) =
            get<PrivacyZonesViewModel> { parametersOf(groupId, isOnboarding) }
        fun privacyZoneDetailVM(groupId: String, zoneId: String) =
            get<PrivacyZoneDetailViewModel> { parametersOf(groupId, zoneId) }

        fun placeCreateVM(
            groupId: String,
            placeId: String?,
            editStep: CreatePlaceScreenStep?,
            lat: Double? = null,
            lng: Double? = null
        ) = get<CreatePlaceViewModel> { parametersOf(groupId, placeId, editStep, lat, lng) }

        fun privacyZoneCreateVM(
            groupId: String,
            zoneId: String?,
            editStep: CreatePrivacyZoneScreenStep?,
            lat: Double? = null,
            lng: Double? = null
        ) = get<CreatePrivacyZoneViewModel> { parametersOf(groupId, zoneId, editStep, lat, lng) }
        val appStatusDebugVM get() = get<AppStatusDebugViewModel>()
        val subscriptionVM get() = get<SubscriptionViewModel>()
        fun paywallVM(type: BillingScreenSourceType) = get<PaywallViewModel> { parametersOf(type) }
        val planInfoVM get() = get<PlanInfoViewModel>()
        val generalInfoVM get() = get<GeneralInfoViewModel>()
        val locationSettingsVM get() = get<LocationSettingsViewModel>()
        val welcomeOnboardingVM get() = get<WelcomeOnboardingViewModel>()
        fun groupJoinWelcomeVM(groupId: String) = get<GroupJoinWelcomeViewModel> { parametersOf(groupId) }
        val verifyEmailVM get() = get<VerifyEmailViewModel>()
        val measurementSystemVM get() = get<MeasurementSystemViewModel>()
        val notificationGroupingVM get() = get<NotificationsGroupingViewModel>()
        val mapInteractionsVM get() = get<MapInteractionsViewModel>()
        val navigationAppVM get() = get<DefaultNavigationAppViewModel>()
        val placeSearchSettingsVM get() = get<PlaceSearchSettingsViewModel>()
        fun locationHistorySettingsVM(groupId: String) = get<LocationHistoryDeleteViewModel>() { parametersOf(groupId) }
        val personalSpaceVM get() = get<PersonalSpaceViewModel>()
        val personalSpaceSettingsVM get() = get<PersonalSpaceSettingsViewModel>()
        fun placeSensitivityVM(groupId: String, placeId: String) = get<PlaceSensitivityViewModel>() { parametersOf(groupId, placeId) }
        val appSettingsVM get() = get<AppSettingsViewModel>()
        val licenceCodeVM get() = get<LicenceCodeViewModel>()
    }
}
