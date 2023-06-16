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
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.welcomeVM)
    @State private var openSignup = false
    @State private var openLogin = false
    var body: some View {
        ZStack {
            NavigationLink(
                destination: LoginScreen(loginType: LoginScreenUiType.signup),
                isActive: $openSignup
            ) { EmptyView() }
            NavigationLink(
                destination: LoginScreen(loginType: LoginScreenUiType.login),
                isActive: $openLogin
            ) { EmptyView() }
            
            ComposeRepresentable {
                ScreensIOS.shared.login.WelcomeScreenVC(
                    vm: appState.vm,
                    onLoginClick: { openLogin = true },
                    onStartClick: { openSignup = true }
                )
            }
            .ignoresSafeArea(.all, edges: .vertical) // Compose has own keyboard handler
        }
    }
}

struct WelcomeScreen_Previews: PreviewProvider {
    static var previews: some View {
        WelcomeScreen()
    }
}
