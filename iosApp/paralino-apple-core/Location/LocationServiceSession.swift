//
//  LocationServiceSession.swift
//  paralino-ios
//
//  Created by Zelimir on 2026-05-27.
//
//  Keeps a CLServiceSession alive while location sharing / LSE is in use.
//  Apple recommends holding this in both the app and the location push extension.
//

import CoreLocation
import Foundation

public final class LocationServiceSession {
    
    public static let shared = LocationServiceSession()
    private var session: AnyObject?
    
    private init() {}
    
    public func ensureAlwaysSession(reason: String) {
        guard session == nil else { return }
        guard #available(iOS 18.0, *) else { return }
        
        session = CLServiceSession(authorization: .always)
        
        Logr.i(
            tag: "location_service",
            message: "CLServiceSession(.always) created (\(reason))"
        )
    }
    
    public func invalidate(reason: String) {
        if #available(iOS 18.0, *), let session = session as? CLServiceSession {
            session.invalidate()
        }
        session = nil
        
        Logr.i(
            tag: "location_service",
            message: "CLServiceSession invalidated (\(reason))"
        )
    }
}

