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
    
    @State private var path: [WelcomeGraph] = []
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.welcomeVM)
    
    var body: some View {
        NavigationStack(path: $path) {
            ComposeRepresentable {
                ScreensIOS.shared.login.WelcomeScreenVC(
                    vm: appState.vm,
                    onLoginClick: { path.append(.login) },
                    onStartClick: { path.append(.signup) }
                )
            }
            .ignoresSafeArea(.all, edges: .vertical)
            .navigationDestination(for: WelcomeGraph.self) { route in
                switch route {
                case .welcome: WelcomeScreen()
                case .login:
                    let loginAppState = LoginScreen.createState(screenUiType: LoginScreenUiType.login)
                    LoginScreen(appState: loginAppState, path: $path)
                case .signup:
                    let loginAppState = LoginScreen.createState(screenUiType: LoginScreenUiType.signup)
                    LoginScreen(appState: loginAppState, path: $path)
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
