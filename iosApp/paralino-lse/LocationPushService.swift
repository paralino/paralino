//
//  LocationPushService.swift
//  paralino-lse
//
//  Created by Zelimir on 2024-10-14.
//

import CoreLocation
import paralino_apple_core
import shared
//import Sentry

class LocationPushService: NSObject, CLLocationPushServiceExtension, CLLocationManagerDelegate {
    
    lazy var locationRepo: LocationRepositoryWrapper = { KoinIOS.Instances.shared.locationRepo }()
    
    var completion: (() -> Void)?
    var locationManager: CLLocationManager?
    
    override init() {
//        SentrySDK.start { options in
//            options.dsn = BuildKonfig.shared.glitchTipDSN
//            options.debug = false
//        }
    
        KoinSetup.initKoin(locationManager: LseLocationManager())
        Logr.i(tag: "location_service", message: "Start location push service")
        
        super.init()
    }

    func didReceiveLocationPushPayload(_ payload: [String : Any], completion: @escaping () -> Void) {
        
        KoinSetup.initKoin(locationManager: LseLocationManager())
        Logr.i(tag: "location_service", message: "Got location request payload")
        
        locationRepo.startLocationUploadEngine()
        
        self.completion = completion
        self.locationManager = CLLocationManager()
        self.locationManager?.distanceFilter = 2
        self.locationManager?.desiredAccuracy = kCLLocationAccuracyBest
        self.locationManager!.delegate = self
        self.locationManager!.startUpdatingLocation()
    }
    
    func serviceExtensionWillTerminate() {
        Logr.i(tag: "location_service", message: "LSE will terminate")
        self.locationManager?.stopUpdatingLocation()
        self.completion?()
    }

    // MARK: - CLLocationManagerDelegate methods
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let location = locations.last?.toLocationModel()
        Logr.i(tag: "location_service", message: "Got location update from system \(location != nil)")
        
        if location == nil { return }
        
        let systemLocation = manager.location?.toLocationModel()
        let locationModel = LocationDataModel.companion.getBetter(first: location, second: systemLocation)
        
        locationRepo.registerNewLocation(data: locationModel!)
        
        Logr.i(tag: "location_service", message: "New location registered \(locations.count)")
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        Logr.i(tag: "location_service", message: "Location manager failed with error \(error.localizedDescription)")
        self.completion?()
    }
}


class LseLocationManager: NSObject, CoreLocationManagerDelegate {
    
    public var lastLocation: CLLocation? {
        nil
    }
    
    public var monitoredRegions: Set<CLRegion> {
        []
    }
    
    public func startMonitoringRegion(region: CLCircularRegion) {}
    public func stopMonitoringRegion(region: CLRegion) {}
}
