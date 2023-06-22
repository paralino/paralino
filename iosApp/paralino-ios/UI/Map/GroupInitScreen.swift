//
//  GroupInitScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 17.06.23.
//

import SwiftUI
import shared

struct GroupInitScreen: View {
    
    @EnvironmentObject var appDelegate: AppDelegate
    @StateObject var appState = ViewModelFactoryIOS(vm: KoinIOS.ViewModels.shared.groupInitVM)
    @State var alertDialog: AlertDialogData? = nil
    @State var shareText: ShareText?
    
    @State private var openSettings = false
    @State private var openPersonalCodes = false
    
    var body: some View {
        ZStack {
            NavigationLink(
                destination: SettingsScreen(),
                isActive: $openSettings
            ) { EmptyView() }
            NavigationLink(
                destination: PersonalCodeScreen(),
                isActive: $openPersonalCodes
            ) { EmptyView() }
            ComposeRepresentable {
                ScreensIOS.shared.home.GroupInitScreenVC(
                    vm: appState.vm,
                    onShowDialog: { dialog in
                        alertDialog = dialog != nil ? AlertDialogData(coreData: dialog!) : nil
                    },
                    openSettings: { openSettings = true },
                    onCreateGroup: {},
                    openPersonalCodes: { openPersonalCodes = true },
                    onSharePersonalCode: { data in
                        shareText = ShareText(text: data)
                    },
                    askNotificationPermission: {
                        let center = UNUserNotificationCenter.current()
                        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
                            appDelegate.locationStatusProvider.hasNotificationPermission = granted
                            appDelegate.locationStatusProvider.refreshLocationStatus()
                        }
                    },
                    onClose: {}
                )
            }
        }
        .mainAlert($alertDialog) { key in
            if let dialogData = alertDialog?.coreDialog {
                if dialogData is MainAlertDialogVerifyEmail {
                    if key == MainAlertDialogButtons.shared.buttonOk {
                        appState.vm.sendEmailVerification()
                    }
                }
            }
        }
        .sheet(item: $shareText) { shareText in
            ActivityView(text: shareText.text)
        }
        .ignoresSafeArea(.all, edges: .bottom)
    }
}

struct GroupInitScreen_Previews: PreviewProvider {
    static var previews: some View {
        GroupInitScreen()
    }
}
