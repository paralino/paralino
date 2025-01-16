//
//  CrashReportingLogr.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-11-09.
//
import shared
import Sentry

class CrashReportingLogr: ICrashReportingLogr {
    
    let canLogToRemote: Bool
    
    init(canLogToRemote: Bool) {
        self.canLogToRemote = canLogToRemote
    }
    
    override func systemLog(priority: LogPriority, tag: String?, message: String, fullOutput: String, t: KotlinThrowable?) {
        guard canLogToRemote else { return }
        guard priority == LogPriority.error else { return }
        guard let error = t?.description() else { return }
        if t is InternalConnectionException { return }
        
        SentrySDK.capture(error: KotlinError.generic(message: message, stackTrace: error))
    }
}
