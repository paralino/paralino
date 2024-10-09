//
//  KoinSetup.swift
//  paralino-nse
//
//  Created by Zelimir on 2024-10-08.
//

import Foundation
import paralino_apple_core
import shared

class KoinSetup {
    
    static func initKoin(locationManager: LocationManager) {
        CoreKoinSetup.initKoin(
            locationManager: locationManager,
            appUpdateManager: AppUpdateManager(),
            billingProvider: BillingProviderIOS()
        )
    }
    
    // MARK: NO-OP classes
    // Keep them empty since they are not used in notification service
    
    class AppUpdateManager: IAppUpdateManager {
        override func startUpdateProcess(data: ConfigData) {}
    }
    
    class BillingProviderIOS: IBillingProvider {
        func configure() async throws -> [IBillingProviderSubscriptionPlanData] {
            return []
        }
    }
}
