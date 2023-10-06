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
    
    let backendConfig = BackendConfig(
        serverUrl: "",
        projectId: "",
        debug: true,
        logToConsole: true,
        logErrorsToRemote: true
    )
    
    let connectionStatusProvider = ConnectionStatusProvider(backendConfig: backendConfig)
    let deviceApiProvider = DeviceApiProvider(backendConfig: backendConfig, locationStatusProvider: locationStatusProvider, connectionStatusProvider: connectionStatusProvider)
    
    let coreIOSModules = KoinIOS.shared.createIOSKoinModules(
        deviceProvider: deviceApiProvider,
        connectionStatusProvider: connectionStatusProvider,
        locationStatusProvider: locationStatusProvider,
        pushSetupProvider: PushSetupProvider(),
        placeNotificationHandler: PlaceNotificationHandler(),
        groupActivityNotificationHandler: GroupActivityNotificationHandler(),
        locationRequestNotificationHandler: LocationRequestNotificationHandler(),
        geofenceManager: GeofenceManager(),
        backendConfig: backendConfig,
        remoteModule: { sessionMonitor in AppwriteRemoteModule(sessionMonitor: sessionMonitor, backendConfig: backendConfig) },
        documentsApi: { module, _ in DocumentsApi(remoteModule: module as! AppwriteRemoteModule) },
        userApi: { module, _ in UserApi(remoteModule: module as! AppwriteRemoteModule) },
        functionsApi: { module, _ in FunctionsApi(remoteModule: module as! AppwriteRemoteModule, deviceApiProvider: deviceApiProvider) },
        realtimeApi: { module, _ in RealtimeApi(remoteModule: module as! AppwriteRemoteModule) },
        cryptoServiceProvider: CryptoFrameworkProvider(),
        valueFormatter: ValueFormatter()
    )
    
    return coreIOSModules
}

private var _koin: Koin_coreKoin?
var koin: Koin_coreKoin {
    return _koin!
}
