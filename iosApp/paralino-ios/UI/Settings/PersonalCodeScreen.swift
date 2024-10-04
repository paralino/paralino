//
//  PersonalCodeScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 22.06.23.
//

import SwiftUI
import shared

struct PersonalCodeScreen: View {
    
    @EnvironmentObject var appDelegate: AppDelegate
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.personalCodeVM)
    @State var shareText: ShareText?
    let notificationPermissionHandler = NotificationPermissionHandler()
    
    var body: some View {
        ZStack {
            Color.themebackground.ignoresSafeArea(.all, edges: .vertical)
            ComposeRepresentable {
                ScreensIOS.shared.settings.PersonalCodeScreenVC(
                    vm: appState.vm,
                    askNotificationPermission: {
                        notificationPermissionHandler.requestNotificationPermission(
                            appDelegate: appDelegate, messageDispather: appState.vm.messageDispatcher
                        )
                    },
                    onShare: { data in
                        shareText = ShareText(text: data)
                    }
                )
            }
            .ignoresSafeArea(.all, edges: .bottom)
            .navigationTitle(MR.shared.strings.settings_section_personal_code_title.localizedString())
        }
        .sheet(item: $shareText) { shareText in
            ActivityView(text: shareText.text)
        }
    }
}

struct PersonalCodeScreen_Previews: PreviewProvider {
    static var previews: some View {
        PersonalCodeScreen()
    }
}
