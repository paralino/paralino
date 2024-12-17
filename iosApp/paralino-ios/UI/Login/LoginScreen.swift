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
    @EnvironmentObject var mainAppState: MainAppObject
    
    var body: some View {
            ZStack {
                Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
                ComposeRepresentable {
                    ScreensIOS.shared.login.LoginScreenVC(
                        vm: appState.vm,
                        onExit: { mainAppState.appStartGraph.removeLast() }
                    )
                }
                .ignoresSafeArea(.all, edges: .bottom)
                .navigationBarBackButtonHidden(true)
            }
        }
    
    static func createState(screenUiType: LoginScreenUiType) -> ViewModelFactoryIOS<LoginViewModel> {
        ViewModelFactoryIOS(
            vm: KoinIOS.ViewModels.shared.loginVM(screenUiType: screenUiType)
        )
    }
}
