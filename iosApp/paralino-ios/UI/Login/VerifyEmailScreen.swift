//
//  RecoverKeysScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import SwiftUI
import shared

struct VerifyEmailScreen: View {
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.verifyEmailVM)
    @EnvironmentObject var mainAppState: MainAppObject
    
    var body: some View {
        NavigationStack(path: $mainAppState.emailVerificationGraph) {
            ZStack {
                Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
                ComposeRepresentable {
                    ScreensIOS.shared.login.VerifyEmailVC(
                        vm: appState.vm,
                        openChangeEmail: {
                            mainAppState.emailVerificationGraph.append(.changeEmail)
                        }
                    )
                }
                .ignoresSafeArea(.keyboard, edges: .bottom)
            }
            .navigationDestination(for: EmailVerificationGraph.self) { route in
                switch route {
                case .verifyEmail: VerifyEmailScreen()
                case .changeEmail: ChangeEmailScreen(onFinish: { mainAppState.emailVerificationGraph.removeLast() })
                }
            }
        }
    }
}
