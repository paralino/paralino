//
//  Logr.swift
//  paralino-ios
//
//  Created by Zelimir on 25.03.23.
//

import Foundation
import shared
import os

let Logr = shared.Logr.shared

class DebbugLogr: SystemLogr {
    
    let logger = Logger()
    
    override func systemLog(priority: LogPriority, tag: String?, message: String, t: KotlinThrowable?) {
        
        let time = Date.now.ISO8601Format(.iso8601(timeZone: TimeZone.current, includingFractionalSeconds: true))
        let tagElement = tag != nil && !tag!.isEmpty ? "[\(tag!)] - " : ""
        let output = "\(time) | \(tagElement)\(message)"
        
        switch priority {
        case .info:
            logger.info("🟢 \(output)")
        case .debug:
            logger.debug("🟤 \(output)")
        case .verbose:
            logger.debug("⚪️ \(output)")
        case .warn:
            logger.warning("🟠 \(output)")
        case .error:
            logger.error("🔴 \(output)")
        default:
            logger.notice("⚪️ \(output)")
        }
    }
}
