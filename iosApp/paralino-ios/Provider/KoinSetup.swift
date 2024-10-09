//
//  KoinSetup.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import paralino_apple_core

class KoinSetup {
    static func initKoin(locationManager: LocationManager) {
        CoreKoinSetup.initKoin(
            locationManager: locationManager,
            appUpdateManager: AppUpdateManager(),
            billingProvider: BillingProviderIOS()
        )
    }
}
