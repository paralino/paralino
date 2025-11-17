//
//  LoginScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 14.04.23.
//

import SwiftUI
import shared

struct LoginScreen: View {
    
    @StateObject var appState: LoginState
    @EnvironmentObject var mainAppState: MainAppObject
    
    var body: some View {
            ZStack {
                Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
                ComposeRepresentable {
                    ScreensIOS.shared.login.LoginScreenVC(
                        vm: appState.vm,
                        onExit: {
                            if (appState.canNavigateBack) {
                                mainAppState.appStartGraph.removeLast()
                            }
                        }
                    )
                }
                .toolbarTitleDisplayMode(.inline)
                .ignoresSafeArea(.all, edges: .bottom)
                .navigationBarBackButtonHidden(true)
            }
        }
    
    static func createState(screenUiType: LoginScreenUiType) -> LoginState {
        LoginState(screenUiType: screenUiType)
    }
}

class LoginState: ObservableObject {
    
    let vm: LoginViewModel
    @Published var canNavigateBack: Bool = true
    
    init (screenUiType: LoginScreenUiType) {
        self.vm = KoinIOS.ViewModels.shared.loginVM(screenUiType: screenUiType)
        
        vm.observe(flow: vm.canNavigateBack) { [weak self] value in
            self?.canNavigateBack = (value as? KotlinBoolean)?.boolValue == true
        }
    }
    
    deinit {
        vm.cleanup()
    }
}
