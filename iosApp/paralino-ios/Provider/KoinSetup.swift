//
//  KoinSetup.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared
import paralino_apple_core

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
}
