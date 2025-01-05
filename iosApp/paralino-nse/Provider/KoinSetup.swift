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
            crashReportingLogr: { appBuildConfig in CrashReportingLogr() },
            locationManager: { NseLocationManager() },
            appUpdateManager: { AppUpdateManager() },
            appRatingPresenter: { AppRatingPresenter() },
            billingProvider: { BillingProviderIOS() },
            locationStatusProvider: { LocationStatusProvider() },
            connectionStatusProvider: { appBuildConfig in ConnectionStatusProvider(appBuildConfig: appBuildConfig) },
            notificationStatusProvider: { NotificationStatusProvider() }
        )
    }
    
    // MARK: NO-OP classes
    // Keep them empty since they are not used in notification service
    
    class CrashReportingLogr: ICrashReportingLogr {
        override func systemLog(priority: LogPriority, tag: String?, message: String, fullOutput: String, t: KotlinThrowable?) {}
    }
    
    class AppUpdateManager: IAppUpdateManager {
        override func startUpdateProcess(data: ConfigData) {}
    }
    
    class AppRatingPresenter: IAppRatingPresenter {
        func startAppRatingFlow() {}
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
