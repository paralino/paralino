//
//  KoinSetup.swift
//  paralino-lse
//
//  Created by Zelimir on 2024-10-14.
//

import Foundation
import paralino_apple_core
import shared

class KoinSetup {
    
    static func initKoin(locationManager: CoreLocationManagerDelegate) {
        CoreKoinSetup.initKoin(
            appHost: AppHost.iosLocationExtension,
            crashReportingLogr: { appBuildConfig in CrashReportingLogr() },
            locationManager: { locationManager },
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
    
    class NotificationStatusProvider: INotificationStatusProvider {
        override func createCurrentStatus() -> NotificationConditionStatus {
            return NotificationConditionStatus(isNotificationPermissionAllowed: true)
        }
    }
}
