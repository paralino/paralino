//
//  LoginScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 14.04.23.
//

import SwiftUI
import shared

struct LoginScreen: View {
    
    @StateObject var appState: ViewModelFactoryIOS<LoginViewModel>
    @Binding var path: [WelcomeGraph]
    
    var body: some View {
            ZStack {
                Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
                ComposeRepresentable {
                    ScreensIOS.shared.login.LoginScreenVC(
                        vm: appState.vm,
                        onExit: { path.removeLast() }
                    )
                }
                .ignoresSafeArea(.all, edges: .bottom)
//                .navigationBarBackButtonHidden(true)
            }
        }
    
    static func createState(screenUiType: LoginScreenUiType) -> ViewModelFactoryIOS<LoginViewModel> {
        ViewModelFactoryIOS(
            vm: KoinIOS.ViewModels.shared.loginVM(screenUiType: screenUiType)
        )
    }
}

struct LoginScreen_Previews: PreviewProvider {
    @StateObject static var loginAppState = ViewModelFactoryIOS(
        vm: KoinIOS.ViewModels.shared.loginVM(screenUiType: LoginScreenUiType.signup)
    )
    @State static var path: [WelcomeGraph] = [.welcome, .login]
    static var previews: some View {
        LoginScreen(appState: loginAppState, path: $path)
    }
}
