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
    
    func start(delegate: CLLocationManagerDelegate, _ completion: @escaping () -> Void) {
        self.manager.delegate = delegate
        self.completion = completion
        startMonitoringAppState()
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
        locationSharingManager.locationSharingStatus { [weak self] state in
            switch ResourceSwift<KotlinBoolean>.init(data: state) {
            case .notInitialized:
                break
            case .ready(data: let data):
                if data.boolValue {
                    self?.startLocationUpdates()
                } else {
                    self?.stopLocationUpdates()
                }
            }
        }
        
        locationPerformanceManager.locationPerformanceStatus { [weak self] model in
            switch model.current {
            case .batterySaver, .balanced:
                self?.stopLocationUpdates()
            case .mediumHigh:
                self?.manager.pausesLocationUpdatesAutomatically = true
                self?.manager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
                self?.manager.distanceFilter = 50
            case .high:
                self?.manager.pausesLocationUpdatesAutomatically = false
                self?.manager.desiredAccuracy = kCLLocationAccuracyBest
                self?.manager.distanceFilter = 2
            case .veryHigh, .manualBoost:
                self?.manager.pausesLocationUpdatesAutomatically = false
                self?.manager.desiredAccuracy = kCLLocationAccuracyBest
                self?.manager.distanceFilter = kCLDistanceFilterNone
            default: break
            }
        }
    }
    
    func startLocationUpdates() {
        DispatchQueue.main.async { [weak self] in
            Task {
                if let canProcess = try? await self?.locationRepo.canProcessLocationRequest().boolValue {
                    _ = try? await self?.locationRepo.startLocationRequest()
                    self?.manager.startUpdatingLocation()
                } else {
                    self?.stopLocationUpdates()
                }
            }
        }
    }
    
    func stopLocationUpdates() {
        manager.stopUpdatingLocation()
        finish()
    }
    
    func finish() {
        DispatchQueue.main.async { [weak self] in
            Task {
                try? await self?.locationRepo.deleteLocationRequestLog()
            }
        }
        if let completion = self.completion {
            completion()
        }
    }
    
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        DispatchQueue.main.async { [weak self] in
            Task {
                if let canProcess = try? await self?.locationRepo.canProcessLocationRequest().boolValue {
                    guard let location = locations.last?.toLocationModel() else { return }
                    let systemLocation = manager.location?.toLocationModel()
                    let locationModel = LocationDataModel.companion.getBetter(first: location, second: systemLocation)
                    
                    Logr.i(tag: "location_service", message: "Got location update from system \(locationModel != nil)")
                    
                    self?.locationRepo.registerNewLocation(data: locationModel!)
                } else {
                    self?.stopLocationUpdates()
                }
            }
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: any Error) {
        finish()
    }
}
