//
//  ChangeEmailScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 23.06.23.
//

import SwiftUI
import shared

struct ChangeEmailScreen: View {
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.changeEmailVM)
    @Binding var path: [NavigationGraph]
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.ChangeEmailScreenVC(
                    vm: appState.vm,
                    onNavigateBack: { path.removeLast() }
                )
            }
            .ignoresSafeArea(.keyboard, edges: .bottom)
            .navigationTitle(MR.shared.strings.settings_account_edit_email.localizedString())
        }
    }
}

struct ChangeEmailScreen_Previews: PreviewProvider {
    @State static var path: [NavigationGraph] = [.settings(.changeEmail)]
    static var previews: some View {
        ChangeEmailScreen(path: $path)
    }
}
