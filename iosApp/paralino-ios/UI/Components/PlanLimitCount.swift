//
//  PlanLimitCount.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-11-10.
//

import SwiftUI

struct PlanLimitCount: View {
    let count: Int
    let limit: Int?
    private let backgroundColor: Color
    private let errorBackgroundColor: Color
    private let setContentColor: Color?
    
    @Environment(\.colorScheme) private var colorScheme
    private var contentColor: Color {
        guard let setContentColor else { return colorScheme == .dark ? Color.accent : Color.accentDark }
        return setContentColor
    }
    
    init(
        count: Int,
        limit: Int?,
        backgroundColor: Color = Color.accentLight.opacity(0.3),
        errorBackgroundColor: Color = Color.errorBackground,
        contentColor: Color = Color.accentDark
    ) {
        self.count = count
        self.limit = limit
        self.backgroundColor = backgroundColor
        self.errorBackgroundColor = errorBackgroundColor
        self.setContentColor = contentColor
    }
    
    init (
        count: Int,
        limit: Int?
    ) {
        self.count = count
        self.limit = limit
        self.backgroundColor = Color.accentLight.opacity(0.3)
        self.errorBackgroundColor = Color.errorBackground
        self.setContentColor = nil
    }
    
    var body: some View {
        Text(attributedText)
            .padding(.vertical, 2)
            .padding(.horizontal, 6)
            .background(
                limit != nil && limit! <= count
                    ? errorBackgroundColor
                    : backgroundColor
            )
            .cornerRadius(4)
    }
    
    private var attributedText: AttributedString {
        var string = AttributedString("\(count)")
        string.font = .system(size: 18, weight: .bold)
        string.foregroundColor = contentColor
        
        if let limit = limit {
            var limitString = AttributedString("/\(limit)")
            limitString.font = .system(size: 12, weight: .semibold)
            limitString.foregroundColor = contentColor
            string.append(limitString)
        }
        
        return string
    }
}
