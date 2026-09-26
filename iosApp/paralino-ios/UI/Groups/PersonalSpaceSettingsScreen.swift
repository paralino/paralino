//
//  PersonalSpaceSettingsScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2025-08-27.
//

import SwiftUI
import shared

struct PersonalSpaceSettingsScreen: View {
    
    @Binding var path: [NavigationGraph]
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.personalSpaceSettingsVM)
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.home.PersonalSpaceSettingsScreenVC(
                    vm: appState.vm,
                    openRawDataScreen: {
                        path.append(.settings(.rawData(type: RawPresentationDataType.PersonalSpacePresentation())))
                    },
                    openBatteryNotificationsSettings: {
                        path.append(.home(.batteryNotificationsSettings(groupId: UserGroup.companion.groupPersonalDevicesId)))
                    },
                    openLocationHistorySettings: {
                        path.append(.home(.locationHistorySettings(groupId: UserGroup.companion.groupPersonalDevicesId)))
                    },
                    openMap: {
                        path.removeAll()
                        path.append(.home(.map))
                    }
                )
            }
            .toolbarTitleDisplayMode(.inline)
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.personalSpace_spaceTitle.localizedString())
        }
    }
}
