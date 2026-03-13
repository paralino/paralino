//
//  AccountScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 22.06.23.
//

import SwiftUI
import shared

struct AccountScreen: View {
    
    @EnvironmentObject var appDelegate: AppDelegate
    @Binding var path: [NavigationGraph]
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.settingsVM)
    @State var shareText: ShareText?
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.AccountScreenVC(
                    vm: appState.vm,
                    openConvertAccount: {
                        path.append(.settings(.convertAccount))
                    },
                    openEditProfile: {
                        path.append(.settings(.editProfile))
                    },
                    openChangeEmail: {
                        path.append(.settings(.changeEmail))
                    },
                    openChangePassword: {
                        path.append(.settings(.changePassword))
                    },
                    openProfileRawScreen: {
                        path.append(.settings(.rawData(type: .UserPresentation())))
                    },
                    openBlockList: {
                        path.append(.settings(.blocklist))
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
            .navigationTitle(MR.shared.strings.settings_section_account_title.localizedString())
        }
        .sheet(item: $shareText) { shareText in
            ActivityView(text: shareText.text)
        }
    }
}

struct AccountScreen_Previews: PreviewProvider {
    @State static var path: [NavigationGraph] = [.settings(.account)]
    static var previews: some View {
        AccountScreen(path: $path)
    }
}
