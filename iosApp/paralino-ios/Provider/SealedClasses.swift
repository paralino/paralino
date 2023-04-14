//
//  SealedClasses.swift
//  paralino-ios
//
//  Created by Zelimir on 14.04.23.
//

import Foundation
import shared

public enum ResourceSwift<T : AnyObject> {
    
    case notInitialized
    case ready(data: T)
    
    public init(data: Any?) {
        self.init(data as! shared.Resource<T>)
    }
    
    public init(_ sealedClass: shared.Resource<T>) {
        if sealedClass is shared.ResourceNotInitialized {
            self = .notInitialized
        } else if let obj = sealedClass as? shared.ResourceReady<T> {
            self = .ready(data: obj.data!)
        } else {
            fatalError("Sealed class not synced with shared framework")
        }
    }
}

public enum PayloadSwift<T : AnyObject> {
    
    case loading
    case success(data: T)
    case error(error: KotlinThrowable)
    
    public init(data: Any?) {
        self.init(data as! shared.Payload<T>)
    }
    
    public init(_ sealedClass: shared.Payload<T>) {
        if sealedClass is shared.PayloadLoading {
            self = .loading
        } else if let obj = sealedClass as? shared.PayloadSuccess<T> {
            self = .success(data: obj.data!)
        } else if let obj = sealedClass as? shared.PayloadError<T> {
            self = .error(error: obj.exception)
        } else {
            fatalError("Sealed class not synced with shared framework")
        }
    }
}
