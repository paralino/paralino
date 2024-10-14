//
//  KoinSetup.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-08.
//

import Foundation
import shared

public class CoreKoinSetup {
    
    public static var koinApp: Koin_coreKoin? {
        _koin
    }
    
    public static func initKoin(
        appHost: AppHost,
        locationManager: LocationManager,
        appUpdateManager: IAppUpdateManager,
        billingProvider: IBillingProvider
    ) {
        
        if (_koin != nil) {
            return
        }
        
        let koinApp = ParalinoCore.shared.doInit(
            preLaunch: { koinApp in koinApp },
            appModules: CoreKoinSetup.createKoinModules(
                apphost: appHost,
                locationManager: locationManager,
                appUpdateManager: appUpdateManager,
                billingProvider: billingProvider
            )
        )
        _koin = koinApp.koin
    }
    
    private static func createKoinModules(
        apphost: AppHost,
        locationManager: LocationManager,
        appUpdateManager: IAppUpdateManager,
        billingProvider: IBillingProvider
    ) -> [Koin_coreModule] {
        
        let appBuildConfig = AppBuildConfig(
            serverUrl: Bundle.main.object(forInfoDictionaryKey: "paralinoServerUrl") as! String,
            projectId: Bundle.main.object(forInfoDictionaryKey: "paralinoServerKey") as! String,
            publicServerUrl: Bundle.main.object(forInfoDictionaryKey: "paralinoPublicServerUrl") as! String,
            // TODO add key
            billingRCApiKey: "",
            iOSTeamId: Bundle.main.object(forInfoDictionaryKey: "TEAM_ID") as! String,
            iOSAppGroup: "group.app.paralino.ios",
            appHost: apphost,
            isDevMode: (Bundle.main.object(forInfoDictionaryKey: "internalTesting") as! String) == "true",
            hasPreviewFeatures: (Bundle.main.object(forInfoDictionaryKey: "internalTesting") as! String) == "true",
            logToConsole: (Bundle.main.object(forInfoDictionaryKey: "forceLogToSystem") as! String) == "true",
            logErrorsToRemote: true
        )
        
        let locationStatusProvider = LocationStatusProvider()
        let connectionStatusProvider = ConnectionStatusProvider(appBuildConfig: appBuildConfig)
        let deviceApiProvider = DeviceApiProvider(
            appBuildConfig: appBuildConfig,
            locationStatusProvider: locationStatusProvider,
            connectionStatusProvider: connectionStatusProvider
        )
        
        let coreIOSModules = KoinIOS.shared.createIOSKoinModules(
            deviceProvider: deviceApiProvider,
            connectionStatusProvider: connectionStatusProvider,
            locationStatusProvider: locationStatusProvider,
            pushSetupProvider: PushSetupProvider(),
            placeNotificationPresenter: { inAppLogger in PlaceNotificationPresenter(inAppLogger: inAppLogger) },
            groupActivityNotificationPresenter: { inAppLogger in GroupActivityNotificationPresenter(inAppLogger: inAppLogger) },
            infoNotificationPresenter: { inAppLogger in InfoNotificationPresenter(inAppLogger: inAppLogger) },
            geofenceManager: GeofenceManager(locationManager: locationManager.manager),
            appBuildConfig: appBuildConfig,
            oneTimeLocationProvider: { locationStatusProvider, locationCacheRepository in
                OneTimeLocationProvider(
                    locationManager: locationManager,
                    locationStatusProvider: locationStatusProvider,
                    locationCacheRepo: locationCacheRepository
                )
            },
            remoteModule: { sessionMonitor in AppwriteRemoteModule(sessionMonitor: sessionMonitor, appBuildConfig: appBuildConfig) },
            documentsApi: { module, _ in DocumentsApi(remoteModule: module as! AppwriteRemoteModule) },
            userApi: { module, _ in UserApi(remoteModule: module as! AppwriteRemoteModule) },
            functionsApi: { module, _ in FunctionsApi(remoteModule: module as! AppwriteRemoteModule, deviceApiProvider: deviceApiProvider) },
            realtimeApi: { module, _ in RealtimeApi(remoteModule: module as! AppwriteRemoteModule) },
            cryptoServiceProvider: CryptoFrameworkProvider(),
            valueFormatter: ValueFormatter(),
            appUpdateManager: appUpdateManager,
            billingProvider: billingProvider
        )
        
        return coreIOSModules
    }

}

private var _koin: Koin_coreKoin?
var koin: Koin_coreKoin {
    return _koin!
}
