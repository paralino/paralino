//
//  ConvertAccountScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 23.06.23.
//

import SwiftUI
import shared

struct ConvertAccountScreen: View {
    
    @Binding var path: [NavigationGraph]
    @StateObject var appState = ConvertAccountState()
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.ConvertAccountScreenVC(
                    vm: appState.vm,
                    onNavigateBack: {
                        if (appState.canNavigateBack) {
                            path.removeLast()
                        }
                    }
                )
            }
            .navigationTitle(MR.shared.strings.settings_account_guest_convert.localizedString())
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(!appState.canNavigateBack)
        }
    }
}

@MainActor
class ConvertAccountState: ObservableObject {
    
    // Read by the nonisolated deinit to clean up the view model.
    nonisolated(unsafe) let vm: ConvertAccountViewModel
    @Published var canNavigateBack: Bool = true
    
    init () {
        self.vm = KoinIOS.ViewModels.shared.convertAccountVM
        
        vm.observe(flow: vm.canNavigateBack) { [weak self] value in
            self?.canNavigateBack = (value as? KotlinBoolean)?.boolValue == true
        }
    }
    
    deinit {
        vm.cleanup()
    }
}
