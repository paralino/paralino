//
//  ValueFormatter.swift
//  paralino-ios
//
//  Created by Zelimir on 26.06.23.
//

import Foundation
import shared

class ValueFormatter: IValueFormatter {
    
    override var unitType: DistanceUnitType {
        let measurementSystem = Locale.current.measurementSystem
        if measurementSystem == .uk || measurementSystem == .us {
            return DistanceUnitType.imperial
        } else {
            return DistanceUnitType.metric
        }
    }
    
    override func formatNumber(number: Double, minFractions: Int32, maxFractions: Int32) -> String {
        let formatter = NumberFormatter()
        formatter.minimumFractionDigits = Int(minFractions)
        formatter.maximumFractionDigits = Int(maxFractions)
        return formatter.string(from: NSNumber(value: number)) ?? ""
    }
    
    override func formatTimeOnly(timeUTC: Kotlinx_datetimeLocalDateTime?) -> String? {
        guard let date = timeUTC?.toDate() else { return nil }
        
        let format = DateFormatter()
        format.dateStyle = .none
        format.timeStyle = .short

        return format.string(from: date)
    }
    
    override func currentTimeForApi() -> String {
        Date().ISO8601Format(.iso8601(timeZone: .gmt))
    }
    
    override func formatBestPattern(timeUTC: Kotlinx_datetimeLocalDateTime, pattern: String) -> String {
        let date = timeUTC.toDate()!
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = DateFormatter.dateFormat(fromTemplate: pattern, options: 0, locale: Locale.current)!
        return dateFormatter.string(from: date)
    }
    
    override func formatWithPattern(timeUTC: Kotlinx_datetimeLocalDateTime, pattern: String) -> String {
        let date = timeUTC.toDate()!
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = pattern
        dateFormatter.locale = Locale.current
        dateFormatter.timeZone = TimeZone.current
        return dateFormatter.string(from: date)
    }
}

extension Kotlinx_datetimeLocalDateTime {
    func toDateComponents() -> DateComponents {
        DateComponents(
            timeZone: .gmt,
            year: Int(year),
            month: Int(monthNumber),
            day: Int(dayOfMonth),
            hour: Int(hour),
            minute: Int(minute),
            second: Int(second),
            nanosecond: Int(nanosecond)
        )
    }
    
    func toDate() -> Date? {
        Calendar.current.date(from: toDateComponents())
    }
}
