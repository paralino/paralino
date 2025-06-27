//
//  GroupSettingsScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-04-15.
//

import SwiftUI
import shared

struct GroupSettingsScreen: View {
    
    @Binding var path: [NavigationGraph]
    @StateObject var appState: ViewModelFactoryIOS<GroupSettingsViewModel>
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.GroupSettingsScreenVC(
                    vm: appState.vm,
                    openEditGroup: {
                        path.append(.home(.groupCreate(groupId: appState.vm.groupId)))
                    },
                    onGroupDeleted: {
                        path = [.home(.map)]
                    },
                    openRawDataScreen: {
                        path.append(.settings(.rawData(type: RawPresentationDataType.GroupPresentation(groupId: appState.vm.groupId))))
                    },
                    openBatteryNotificationsSettings: {
                        path.append(.home(.batteryNotificationsSettings(groupId: appState.vm.groupId)))
                    },
                    openLocationHistorySettings: {
                        path.append(.home(.locationHistorySettings(groupId: appState.vm.groupId)))
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.title_settings.localizedString())
        }
    }
    
    static func createState(groupId: String) -> ViewModelFactoryIOS<GroupSettingsViewModel> {
        ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.groupSettingsVM(groupId: groupId))
    }
}
