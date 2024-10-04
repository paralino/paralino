//
//  KoinSetup.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared

func initKoin(locationStatusProvider: LocationStatusProvider) {
    let koinApp = ParalinoCore.shared.doInit(
        preLaunch: { koinApp in koinApp },
        appModules: createKoinModules(locationStatusProvider: locationStatusProvider)
    )
    _koin = koinApp.koin
}

func createKoinModules(locationStatusProvider: LocationStatusProvider) -> [Koin_coreModule] {
    
    let appBuildConfig = AppBuildConfig(
        serverUrl: Bundle.main.object(forInfoDictionaryKey: "paralinoServerUrl") as! String,
        projectId: Bundle.main.object(forInfoDictionaryKey: "paralinoServerKey") as! String,
        publicServerUrl: Bundle.main.object(forInfoDictionaryKey: "paralinoPublicServerUrl") as! String,
        isDevMode: (Bundle.main.object(forInfoDictionaryKey: "internalTesting") as! String) == "true",
        // TODO add key
        billingRCApiKey: "",
        hasPreviewFeatures: (Bundle.main.object(forInfoDictionaryKey: "internalTesting") as! String) == "true",
        logToConsole: (Bundle.main.object(forInfoDictionaryKey: "forceLogToSystem") as! String) == "true",
        logErrorsToRemote: true
    )
    
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
        placeNotificationPresenter: PlaceNotificationPresenter(),
        groupActivityNotificationPresenter: GroupActivityNotificationPresenter(),
        infoNotificationPresenter: InfoNotificationPresenter(),
        geofenceManager: GeofenceManager(),
        appBuildConfig: appBuildConfig,
        oneTimeLocationProvider: { locationStatusProvider, locationCacheRepository in
            OneTimeLocationProvider(
                locationManager: AppDelegate.shared.locationManager,
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
        appUpdateManager: AppUpdateManager(),
        billingProvider: BillingProviderIOS()
    )
    
    return coreIOSModules
}

private var _koin: Koin_coreKoin?
var koin: Koin_coreKoin {
    return _koin!
}
