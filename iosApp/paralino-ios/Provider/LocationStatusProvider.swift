//
//  LocationStatusProviderIOS.swift
//  paralino-ios
//
//  Created by Zelimir on 2026-10-12.
//

import shared
import CoreLocation
import UIKit

class LocationStatusProvider: ILocationStatusProvider, @unchecked Sendable {
    
    // backgroundRefreshStatus for isUnrestrictedBackgroundPermissionAllowed can only be read on main thread, so we have to cache it
    // and updated on main, which allows creating status from any thread
    private let lock = NSLock()
    private var _isBackgroundRefreshAllowed = false
    private var isBackgroundRefreshAllowed: Bool {
        get {
            lock.lock()
            defer { lock.unlock() }
            return _isBackgroundRefreshAllowed
        }
        set {
            lock.lock()
            defer { lock.unlock() }
            _isBackgroundRefreshAllowed = newValue
        }
    }
    private var observers: [NSObjectProtocol] = []
    
    override init() {
        super.init()
        
        // also checking on app active in case change notification was missed
        observers = [
            UIApplication.backgroundRefreshStatusDidChangeNotification,
            UIApplication.didBecomeActiveNotification
        ].map { name in
            NotificationCenter.default.addObserver(forName: name, object: nil, queue: .main) { [weak self] _ in
                self?.updateBackgroundRefreshStatus()
            }
        }
        
        updateBackgroundRefreshStatus()
    }

    deinit {
        observers.forEach { NotificationCenter.default.removeObserver($0) }
    }

    private func updateBackgroundRefreshStatus() {
        // backgroundRefreshStatus can only be read on main thread
        guard Thread.isMainThread else { return }

        let isAllowed = MainActor.assumeIsolated { UIApplication.shared.backgroundRefreshStatus != .denied }
        guard isAllowed != isBackgroundRefreshAllowed else { return }
        isBackgroundRefreshAllowed = isAllowed
        refreshLocationStatus()
    }
    
    override func createCurrentStatus() -> LocationConditionStatus {
        let locationManager = CLLocationManager()
        return locationManager.locationCondtionStatus(
            isUnrestrictedBackgroundPermissionAllowed: isBackgroundRefreshAllowed
        )
    }
}
