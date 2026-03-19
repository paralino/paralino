//
//  KoinSetup.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-08.
//

import Foundation
import shared

public class CoreKoinSetup {
    
    private static let initializationQueue = DispatchQueue(label: "app.paralino.ios.initialization")
    
    public static var koinApp: Koin_coreKoin? {
        _koin
    }
    
    public static func initKoin(
        appHost: AppHost,
        crashReportingLogr: @escaping (AppBuildConfig) -> ICrashReportingLogr,
        locationManager: @escaping () -> CoreLocationManagerDelegate,
        appUpdateManager: @escaping () -> IAppUpdateManager,
        appRatingPresenter: @escaping () -> IAppRatingPresenter,
        billingProvider: @escaping () -> IBillingProvider,
        locationStatusProvider: @escaping () -> ILocationStatusProvider,
        connectionStatusProvider: @escaping (AppBuildConfig) -> IConnectionStatusProvider,
        notificationStatusProvider: @escaping () -> INotificationStatusProvider
    ) {
        
        initializationQueue.sync {
            if (_koin != nil) {
                return
            }
            
            let koinApp = ParalinoCore.shared.doInit(
                preLaunch: { koinApp in koinApp },
                appModules: CoreKoinSetup.createKoinModules(
                    apphost: appHost,
                    crashReportingLogr: crashReportingLogr,
                    locationManager: locationManager,
                    appUpdateManager: appUpdateManager,
                    appRatingPresenter: appRatingPresenter,
                    billingProvider: billingProvider,
                    locationStatusProvider: locationStatusProvider,
                    connectionStatusProvider: connectionStatusProvider,
                    notificationStatusProvider: notificationStatusProvider
                )
            )
            
            _koin = koinApp.koin
        }
    }
    
    private static func createKoinModules(
        apphost: AppHost,
        crashReportingLogr: @escaping (AppBuildConfig) -> ICrashReportingLogr,
        locationManager: @escaping () -> CoreLocationManagerDelegate,
        appUpdateManager: @escaping () -> IAppUpdateManager,
        appRatingPresenter: @escaping () -> IAppRatingPresenter,
        billingProvider: @escaping () -> IBillingProvider,
        locationStatusProvider: @escaping () -> ILocationStatusProvider,
        connectionStatusProvider: @escaping (AppBuildConfig) -> IConnectionStatusProvider,
        notificationStatusProvider: @escaping () -> INotificationStatusProvider
    ) -> [Koin_coreModule] {
        
        let systemBundleId = Bundle.main.bundleIdentifier ?? "app.paralino.ios"
        let bundleId = systemBundleId.starts(with: "app.paralino.ios.test") ? "app.paralino.ios.test" : "app.paralino.ios"
        
        let appBuildConfig = AppBuildConfig(
            serverUrl: Bundle.main.object(forInfoDictionaryKey: "paralinoServerUrl") as! String,
            projectId: Bundle.main.object(forInfoDictionaryKey: "paralinoServerKey") as! String,
            publicServerUrl: Bundle.main.object(forInfoDictionaryKey: "paralinoPublicServerUrl") as! String,
            billingRCApiKey: Bundle.main.object(forInfoDictionaryKey: "revenueCatApiKey") as! String,
            iOSTeamId: Bundle.main.object(forInfoDictionaryKey: "TEAM_ID") as! String,
            iOSAppGroup: "group.\(bundleId)",
            appPackageName: bundleId,
            appHost: apphost,
            isDevMode: (Bundle.main.object(forInfoDictionaryKey: "internalTesting") as! String) == "true",
            hasPreviewFeatures: (Bundle.main.object(forInfoDictionaryKey: "internalTesting") as! String) == "true",
            logToConsole: (Bundle.main.object(forInfoDictionaryKey: "forceLogToSystem") as! String) == "true",
            logErrorsToRemote: true,
            hasGoogleMaps: false,
            hasNotificationSocketService: false,
            appFlavor: AppFlavor.appleIos
        )
        
//        let locationStatusProvider = LocationStatusProvider()
//        let connectionStatusProvider = ConnectionStatusProvider(appBuildConfig: appBuildConfig)
//        let notificationStatusProvider = NotificationStatusProvider()
        
        let locationManager = locationManager()
        let locationStatusProvider = locationStatusProvider()
        let connectionStatusProvider = connectionStatusProvider(appBuildConfig)
        let notificationStatusProvider = notificationStatusProvider()
        
        let deviceApiProvider = DeviceApiProvider(
            appBuildConfig: appBuildConfig,
            locationStatusProvider: locationStatusProvider,
            connectionStatusProvider: connectionStatusProvider,
            notificationStatusProvider: notificationStatusProvider
        )
        
        let coreIOSModules = KoinIOS.shared.createIOSKoinModules(
            deviceProvider: deviceApiProvider,
            crashReportingLogr: { crashReportingLogr(appBuildConfig) },
            connectionStatusProvider: connectionStatusProvider,
            locationStatusProvider: locationStatusProvider,
            notificationStatusProvider: notificationStatusProvider,
            pushSetupProvider: { PushSetupProvider() },
            placeNotificationPresenter: { inAppLogger in PlaceNotificationPresenter(inAppLogger: inAppLogger) },
            groupActivityNotificationPresenter: { inAppLogger in GroupActivityNotificationPresenter(inAppLogger: inAppLogger) },
            infoNotificationPresenter: { inAppLogger in InfoNotificationPresenter(inAppLogger: inAppLogger) },
            geofenceManager: { GeofenceManager(locationManager: locationManager) },
            appBuildConfig: appBuildConfig,
            oneTimeLocationProvider: { locationStatusProvider, locationCacheRepository in
                OneTimeLocationProvider(
                    locationManager: locationManager,
                    locationStatusProvider: locationStatusProvider,
                    locationCacheRepo: locationCacheRepository
                )
            },
            remoteModule: { sessionMonitor, networkQualityMonitor, appConfig in AppwriteRemoteModule(sessionMonitor: sessionMonitor, appConfig: appConfig, networkQualityMonitor: networkQualityMonitor) },
            documentsApi: { module, _ in DocumentsApi(remoteModule: module as! AppwriteRemoteModule) },
            userApi: { module, _ in UserApi(remoteModule: module as! AppwriteRemoteModule) },
            functionsApi: { module, _ in FunctionsApi(remoteModule: module as! AppwriteRemoteModule, deviceApiProvider: deviceApiProvider) },
            realtimeApi: { module, _ in RealtimeApi(remoteModule: module as! AppwriteRemoteModule) },
            cryptoServiceProvider: { CryptoFrameworkProvider() },
            valueFormatter: { prefs in ValueFormatter(appPrefs: prefs) },
            appUpdateManager: appUpdateManager,
            billingProvider: billingProvider,
            appRatingPresenter: appRatingPresenter
        )
        
        return coreIOSModules
    }

}

private var _koin: Koin_coreKoin?
var koin: Koin_coreKoin {
    return _koin!
}
