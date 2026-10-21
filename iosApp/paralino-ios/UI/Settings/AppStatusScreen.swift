//
//  AppStatusScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 2026-10-13.
//

import SwiftUI
import shared

struct AppStatusScreen: View {
    @EnvironmentObject var appDelegate: AppDelegate
    @Binding var path: [NavigationGraph]
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.appStatusVM)

    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.AppStatusScreenVC(
                    vm: appState.vm,
                    askNotificationPermission: {
                        appDelegate.notificationPermissionHandler.requestNotificationPermission(
                            messageDispather: appState.vm
                        )
                    },
                    askLocationService: {
                        appDelegate.locationPermissionHandler.requestLocationServices(
                            messageDispather: appState.vm
                        )
                    },
                    askLocationPermission: {
                        appDelegate.locationPermissionHandler.requestLocationPermission(
                            messageDispather: appState.vm
                        )
                    },
                    askBackgroundLocationPermission: {
                        appDelegate.locationPermissionHandler.requestLocationPermission(
                            messageDispather: appState.vm
                        )
                    },
                    askBackgroundAccessPermission: {
                        appDelegate.locationPermissionHandler.requestBackgroundRefreshPermission(
                            messageDispather: appState.vm
                        )
                    },
                    openLocationSettings: {
                        path.append(.settings(.locationSettings))
                    },
                    openDebugLogs: {
                        path.append(.settings(.appStatusDebug))
                    }
                )
            }
            .toolbarTitleDisplayMode(.inline)
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.setting_status_title.localizedString())
        }
    }
}
