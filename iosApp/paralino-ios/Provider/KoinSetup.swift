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
    
    let coreIOSModules = KoinIOS.shared.createIOSKoinModules(
        deviceProvider: DeviceApiProvider(),
        connectionStatusProvider: ConnectionStatusProvider(backendConfig: backendConfig),
        locationStatusProvider: locationStatusProvider,
        pushSetupProvider: PushSetupProvider(),
        placeNotificationHandler: PlaceNotificationHandler(),
        groupActivityNotificationHandler: GroupActivityNotificationHandler(),
        geofenceManager: GeofenceManager(),
        backendConfig: backendConfig
    )
    return coreIOSModules
}
