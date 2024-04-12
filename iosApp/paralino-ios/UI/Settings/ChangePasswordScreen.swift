//
//  ChangePasswordScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 23.06.23.
//

import SwiftUI
import shared

struct ChangePasswordScreen: View {
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.changePasswordVM)
    @Binding var path: [NavigationGraph]
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.ChangePasswordScreenVC(
                    vm: appState.vm,
                    onNavigateBack: { path.removeLast() }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.settings_account_edit_password.localizedString())
        }
    }
}

struct ChangePasswordScreen_Previews: PreviewProvider {
    @State static var path: [NavigationGraph] = [.settings(.changePassword)]
    static var previews: some View {
        ChangePasswordScreen(path: $path)
    }
}
