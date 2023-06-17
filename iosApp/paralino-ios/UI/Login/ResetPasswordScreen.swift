//
//  ResetPasswordScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import SwiftUI
import shared

struct ResetPasswordScreen: View {
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.resetPasswordVM)
    
    var body: some View {
        ComposeRepresentable {
            ScreensIOS.shared.login.ResetPasswordVC(
                vm: appState.vm,
                openLoginScreen: {},
                onClose: {}
            )
        }
    }
}

struct ResetPasswordScreen_Previews: PreviewProvider {
    static var previews: some View {
        ResetPasswordScreen()
    }
}
