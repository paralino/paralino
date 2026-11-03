//
//  SubscriptionsScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-03.
//

import SwiftUI
import shared

struct GenericSettingsScreen: View {
    
    @EnvironmentObject var appDelegate: AppDelegate
    @Binding var path: [NavigationGraph]
    @StateObject var appState: GenericSettingsScreenState
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.GenericSettingsScreenVC(
                    vm: appState.vm,
                    openPaywall: {
                        path.append(.settings(.paywall(type: BillingScreenSourceType.upgradeDefault)))
                    },
                    askNotificationPermission: {
                        appDelegate.notificationPermissionHandler.requestNotificationPermission(
                            messageDispather: appState.vm.messageDispatcher
                        )
                    },
                    askBackgroundAccessPermission: {
                        appDelegate.locationPermissionHandler.requestBackgroundRefreshPermission(
                            messageDispather: appState.vm.messageDispatcher
                        )
                    },
                    applyAppTheme: { theme in
                        AppThemeManager.apply(theme: theme)
                    },
                    applyDarkThemeStyle: { style in
                        AppThemeManager.applyDarkThemeStyle(style)
                    }
                )
            }
            .toolbarTitleDisplayMode(.inline)
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(appState.screenTitle ?? "")
        }
    }
}

@MainActor
class GenericSettingsScreenState: ObservableObject {
    
    @Published var screenTitle: String?
    // Read by the nonisolated deinit to clean up the view model.
    nonisolated(unsafe) let vm: BaseGenericSettingViewModel
    
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
