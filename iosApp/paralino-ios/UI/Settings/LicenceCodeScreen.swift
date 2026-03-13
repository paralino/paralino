//
//  LicenceCodeScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2026-03-12.
//

import SwiftUI
import shared

struct LicenceCodeScreen: View {
    @Binding var path: [NavigationGraph]
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.licenceCodeVM)
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.LicenceCodeScreenVC(
                    vm: appState.vm,
                    onNavigateBack: {
                        path.removeLast()
                    }
                )
            }
            .toolbarTitleDisplayMode(.inline)
            .ignoresSafeArea(.keyboard, edges: .bottom)
            .navigationTitle(MR.shared.strings.settings_section_general_title.localizedString())
        }
    }
}
