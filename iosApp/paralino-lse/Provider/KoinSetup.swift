//
//  KoinSetup.swift
//  paralino-lse
//
//  Created by Zelimir on 2024-10-14.
//

import Foundation
import paralino_apple_core
import shared
import Network
import CoreTelephony
import CoreLocation

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
        var isExternalProvider: Bool {
            return false
        }
        func startDirectPurchase(url: String) async throws {}
        func configure() async throws -> IBillingProviderSubscriptionProviderData {
            return IBillingProviderSubscriptionProviderData(plans: [], billingConfig: nil, canCloseOnboardingPaywall: true, billingSelectorPosition: nil)
        }
    }
    
    class LocationStatusProvider: ILocationStatusProvider {
        override func createCurrentStatus() -> LocationConditionStatus {
            let locationManager = CLLocationManager()
            return locationManager.locationCondtionStatus(isUnrestrictedBackgroundPermissionAllowed: true)
        }
    }
    
    class NotificationStatusProvider: INotificationStatusProvider {
        override func createCurrentStatus() -> NotificationConditionStatus {
            return NotificationConditionStatus(isNotificationPermissionAllowed: true)
        }
    }
}
