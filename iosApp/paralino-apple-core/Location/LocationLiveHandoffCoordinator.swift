//
//  LocationLiveHandoffCoordinator.swift
//  paralino-ios
//
//  Created by Zelimir on 2026-05-27.
//

import CoreLocation
import Foundation
import shared

public final class LocationLiveHandoffCoordinator: @unchecked Sendable {
    
    public static let shared = LocationLiveHandoffCoordinator()
    
    public static let darwinHandoffNotificationName = CFNotificationName(
        "app.paralino.ios.locationLiveHandoff" as CFString
    )
    
    private static let mainAppSessionIdleSeconds: UInt64 = 50
    
    private var mainAppIdleTask: Task<Void, Never>?
    private var handoffHandler: (@Sendable () -> Void)?
    private var isLiveActive = false
    
    private init() {}
    
    private static func recordLSEHandoffPing() {
        let now = Date().timeIntervalSince1970
        AppPrefs.companion.instance.iOSLastLSEHandoffPingTime = now
        Logr.d(
            tag: "location_service",
            message: "Recorded LSE handoff ping: \(now)"
        )
    }
    
    public func hasRecentHandoffPing() -> Bool {
        let pingTime = AppPrefs.companion.instance.iOSLastLSEHandoffPingTime
        let now = Date().timeIntervalSince1970
        let age = now - pingTime
        Logr.v(
            tag: "location_service",
            message: "Checking LSE handoff ping: age is \(age) seconds"
        )
        return age >= 0 && age < 30.0 // True if ping occurred in last 60 seconds
    }
    
    public func clearHandoffPing() {
        AppPrefs.companion.instance.iOSLastLSEHandoffPingTime = 0
        Logr.d(
            tag: "location_service",
            message: "Cleared LSE handoff ping"
        )
    }
    
    public func recordMainAppHeartbeat() {
        let now = Date().timeIntervalSince1970
        AppPrefs.companion.instance.iOSLastMainAppHeartbeatTime = now
    }
    
    public func isMainAppActivelyRunning() -> Bool {
        let heartbeatTime = AppPrefs.companion.instance.iOSLastMainAppHeartbeatTime
        let now = Date().timeIntervalSince1970
        let age = now - heartbeatTime
        Logr.i(
            tag: "location_service",
            message: "Checking Main App heartbeat: age is \(age) seconds"
        )
        return age >= 0 && age < 5.0 // < 5 seconds old
    }
    
    public func isMainAppLSESessionActive() -> Bool {
        return isLiveActive
    }
    
    public func handleLocationPushExtension(
        startLseLocationManager: @escaping @Sendable () -> Void,
        completion: @escaping @Sendable () -> Void
    ) {
        Self.recordLSEHandoffPing()
        Self.postHandoffNotification()
        
        // Wait a bit to give the main app a chance to wake up
        DispatchQueue.global().asyncAfter(deadline: .now() + 2.0) { [weak self] in
            guard let self = self else {
                completion()
                return
            }
            
            if self.isMainAppActivelyRunning() {
                Logr.i(
                    tag: "location_service",
                    message: "LSE: Main app successfully responded and is active. Skipping LSE tracking."
                )
                completion()
                return
            }
            
            Logr.i(tag: "location_service", message: "LSE: Main app did not respond within 2s. Start LSE tracking.")
            startLseLocationManager()
        }
    }
    
    public func extensionWillTerminate() {
        Self.postHandoffNotification()
    }
    
    public func registerMainAppHandoffObserver(
        onHandoff: @escaping @Sendable () -> Void
    ) {
        handoffHandler = onHandoff
        
        let center = CFNotificationCenterGetDarwinNotifyCenter()
        CFNotificationCenterAddObserver(
            center,
            Unmanaged.passUnretained(self).toOpaque(),
            Self.darwinHandoffCallback,
            Self.darwinHandoffNotificationName.rawValue,
            nil,
            .deliverImmediately
        )
    }
    
    public func beginMainAppLSESession() {
        self.isLiveActive = true
        
        // Record heartbeat immediately when starting the session so LSE can check control instantly.
        recordMainAppHeartbeat()
        LocationServiceSession.shared.ensureAlwaysSession(reason: "main app live session")
        renewMainAppIdleTimer()
    }
    
    public func stopMainAppLSESession() {
        isLiveActive = false
        mainAppIdleTask?.cancel()
        mainAppIdleTask = nil
        
        LocationServiceSession.shared.invalidate(reason: "main app live session ended")
        Logr.i(tag: "location_service", message: "Main app live location session ended")
    }
    
    private static let darwinHandoffCallback: CFNotificationCallback = { _, observer, _, _, _ in
        guard let observer else { return }
        let coordinator = Unmanaged<LocationLiveHandoffCoordinator>
            .fromOpaque(observer)
            .takeUnretainedValue()
        coordinator.deliverMainAppHandoff()
    }
    
    private func deliverMainAppHandoff() {
        let handler = handoffHandler
        DispatchQueue.main.async {
            handler?()
        }
    }
    
    public func renewMainAppIdleTimer() {
        mainAppIdleTask?.cancel()
        mainAppIdleTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: Self.mainAppSessionIdleSeconds * 1_000_000_000)
            guard !Task.isCancelled else { return }
            Logr.i(tag: "location_service", message: "Main app live session idle timeout reached")
            self?.stopMainAppLSESession()
        }
    }
    
    public static func postHandoffNotification() {
        CFNotificationCenterPostNotification(
            CFNotificationCenterGetDarwinNotifyCenter(),
            darwinHandoffNotificationName,
            nil,
            nil,
            true
        )
    }
}
