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
    
    var data: T? {
        switch self {
        case .notInitialized: return nil
        case .ready(let content): return content
        }
    }
    
    public init(data: Any?) {
        if data == nil {
            self = .notInitialized
        } else if data is shared.ResourceNotInitialized {
            self = .notInitialized
        } else if let obj = data as? shared.ResourceReady<T> {
            self = .ready(data: obj.data!)
        } else if let data = data as? T {
            self = .ready(data: data)
        } else {
            self = .notInitialized
        }
    }
}

public enum PayloadSwift<T : AnyObject> {
    
    case loading
    case success(data: T)
    case error(error: KotlinThrowable)
    
    var dataOrNil: T? {
        switch self {
        case .loading: return nil
        case .success(data: let data): return data
        case .error(error: _): return nil
        }
    }
    
    public init(data: Any?) {
        if data == nil {
            self = .loading
        } else if let obj = data as? shared.PayloadSuccess<T> {
            self = .success(data: obj.data!)
        } else if let obj = data as? shared.PayloadError<T> {
            self = .error(error: obj.exception)
        } else if data is PayloadLoading {
            self = .loading
        } else if let data = data as? T {
            self = .success(data: data)
        } else {
            self = .error(error: KotlinException(message: "Invalid payload type"))
        }
    }
}
