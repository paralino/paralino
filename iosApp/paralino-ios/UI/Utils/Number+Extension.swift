//
//  Number+Extension.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-22.
//

extension Comparable {
    func clamped(to limits: ClosedRange<Self>) -> Self {
        return min(max(self, limits.lowerBound), limits.upperBound)
    }
}
