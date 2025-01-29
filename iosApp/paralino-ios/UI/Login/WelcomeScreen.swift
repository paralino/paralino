//
//  WellcomeScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 14.04.23.
//

import Foundation
import SwiftUI
import shared


struct WelcomeScreen: View {
    
    @EnvironmentObject var mainAppState: MainAppObject
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.welcomeVM)
    
    @Environment(\.safeAreaInsets) private var safeAreaInsets
    
    var body: some View {
        NavigationStack(path: $mainAppState.appStartGraph) {
            ComposeRepresentable {
                ScreensIOS.shared.login.WelcomeScreenVC(
                    vm: appState.vm,
                    paddingValues: safeAreaInsets,
                    onLoginClick: { mainAppState.appStartGraph.append(.signup) },
                    onStartClick: { mainAppState.appStartGraph.append(.onboarding) }
                )
            }
            .ignoresSafeArea(.all, edges: .vertical)
            .navigationDestination(for: WelcomeGraph.self) { route in
                switch route {
                case .welcome: WelcomeScreen()
                case .onboarding: WelcomeOnboardingScreen(
                    isDemoMode: false,
                    onFinish: {
                        mainAppState.appStartGraph.append(.signup)
                    }
                )
                case .login:
                    let loginAppState = LoginScreen.createState(screenUiType: LoginScreenUiType.login)
                    LoginScreen(appState: loginAppState)
                case .signup:
                    let loginAppState = LoginScreen.createState(screenUiType: LoginScreenUiType.signup)
                    LoginScreen(appState: loginAppState)
                case .passwordReset(let deeplink):
                    ResetPasswordScreen(appState: ResetPasswordScreen.createState(deeplink: deeplink))
                }
            }
        }
    }
}
