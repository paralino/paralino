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
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.ChangeEmailScreenVC(
                    vm: appState.vm,
                    onNavigateBack: {}
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
        }
    }
}

struct ChangeEmailScreen_Previews: PreviewProvider {
    static var previews: some View {
        ChangeEmailScreen()
    }
}
