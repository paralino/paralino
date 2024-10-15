//
//  LocationPushService.swift
//  paralino-lse
//
//  Created by Zelimir on 2024-10-14.
//

import CoreLocation
import paralino_apple_core
import shared

class LocationPushService: NSObject, CLLocationPushServiceExtension, CLLocationManagerDelegate {

    var completion: (() -> Void)?
    var clLocationManager: CLLocationManager?
    let locationManager: LocationManager
    
    override init() {
        self.locationManager = LocationManager(setupManager: false)
        self.locationManager.manager.delegate = locationManager
        KoinSetup.initKoin(locationManager: self.locationManager)
    }

    func didReceiveLocationPushPayload(_ payload: [String : Any], completion: @escaping () -> Void) {
        self.completion = completion
        self.clLocationManager = CLLocationManager()
        self.clLocationManager!.delegate = self
        self.clLocationManager!.requestLocation()
    }
    
    func serviceExtensionWillTerminate() {
        // Called just before the extension will be terminated by the system.
        self.completion?()
    }

    // MARK: - CLLocationManagerDelegate methods
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        // Process the location(s) as appropriate
        // let location = locations.first

        // If sharing the locations to another user, end-to-end encrypt them to protect privacy
        
        // When finished, always call completion()
        self.completion?()
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        self.completion?()
    }

}
