//
//  WelcomeOnboardingScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-12-17.
//

import SwiftUI
import shared

struct WelcomeOnboardingScreen: View {
    
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.welcomeOnboardingVM)
    let isDemoMode: Bool
    let onFinish: () -> Void
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.login.WelcomeOnboardingScreen(
                    vm: appState.vm,
                    isDemo: isDemoMode,
                    onFinish: onFinish
                )
            }
            .navigationBarBackButtonHidden(true)
        }
    }
}
