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
        if t is InternalConnectionException
            || t is RemoteConnectionException
            || t is TimeoutException
            || t is MissingGroupKeyException
            || t is KotlinCancellationException {
            return
        }
        // manually checking for HTTPClientError since it gets mapped to a typed exception
        if t?.message?.contains("HTTPClientError") == true { return }
        
        SentrySDK.capture(error: KotlinError.generic(message: message, stackTrace: error))
    }
}
