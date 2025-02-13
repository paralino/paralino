//
//  ChangeEmailScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 23.06.23.
//

import SwiftUI
import shared

struct ChangeEmailScreen: View {
    @StateObject var appState = ChangeEmailState()
    let onFinish: () -> Void
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.ChangeEmailScreenVC(
                    vm: appState.vm,
                    onNavigateBack: { onFinish() }
                )
            }
            .ignoresSafeArea(.keyboard, edges: .bottom)
            .navigationTitle(MR.shared.strings.settings_account_edit_email.localizedString())
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(!appState.canNavigateBack)
        }
    }
}

class ChangeEmailState: ObservableObject {
    
    let vm: ChangeEmailViewModel
    @Published var canNavigateBack: Bool = true
    
    init () {
        self.vm = KoinIOS.ViewModels.shared.changeEmailVM
        
        vm.observe(flow: vm.canNavigateBack) { [weak self] value in
            self?.canNavigateBack = (value as? KotlinBoolean)?.boolValue == true
        }
    }
    
    deinit {
        vm.cleanup()
    }
}
