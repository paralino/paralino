//
//  LseLocationManager.swift
//  paralino-lse
//
//  Created by Zelimir on 2024-10-17.
//

import Foundation
import CoreLocation
import paralino_apple_core
import shared

class LseLocationManager: NSObject, ObservableObject, CoreLocationManagerDelegate {
    
    var completion: (() -> Void)?
    public let manager = CLLocationManager()
    
    lazy private var locationRepo: LocationRepositoryWrapper = { KoinIOS.Instances.shared.locationRepo }()
    lazy private var locationSharingManager: LocationSharingManager = { KoinIOS.Instances.shared.locationSharingManager }()
    lazy private var locationPerformanceManager: LocationPerformanceManager = { KoinIOS.Instances.shared.locationPerformanceManager }()
    
    private var wasLocationPerformanceActive = false
    
    func start(notificationId: String?, _ completion: @escaping () -> Void) {
        
        self.completion = completion
        locationRepo.startLocationUploadEngine()
        startMonitoringAppState()
        startLocationUpdates(notificationId: notificationId)
    }
    
    public var lastLocation: CLLocation? {
        manager.location
    }
    
    public var monitoredRegions: Set<CLRegion> {
        manager.monitoredRegions
    }
    
    public func startMonitoringRegion(region: CLCircularRegion) {}
    public func stopMonitoringRegion(region: CLRegion) {}
    
    private func startMonitoringAppState() {
        locationPerformanceManager.locationPerformanceStatus { [weak self] model in
            Logr.i(tag: "location_service", message: "Activate performance mode: \(model.current)")
            switch model.current {
            case .highBatterySaver, .batterySaver, .balanced:
                if self?.wasLocationPerformanceActive == true {
                    self?.stopLocationUpdates()
                }
            case .mediumHigh:
                self?.wasLocationPerformanceActive = true
                self?.manager.pausesLocationUpdatesAutomatically = true
                self?.manager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
                self?.manager.distanceFilter = 50
            case .high:
                self?.wasLocationPerformanceActive = true
                self?.manager.pausesLocationUpdatesAutomatically = false
                self?.manager.desiredAccuracy = kCLLocationAccuracyBest
                self?.manager.distanceFilter = kCLDistanceFilterNone
            case .veryHigh, .manualBoost:
                self?.wasLocationPerformanceActive = true
                self?.manager.pausesLocationUpdatesAutomatically = false
                self?.manager.desiredAccuracy = kCLLocationAccuracyBest
                self?.manager.distanceFilter = kCLDistanceFilterNone
            default: break
            }
        }
    }
    
    private func startLocationUpdates(notificationId: String?) {
        DispatchQueue.main.async { [weak self] in
            Task {
                if
                    let canProcess = try? await self?.locationRepo.canProcessLocationRequest().boolValue,
                    let result = try? await self?.locationRepo.startLocationRequest(notificationId: notificationId)
                {
                    Logr.i(tag: "location_service", message: "Start location updates from manager")
                    self?.manager.startUpdatingLocation()
                } else {
                    self?.stopLocationUpdates()
                }
            }
        }
    }
    
    private func stopLocationUpdates() {
        Logr.i(tag: "location_service", message: "Stop location updates")
        manager.stopUpdatingLocation()
        finish()
    }
    
    func finish() {
        Logr.i(tag: "location_service", message: "Finish location updates")
        DispatchQueue.main.async { [weak self] in
            Task {
                self?.locationPerformanceManager.disableManualBoost()
                try? await self?.locationRepo.deleteLocationRequestLog()
            }
        }
        
        Logr.i(tag: "location_service", message: "Call completion for location updates")
        if let completion = self.completion {
            completion()
        }
    }
    
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let location = locations.last?.toLocationModel()
        Logr.i(tag: "location_service", message: "Got location update from system \(location != nil)")
        
        if location == nil { return }
        
        DispatchQueue.main.async { [weak self] in
            Task {
                if let canProcess = try? await self?.locationRepo.canProcessLocationRequest().boolValue {
                    
                    let systemLocation = manager.location?.toLocationModel()
                    let locationModel = LocationDataModel.companion.getBetter(first: location, second: systemLocation)
                    
                    Logr.i(tag: "location_service", message: "Can process location update from system \(locationModel != nil)")
                    
                    self?.locationRepo.registerNewLocation(data: locationModel!)
                } else {
                    self?.stopLocationUpdates()
                }
            }
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: any Error) {
        Logr.i(tag: "location_service", message: "Location manager did fail with error \(error)")
        finish()
    }
}
