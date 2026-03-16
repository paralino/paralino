//
//  KoinSetup.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared
import paralino_apple_core
import CoreLocation
import UIKit

class KoinSetup {
    static func initKoin(
        locationManager: LocationManager,
        appRatingPresenter: AppRatingPresenter
    ) {
        CoreKoinSetup.initKoin(
            appHost: AppHost.mainApp,
            crashReportingLogr: { appBuildConfig in CrashReportingLogr(canLogToRemote: appBuildConfig.logErrorsToRemote) },
            locationManager: { locationManager },
            appUpdateManager: { AppUpdateManager() },
            appRatingPresenter: { appRatingPresenter },
            billingProvider: { BillingProviderIOS() },
            locationStatusProvider: { LocationStatusProvider() },
            connectionStatusProvider: { appBuildConfig in ConnectionStatusProvider(appBuildConfig: appBuildConfig) },
            notificationStatusProvider: { NotificationStatusProvider() }
        )
    }
    
    class LocationStatusProvider: ILocationStatusProvider {
        override func createCurrentStatus() -> LocationConditionStatus {
            let isBackgroundRefreshAllowed: Bool = {
                // calling backgroundRefreshStatus can only be done in main thread
                if Thread.isMainThread {
                    return UIApplication.shared.backgroundRefreshStatus != .denied
                } else {
                    // we do not care about this value currently, only used in foreground for one specific UI state
                    return true
                }
            }()
            
            let locationManager = CLLocationManager()
            return locationManager.locationCondtionStatus(
                isUnrestrictedBackgroundPermissionAllowed: isBackgroundRefreshAllowed
            )
        }
    }
}
