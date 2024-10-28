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
    
    var body: some View {
        NavigationStack(path: $mainAppState.appStartGraph) {
            ComposeRepresentable {
                ScreensIOS.shared.login.WelcomeScreenVC(
                    vm: appState.vm,
                    onLoginClick: { mainAppState.appStartGraph.append(.login) },
                    onStartClick: { mainAppState.appStartGraph.append(.signup) }
                )
            }
            .ignoresSafeArea(.all, edges: .vertical)
            .navigationDestination(for: WelcomeGraph.self) { route in
                switch route {
                case .welcome: WelcomeScreen()
                case .login:
                    let loginAppState = LoginScreen.createState(screenUiType: LoginScreenUiType.login)
                    LoginScreen(appState: loginAppState)
                case .signup:
                    let loginAppState = LoginScreen.createState(screenUiType: LoginScreenUiType.signup)
                    LoginScreen(appState: loginAppState)
                }
            }
        }
    }
}

struct WelcomeScreen_Previews: PreviewProvider {
    static var previews: some View {
        WelcomeScreen()
    }
}
