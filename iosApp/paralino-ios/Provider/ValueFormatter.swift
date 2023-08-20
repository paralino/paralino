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
    
    override func getDistanceSmall(meters: Float, unitType: DistanceUnitType?) -> String {
        let realUnitType = unitType ?? self.unitType
        switch realUnitType {
        case .metric: return "\(meters)"
        case .imperial: return "\(meters)"
        default: return "\(meters)"
        }
    }
    
    override func getLastPing(time: Kotlinx_datetimeLocalDateTime?) -> String? {
        time?.description()
    }
}
