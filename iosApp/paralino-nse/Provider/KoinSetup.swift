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
    
    static func initKoin() {
        CoreKoinSetup.initKoin(
            appHost: AppHost.iosNotificationExtension,
            locationManager: { NseLocationManager() },
            appUpdateManager: { AppUpdateManager() },
            billingProvider: { BillingProviderIOS() },
            locationStatusProvider: { LocationStatusProvider() },
            connectionStatusProvider: { appBuildConfig in ConnectionStatusProvider(appBuildConfig: appBuildConfig) },
            notificationStatusProvider: { NotificationStatusProvider() }
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
    
    class LocationStatusProvider: ILocationStatusProvider {
        override func createCurrentStatus() -> LocationConditionStatus {
            return LocationConditionStatus(isGpsOn: true, isFinePermissionAllowed: false, isCoarsePermissionAllowed: false, isBackgroundPermissionAllowed: false)
        }
    }
    
    class NotificationStatusProvider: INotificationStatusProvider {
        override func createCurrentStatus() -> NotificationConditionStatus {
            return NotificationConditionStatus(isNotificationPermissionAllowed: true)
        }
    }
}
