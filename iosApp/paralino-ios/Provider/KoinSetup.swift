//
//  KoinSetup.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared

func createKoinModules(locationStatusProvider: LocationStatusProvider) -> [Koin_coreModule] {
    
    let backendConfig = BackendConfig(
        serverUrl: "",
        projectId: "",
        debug: true,
        canUseEmail: true,
        logToConsole: true,
        logErrorsToRemote: true
    )
    
    let connectionStatusProvider = ConnectionStatusProvider(backendConfig: backendConfig)
    
    let coreIOSModules = KoinIOS.shared.createIOSKoinModules(
        deviceProvider: DeviceApiProvider(locationStatusProvider: locationStatusProvider, connectionStatusProvider: connectionStatusProvider),
        connectionStatusProvider: connectionStatusProvider,
        locationStatusProvider: locationStatusProvider,
        pushSetupProvider: PushSetupProvider(),
        placeNotificationHandler: PlaceNotificationHandler(),
        groupActivityNotificationHandler: GroupActivityNotificationHandler(),
        geofenceManager: GeofenceManager(),
        backendConfig: backendConfig,
        remoteModule: { sessionMonitor in AppwriteRemoteModule(sessionMonitor: sessionMonitor, backendConfig: backendConfig) },
        documentsApi: { module, _ in DocumentsApi(remoteModule: module as! AppwriteRemoteModule) },
        userApi: { module, _ in UserApi(remoteModule: module as! AppwriteRemoteModule) },
        functionsApi: { module, _ in FunctionsApi(remoteModule: module as! AppwriteRemoteModule) },
        realtimeApi: { module, _ in RealtimeApi(remoteModule: module as! AppwriteRemoteModule) }
    )
    return coreIOSModules
}
