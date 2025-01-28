//
//  GroupSettingsScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-04-15.
//

import SwiftUI
import shared

struct BatteryNotificationsSettingsScreen: View {
    
    @Binding var path: [NavigationGraph]
    @StateObject var appState: ViewModelFactoryIOS<BatteryNotificationsViewModel>
    @Environment(\.safeAreaInsets) private var safeAreaInsets
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.BatteryNotificationsSettingsScreenVC(
                    vm: appState.vm,
                    paddingValues: AppleScreenPaddingValues(
                        start: safeAreaInsets.start,
                        top: 0,
                        end: safeAreaInsets.end,
                        bottom: safeAreaInsets.bottom
                    ),
                    openPaywall: {
                        path.append(.settings(.paywall(type: BillingScreenSourceType.upgrade)))
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.group_settings_battery_notifications_title.localizedString())
        }
    }
    
    static func createState(groupId: String) -> ViewModelFactoryIOS<BatteryNotificationsViewModel> {
        ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.groupBatterySettingsVM(groupId: groupId))
    }
}
