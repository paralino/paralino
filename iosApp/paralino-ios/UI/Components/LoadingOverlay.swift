//
//  LoadingOverlay.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-28.
//

import Foundation
import SwiftUI
import shared

struct LoadingOverlayModifier: ViewModifier {
    let isLoading: Bool
    let message: String
    
    func body(content: Content) -> some View {
        ZStack {
            content
                .blur(radius: isLoading ? 4 : 0)
            
            if isLoading {
                Color.themebackground.opacity(0.5)
                    .edgesIgnoringSafeArea(.all)
                
                VStack {
                    ProgressView(message)
                        .progressViewStyle(CircularProgressViewStyle(tint: .themeOnSurface))
                        .padding()
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color.themeSurface)
                                .shadow(color: .themeOnSurface.opacity(0.5), radius: 16)
                        )
                }
            }
        }
        .animation(.easeInOut, value: isLoading)
    }
}

extension View {
    func loadingOverlay(isLoading: Bool) -> some View {
        self.modifier(LoadingOverlayModifier(isLoading: isLoading, message: MR.shared.strings.login_step_generic_loading.localizedString()))
    }
    
    func loadingOverlay(isLoading: Bool, message: String) -> some View {
        self.modifier(LoadingOverlayModifier(isLoading: isLoading, message: message))
    }
}

#Preview {
    ZStack {
        Color.themebackground
    }.loadingOverlay(isLoading: true)
}
