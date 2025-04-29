//
//  SubscriptionsScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-03.
//

import SwiftUI
import shared

struct GenericSettingsScreen: View {
    @Binding var path: [NavigationGraph]
    @StateObject var appState: GenericSettingsScreenState
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.GenericSettingsScreenVC(
                    vm: appState.vm,
                    openPaywall: {
                        path.append(.settings(.paywall(type: BillingScreenSourceType.upgrade)))
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(appState.screenTitle ?? "")
        }
    }
}

class GenericSettingsScreenState: ObservableObject {
    
    @Published var screenTitle: String?
    let vm: BaseGenericSettingViewModel
    
    init (vm: BaseGenericSettingViewModel) {
        self.vm = vm
        self.screenTitle = vm.initialScreenTitle
        vm.observe(flow: vm.screenTitle) { [weak self] title in
            self?.screenTitle = title as? String
        }
    }
    
    deinit {
        vm.cleanup()
    }
}
