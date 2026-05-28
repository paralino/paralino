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
    
    private lazy var locationRepo: LocationRepositoryWrapper = { KoinIOS.Instances.shared.locationRepo }()
    private let locationManagerDelegate = LseLocationManager()
    
    private var pushCompletion: (() -> Void)?
    private var locationManager: CLLocationManager?
    
    
    override init() {
        KoinSetup.initKoin(locationManager: locationManagerDelegate)
        super.init()
    }

    func didReceiveLocationPushPayload(_ payload: [String : Any], completion: @escaping () -> Void) {
        pushCompletion = completion
        
        KoinSetup.initKoin(locationManager: locationManagerDelegate)
        
        Logr.i(tag: "location_service", message: "LSE: Got location push payload. Pinging main app.")
        
        self.locationRepo.startLocationUploadEngine()
        
        self.locationManager = CLLocationManager()
        self.locationManager?.distanceFilter = 0
        self.locationManager?.desiredAccuracy = kCLLocationAccuracyBest
        self.locationManager?.delegate = self
        
        LocationLiveHandoffCoordinator.shared.handleLocationPushExtension(
            startLseLocationManager: { [weak self] in
                guard let self = self else { return }
                DispatchQueue.main.async {
                    Task {
                        try? await Task.sleep(nanoseconds: 500 * 1_000_000)
                        Logr.i(tag: "location_service", message: "Start location updates")
                        self.locationManager?.startUpdatingLocation()
                    }
                }
            },
            completion: { [weak self] in
                self?.completePushIfNeeded()
            }
        )
    }
    
    func serviceExtensionWillTerminate() {
        Logr.i(tag: "location_service", message: "LSE will terminate")
        self.locationManager?.stopUpdatingLocation()
        LocationLiveHandoffCoordinator.shared.extensionWillTerminate()
        completePushIfNeeded()
    }
    
    private func completePushIfNeeded() {
        self.pushCompletion?()
        self.pushCompletion = nil
    }

    // MARK: - CLLocationManagerDelegate methods
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        // If the main app wakes up and starts its session in the background, we can stop the LSE updates
        if LocationLiveHandoffCoordinator.shared.isMainAppActivelyRunning() {
            Logr.i(tag: "location_service", message: "LSE: Main app is now actively streaming. Stopping LSE location manager.")
            manager.stopUpdatingLocation()
            completePushIfNeeded()
            return
        }
        
        Logr.i(tag: "location_service", message: "LSE: got update \(locations.count), registering location")
        
        guard let location = locations.last?.toLocationModel() else { return }
        let systemLocation = manager.location?.toLocationModel()
        let locationModel = LocationDataModel.companion.getBetter(first: location, second: systemLocation)
        
        _ = locationRepo.registerNewLocation(data: locationModel!)
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        Logr.i(tag: "location_service", message: "LSE: Location manager failed with error: \(error.localizedDescription)")
        completePushIfNeeded()
    }
}

class LseLocationManager: NSObject, CoreLocationManagerDelegate {
    
    public var lastLocation: CLLocation? { nil }
    public var monitoredRegions: Set<CLRegion> { [] }
    
    public func startMonitoringRegion(region: CLCircularRegion) {}
    public func stopMonitoringRegion(region: CLRegion) {}
}
