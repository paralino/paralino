//
//  ChangePasswordScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 23.06.23.
//

import SwiftUI
import shared

struct ChangePasswordScreen: View {
    @StateObject var appState = ChangePasswordState()
    @Binding var path: [NavigationGraph]
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.ChangePasswordScreenVC(
                    vm: appState.vm,
                    onNavigateBack: {
                        if (appState.canNavigateBack) {
                            path.removeLast()
                        }
                    }
                )
            }
            .navigationTitle(MR.shared.strings.settings_account_edit_password.localizedString())
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(!appState.canNavigateBack)
        }
    }
}

@MainActor
class ChangePasswordState: ObservableObject {
    
    // Read by the nonisolated deinit to clean up the view model.
    nonisolated(unsafe) let vm: ChangePasswordViewModel
    @Published var canNavigateBack: Bool = true
    
    init () {
        self.vm = KoinIOS.ViewModels.shared.changePasswordVM
        
        vm.observe(flow: vm.canNavigateBack) { [weak self] value in
            self?.canNavigateBack = (value as? KotlinBoolean)?.boolValue == true
        }
    }
    
    deinit {
        vm.cleanup()
    }
}
