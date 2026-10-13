//
//  SettingsScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 22.06.23.
//

import SwiftUI
import shared

struct SettingsScreen: View {
    
    @EnvironmentObject var appDelegate: AppDelegate
    @Binding var path: [NavigationGraph]
    
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.settingsVM)
    @State var shareText: ShareText?
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.SettingsScreenVC(
                    vm: appState.vm,
                    onShareApp: { data in
                        shareText = ShareText(text: data)
                    },
                    openConvertAccount: {
                        path.append(.settings(.convertAccount))
                    },
                    openProfileDetails: {
                        path.append(.settings(.account))
                    },
                    openPersonalCodes: {
                        path.append(.settings(.personalCodes))
                    },
                    openBlockList: {
                        path.append(.settings(.blocklist))
                    },
                    openGeneralInfo: {
                        path.append(.settings(.generalInfo))
                    },
                    openAppStatus: {
                        path.append(.settings(.appStatusDebug))
                    },
                    openStatus: {
                        path.append(.settings(.appStatus))
                    },
                    openSubscriptions: {
                        path.append(.settings(.subscriptions))
                    },
                    openPaywall: {
                        path.append(.settings(.paywall(type: BillingScreenSourceType.normal)))
                    },
                    openAppSettings: {
                        path.append(.settings(.appSettings))
                    },
                    openLocationSettings: {
                        path.append(.settings(.locationSettings))
                    },
                    onShare: { data in
                        shareText = ShareText(text: data)
                    },
                    askNotificationPermission: {
                        appDelegate.notificationPermissionHandler.requestNotificationPermission(
                            messageDispather: appState.vm.messageDispatcher
                        )
                    }
                )
            }
            .toolbarTitleDisplayMode(.inline)
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.title_settings.localizedString())
        }
        .sheet(item: $shareText) { shareText in
            ActivityView(text: shareText.text)
        }
    }
}

struct SettingsScreen_Previews: PreviewProvider {
    @State static var path: [NavigationGraph] = [.settings(.settings)]
    static var previews: some View {
        SettingsScreen(path: $path)
    }
}
