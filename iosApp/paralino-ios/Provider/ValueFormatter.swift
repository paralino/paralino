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
        return DistanceUnitType.metric
    }
    
    override func formatNumber(number: Double, minFractions: Int32, maxFractions: Int32) -> String {
        let formatter = NumberFormatter()
        formatter.minimumFractionDigits = Int(minFractions)
        formatter.maximumFractionDigits = Int(maxFractions)
        return formatter.string(from: NSNumber(value: number)) ?? ""
    }
    
    override func formatTimeOnly(timeUTC: Kotlinx_datetimeLocalDateTime?) -> String? {
        guard let time = timeUTC else { return nil }
        let dateComponents = DateComponents(
            timeZone: .gmt,
            year: Int(time.year),
            month: Int(time.monthNumber),
            day: Int(time.dayOfMonth),
            hour: Int(time.hour),
            minute: Int(time.minute),
            second: Int(time.second),
            nanosecond: Int(time.nanosecond)
        )
        
        guard let date = Calendar.current.date(from: dateComponents) else { return nil }
        
        let format = DateFormatter()
        format.dateStyle = .none
        format.timeStyle = .short

        return format.string(from: date)
    }
    
    override func currentTimeForApi() -> String {
        Date().ISO8601Format(.iso8601(timeZone: .gmt))
    }
    
    override func formatBestPattern(epochSeconds: Int64, pattern: String) -> String {
        let date = Date(timeIntervalSince1970: Double(epochSeconds))
        let dateFormatter = DateFormatter()
        let dayFormat = DateFormatter.dateFormat(fromTemplate: pattern, options: 0, locale: Locale.current)!
        dateFormatter.dateFormat = dayFormat
        return dateFormatter.string(from: date)
    }
}
