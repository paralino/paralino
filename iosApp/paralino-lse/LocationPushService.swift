//
//  LocationPushService.swift
//  paralino-lse
//
//  Created by Zelimir on 2024-10-14.
//

import CoreLocation
import paralino_apple_core
import shared
import Sentry

class LocationPushService: NSObject, CLLocationPushServiceExtension, CLLocationManagerDelegate {

    var locationManager: LseLocationManager = LseLocationManager()
    
    override init() {
        SentrySDK.start { options in
            options.dsn = BuildKonfig.shared.glitchTipDSN
            options.debug = false
        }
        
        KoinSetup.initKoin(locationManager: locationManager)
        
        super.init()
    }

    func didReceiveLocationPushPayload(_ payload: [String : Any], completion: @escaping () -> Void) {
        KoinSetup.initKoin(locationManager: locationManager)
        
        let notificationId = payload["notificationId"] as? String
        
        Logr.i(tag: "location_service", message: "Got location request payload")
        
        self.locationManager.manager.delegate = self
        self.locationManager.start(notificationId: notificationId, completion)
    }
    
    func serviceExtensionWillTerminate() {
        self.locationManager.finish()
    }

    // MARK: - CLLocationManagerDelegate methods
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        locationManager.locationManager(manager, didUpdateLocations: locations)
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        locationManager.locationManager(manager, didFailWithError: error)
    }
}
