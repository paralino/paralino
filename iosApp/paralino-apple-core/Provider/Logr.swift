//
//  Logr.swift
//  paralino-ios
//
//  Created by Zelimir on 25.03.23.
//

import Foundation
import shared
import os
import Sentry

let Logr = shared.Logr.shared

class DebbugLogr: SystemLogr {
    
    let canLogToConsole: Bool
    let logger = Logger()
    
    init(canLogToConsole: Bool) {
        self.canLogToConsole = canLogToConsole
    }
    
    override func systemLog(priority: LogPriority, tag: String?, message: String, t: KotlinThrowable?) {
        
        guard canLogToConsole else { return }
        
        let time = Date.now.ISO8601Format(.iso8601(timeZone: TimeZone.current, includingFractionalSeconds: true))
        let tagElement = tag != nil && !tag!.isEmpty ? "[\(tag!)] - " : ""
        let output = "\(time) | \(tagElement)\(message)"
        
        // using public as privacy option will enable logs in service extensions
        // we are controling when to log to console through canLogToConsole flags so public is not a problem here
        switch priority {
        case .info:
            logger.info("🟢 \(output, privacy: .public)")
        case .debug:
            logger.debug("🟤 \(output, privacy: .public)")
        case .verbose:
            logger.debug("⚪️ \(output, privacy: .public)")
        case .warn:
            logger.warning("🟠 \(output, privacy: .public)")
        case .error:
            logger.error("🔴 \(output, privacy: .public)")
        default:
            logger.notice("⚪️ \(output, privacy: .public)")
        }
    }
}

class KotlinError: Swift.Error, Decodable {
    let message: String
    let stackTrace: String
    init(message: String, stackTrace: String) {
        self.message = message
        self.stackTrace = stackTrace
    }
}
