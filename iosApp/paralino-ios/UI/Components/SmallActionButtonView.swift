//
//  SimpleActionButton.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-11-10.
//

import SwiftUI

struct SmallActionButtonView: View {
    let text: String
    let action: (() -> Void)?
    
    var backgroundColor: Color = .themePrimary
    var contentColor: Color = .themeOnPrimary
    var hasEndArrow: Bool = false
    
    var body: some View {
        Button(action: { action?() }) {
            HStack(spacing: 4) {
                Spacer()
                    .frame(width: 4)
                
                Text(text)
                    .font(.caption)
                
                if hasEndArrow {
                    Image(systemName: "chevron.right")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 12, height: 12)
                        .foregroundColor(contentColor)
                } else {
                    Spacer()
                        .frame(width: 4)
                }
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 6)
            .background(backgroundColor)
            .foregroundColor(contentColor)
            .clipShape(Capsule())
        }
        .disabled(action == nil)
    }
}
