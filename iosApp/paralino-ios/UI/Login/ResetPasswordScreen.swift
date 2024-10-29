//
//  ResetPasswordScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import SwiftUI
import shared

struct ResetPasswordScreen: View {
    @StateObject var appState: ViewModelFactoryIOS<ResetPasswordViewModel>
    @EnvironmentObject var mainAppState: MainAppObject
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.login.ResetPasswordVC(
                    vm: appState.vm,
                    openLoginScreen: {
                        mainAppState.appStartGraph = [.login]
                    }
                )
            }
            .ignoresSafeArea(.keyboard, edges: .bottom)
            .navigationTitle(MR.shared.strings.reset_password_screen_title.localizedString())
        }
    }
    
    static func createState(deeplink: DeeplinkType.PasswordReset) -> ViewModelFactoryIOS<ResetPasswordViewModel> {
        ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.resetPasswordVM(userId: deeplink.userId, secret: deeplink.secret))
    }
}
