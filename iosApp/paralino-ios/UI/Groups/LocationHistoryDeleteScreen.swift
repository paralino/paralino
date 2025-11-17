//
//  GroupSettingsScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-04-15.
//

import SwiftUI
import shared

struct LocationHistoryDeleteScreen: View {
    
    @Binding var path: [NavigationGraph]
    @StateObject var appState: ViewModelFactoryIOS<LocationHistoryDeleteViewModel>
    @Environment(\.safeAreaInsets) private var safeAreaInsets
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.LocationHistoryDeleteScreenVC(
                    vm: appState.vm,
                    paddingValues: AppleScreenPaddingValues(
                        start: safeAreaInsets.start,
                        top: 0,
                        end: safeAreaInsets.end,
                        bottom: safeAreaInsets.bottom
                    ),
                    onNavigateBack: {
                        path.removeLast()
                    }
                )
            }
            .toolbarTitleDisplayMode(.inline)
            .ignoresSafeArea(.keyboard, edges: .bottom)
            .navigationTitle(MR.shared.strings.history_delete_screen_title.localizedString())
        }
    }
    
    static func createState(groupId: String) -> ViewModelFactoryIOS<LocationHistoryDeleteViewModel> {
        ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.locationHistorySettingsVM(groupId: groupId))
    }
}
