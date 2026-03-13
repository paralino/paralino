//
//  ValueFormatter.swift
//  paralino-ios
//
//  Created by Zelimir on 26.06.23.
//

import Foundation
import shared

class ValueFormatter: IValueFormatter {
    
    private let appPrefs: AppPrefs
    
    init(appPrefs: AppPrefs) {
        self.appPrefs = appPrefs
    }
    
    override var measurementSystem: MeasurementSystemType {
        switch appPrefs.measurementSystem {
            case .imperial: return MeasurementSystemType.imperial
            case .metric: return MeasurementSystemType.metric
            default: return deviceDefaultMeasurementSystem
        }
    }
    
    override var deviceDefaultMeasurementSystem: MeasurementSystemType {
        let measurementSystem = Locale.current.measurementSystem
        if measurementSystem == .uk || measurementSystem == .us {
            return MeasurementSystemType.imperial
        } else {
            return MeasurementSystemType.metric
        }
    }
    
    override func formatNumber(number: Double, minFractions: Int32, maxFractions: Int32) -> String {
        let formatter = NumberFormatter()
        formatter.minimumFractionDigits = Int(minFractions)
        formatter.maximumFractionDigits = Int(maxFractions)
        return formatter.string(from: NSNumber(value: number)) ?? ""
    }
    
    override func formatTimeOnly(timeUTC: Kotlinx_datetimeLocalDateTime?, showSeconds: Bool) -> String? {
        guard let date = timeUTC?.toDate(timeZone: .gmt) else { return nil }
        
        let format = DateFormatter()
        format.dateStyle = .none
        format.timeStyle = showSeconds ? .medium : .short
        
        return format.string(from: date)
    }
    
    override func currentTimeForApi() -> String {
        Date().ISO8601Format(.iso8601(timeZone: .gmt))
    }
    
    override func formatBestPattern(timeUTC: Kotlinx_datetimeLocalDateTime, pattern: String) -> String {
        let date = timeUTC.toDate(timeZone: .gmt)!
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = DateFormatter.dateFormat(fromTemplate: pattern, options: 0, locale: Locale.current)!
        return dateFormatter.string(from: date)
    }
    
    override func formatBestPatternLocal(timeLocal: Kotlinx_datetimeLocalDateTime, pattern: String) -> String {
        let date = timeLocal.toDate(timeZone: .current)!
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = DateFormatter.dateFormat(fromTemplate: pattern, options: 0, locale: Locale.current)!
        return dateFormatter.string(from: date)
    }
    
    override func formatWithPattern(timeUTC: Kotlinx_datetimeLocalDateTime, pattern: String) -> String {
        let date = timeUTC.toDate(timeZone: .current)!
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = pattern
        dateFormatter.locale = Locale.current
        dateFormatter.timeZone = TimeZone.current
        return dateFormatter.string(from: date)
    }
    
    override func formatWithPattern(dateUTC: Kotlinx_datetimeLocalDate, pattern: String) -> String {
        let date = dateUTC.toDate(timeZone: .current)!
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = pattern
        dateFormatter.locale = Locale.current
        dateFormatter.timeZone = TimeZone.current
        return dateFormatter.string(from: date)
    }
}

extension Kotlinx_datetimeLocalDateTime {
    func toDateComponents(timeZone: TimeZone) -> DateComponents {
        DateComponents(
            timeZone: timeZone,
            year: Int(year),
            month: Int(month.ordinal + 1),
            day: Int(day),
            hour: Int(hour),
            minute: Int(minute),
            second: Int(second),
            nanosecond: Int(nanosecond)
        )
    }
    
    func toDate(timeZone: TimeZone) -> Date? {
        Calendar.current.date(from: toDateComponents(timeZone: timeZone))
    }
}

extension Kotlinx_datetimeLocalDate {
    func toDateComponents(timeZone: TimeZone) -> DateComponents {
        DateComponents(
            timeZone: timeZone,
            year: Int(year),
            month: Int(month.ordinal + 1),
            day: Int(day)
        )
    }
    
    func toDate(timeZone: TimeZone) -> Date? {
        Calendar.current.date(from: toDateComponents(timeZone: timeZone))
    }
}
